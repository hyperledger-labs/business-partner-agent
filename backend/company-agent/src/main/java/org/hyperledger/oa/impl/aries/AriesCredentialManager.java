/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.aries.api.credential.CredentialAttributes;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.credential.CredentialProposalRequest;
import org.hyperledger.aries.api.credential.CredentialProposalRequest.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.AriesCredential;
import org.hyperledger.oa.api.aries.AriesCredential.AriesCredentialBuilder;
import org.hyperledger.oa.api.aries.ProfileVC;
import org.hyperledger.oa.api.exception.NetworkException;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.controller.api.WebSocketMessageBody;
import org.hyperledger.oa.impl.MessageService;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
public class AriesCredentialManager {

    @Value("${oagent.did.prefix}")
    String didPrefix;

    @Inject
    @Setter
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    PartnerCredDefLookup credLookup;

    @Inject
    SchemaService schemaService;

    @Inject
    Converter conv;

    @Inject
    ObjectMapper mapper;

    @Inject
    MessageService messageService;

    @Inject
    LabelStrategy labelStrategy;

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId) {
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (dbPartner.isPresent()) {
            final Optional<MyDocument> dbDoc = docRepo.findById(myDocId);
            if (dbDoc.isPresent()) {
                if (!CredentialType.SCHEMA_BASED.equals(dbDoc.get().getType())) {
                    throw new PartnerException("Only documents that are based on a " +
                            "schema can be converted into a credential");
                }
                try {
                    final Optional<org.hyperledger.oa.model.BPASchema> s = schemaService
                            .getSchemaFor(dbDoc.get().getSchemaId());
                    if (s.isPresent()) {
                        final Optional<String> credDefId = credLookup.findCredentialDefinitionId(
                                partnerId, s.get().getSeqNo());
                        if (credDefId.isPresent()) {
                            ac.issueCredentialSendProposal(
                                    CredentialProposalRequest
                                            .builder()
                                            .connectionId(dbPartner.get().getConnectionId())
                                            .schemaId(s.get().getSchemaId())
                                            .credentialProposal(
                                                    new CredentialPreview(
                                                            CredentialAttributes.from(dbDoc.get().getDocument())))
                                            .credentialDefinitionId(credDefId.get())
                                            .build());
                        } else
                            throw new PartnerException("Found no matching credential definition id. "
                                    + "Partner can not issue bank account credentials");
                    } else {
                        throw new PartnerException("No configured schema found for id: " + dbDoc.get().getSchemaId());
                    }
                } catch (IOException e) {
                    throw new NetworkException("No aries connection", e);
                }
            } else {
                throw new PartnerException("No document found for id: " + myDocId.toString());
            }
        } else {
            throw new PartnerException("No partner found for id: " + partnerId.toString());
        }
    }

    // all other state changes that are not store or acked
    public void handleCredentialEvent(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> credRepo.updateState(cred.getId(), credEx.getState()), () -> {
                    MyCredential dbCred = MyCredential
                            .builder()
                            .isPublic(Boolean.FALSE)
                            .connectionId(credEx.getConnectionId())
                            .state(credEx.getState())
                            .threadId(credEx.getThreadId())
                            .build();
                    credRepo.save(dbCred);
                });
    }

    // credential signed, but not in wallet yet
    public void handleStoreCredential(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    try {
                        credRepo.updateState(cred.getId(), credEx.getState());
                        // TODO should not be necessary with --auto-store-credential set
                        ac.issueCredentialRecordsStore(credEx.getCredentialExchangeId());
                    } catch (IOException e) {
                        log.error("aca-py not reachable", e);
                    }
                }, () -> log.error("Received store credential event without matching thread id"));
    }

    // credential, signed and stored in wallet
    public void handleCredentialAcked(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    String label = labelStrategy.apply(credEx.getCredential());
                    cred
                            .setReferent(credEx.getCredential().getReferent())
                            .setCredential(conv.toMap(credEx.getCredential()))
                            .setType(CredentialType.SCHEMA_BASED)
                            .setState(credEx.getState())
                            .setIssuer(resolveIssuer(credEx.getCredential()))
                            .setIssuedAt(Instant.now())
                            .setLabel(label);
                    MyCredential updated = credRepo.update(cred);
                    messageService.sendMessage(WebSocketMessageBody.credentialReceived(buildAriesCredential(updated)));

                }, () -> log.error("Received credential without matching thread id, credential is not stored."));
    }

    public Optional<MyCredential> toggleVisibility(UUID id) {
        final Optional<MyCredential> cred = credRepo.findById(id);
        if (cred.isPresent()) {
            credRepo.updateIsPublic(id, !cred.get().getIsPublic());
            vpMgmt.recreateVerifiablePresentation();
        }
        return cred;
    }

    // credential CRUD operations

    public List<AriesCredential> listCredentials() {
        List<AriesCredential> result = new ArrayList<>();
        credRepo.findAll().forEach(c -> result.add(buildAriesCredential(c)));
        return result;
    }

    public Optional<AriesCredential> getAriesCredentialById(@NonNull UUID id) {
        final Optional<MyCredential> dbCred = credRepo.findById(id);
        return dbCred.map(this::buildAriesCredential);
    }

    private AriesCredential buildAriesCredential(MyCredential dbCred) {
        final AriesCredentialBuilder myCred = AriesCredential.fromMyCredential(dbCred);
        if (dbCred.getCredential() != null) {
            final Credential ariesCred = conv.fromMap(dbCred.getCredential(), Credential.class);
            myCred
                    .schemaId(ariesCred.getSchemaId())
                    .credentialDefinitionId(ariesCred.getCredentialDefinitionId())
                    .typeLabel(schemaService.getSchemaLabel(ariesCred.getSchemaId()))
                    .credentialData(ariesCred.getAttrs());
            // TODO only for backwards compatibility, can be removed at some point
            if (dbCred.getIssuer() == null) {
                myCred.issuer(resolveIssuer(ariesCred));
            }
        }
        return myCred.build();
    }

    /**
     * Updates the credentials label
     * 
     * @param id    the credential id
     * @param label the credentials label
     * @return the updated credential if found
     */
    public Optional<AriesCredential> updateCredentialById(@NonNull UUID id, @Nullable String label) {
        final Optional<AriesCredential> cred = getAriesCredentialById(id);
        if (cred.isPresent()) {
            String mergedLabel = labelStrategy.apply(label, cred.get());
            credRepo.updateLabel(id, mergedLabel);
            cred.get().setLabel(label);
        }
        return cred;
    }

    public void deleteCredentialById(@NonNull UUID id) {
        credRepo.findById(id).ifPresent(c -> {
            boolean isPublic = c.getIsPublic();
            try {
                if (c.getReferent() != null) {
                    ac.credentialRemove(c.getReferent());
                }
            } catch (AriesException | IOException e) {
                // if we fail here its not good, but also no deal breaker, so log and continue
                log.error("Could not delete aca-py credential for referent: {}", c.getReferent(), e);
            }
            credRepo.deleteById(id);
            if (isPublic) {
                vpMgmt.recreateVerifiablePresentation();
            }
        });
    }

    /**
     * Tries to resolve the issuers DID into a human readable name. Resolution order
     * is: 1. Partner alias the user gave 2. Legal name from the partners public
     * profile 3. ACA-PY Label 4. DID
     *
     * @param ariesCred {@link Credential}
     * @return the issuer or null when the credential or the credential definition
     *         id is null
     */
    @Nullable
    String resolveIssuer(@Nullable Credential ariesCred) {
        String issuer = null;
        if (ariesCred != null && StringUtils.isNotEmpty(ariesCred.getCredentialDefinitionId())) {
            String did = didPrefix + AriesStringUtil.credDefIdGetDid(ariesCred.getCredentialDefinitionId());
            Optional<Partner> p = partnerRepo.findByDid(did);
            if (p.isPresent()) {
                if (StringUtils.isNotEmpty(p.get().getAlias())) {
                    issuer = p.get().getAlias();
                } else if (p.get().getVerifiablePresentation() != null) {
                    VerifiablePresentation<VerifiableIndyCredential> vp = conv
                            .fromMap(p.get().getVerifiablePresentation(), Converter.VP_TYPEREF);
                    Optional<VerifiableIndyCredential> profile = vp.getVerifiableCredential()
                            .stream().filter(ic -> ic.getType().contains("OrganizationalProfileCredential")).findAny();
                    if (profile.isPresent() && profile.get().getCredentialSubject() != null) {
                        ProfileVC pVC = mapper.convertValue(profile.get().getCredentialSubject(), ProfileVC.class);
                        issuer = pVC.getLegalName();
                    }
                }
                if (issuer == null && p.get().getIncoming() != null && p.get().getIncoming()) {
                    issuer = p.get().getLabel();
                }
            }
            if (issuer == null) {
                issuer = did;
            }
        }
        return issuer;
    }
}
