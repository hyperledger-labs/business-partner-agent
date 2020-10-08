/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

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
import org.hyperledger.oa.api.aries.BankAccount;
import org.hyperledger.oa.api.aries.ProfileVC;
import org.hyperledger.oa.api.exception.NetworkException;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.LedgerClient;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.context.annotation.Value;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
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
    SchemaService schemaService;

    @Inject
    Converter conv;

    @Inject
    LedgerClient ledger;

    @Inject
    ObjectMapper mapper;

    public Optional<List<PartnerCredentialType>> getPartnerCredDefs(@NonNull UUID partnerId) {
        Optional<List<PartnerCredentialType>> result = Optional.empty();
        final Optional<Partner> p = partnerRepo.findById(partnerId);
        if (p.isPresent() && StringUtils.isNotEmpty(p.get().getDid())) {
            result = ledger.getCredentialDefinitionIdsForDid(AriesStringUtil.didGetLastSegment(p.get().getDid()));
        }
        return result;
    }

    private Optional<String> findBACredentialDefinitionId(@NonNull UUID partnerId, @NonNull Integer seqNo) {
        Optional<String> result = Optional.empty();

        final Optional<List<PartnerCredentialType>> pct = getPartnerCredDefs(partnerId);
        if (pct.isPresent()) {
            final List<PartnerCredentialType> baCreds = pct.get().stream()
                    .filter(cred -> AriesStringUtil.credDefIdGetSquenceNo(
                            cred.getCredentialDefinitionId()).equals(seqNo.toString()))
                    .collect(Collectors.toList());
            if (baCreds.size() > 0) {
                result = Optional.of(baCreds.get(baCreds.size() - 1).getCredentialDefinitionId());
            }
        }
        return result;
    }

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId) {
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (dbPartner.isPresent()) {
            final Optional<MyDocument> dbDoc = docRepo.findById(myDocId);
            if (dbDoc.isPresent()) {
                if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(dbDoc.get().getType())) {
                    final BankAccount bankAccount = conv.fromMap(dbDoc.get().getDocument(), BankAccount.class);
                    try {
                        final org.hyperledger.oa.model.BPASchema s = schemaService
                                .getSchemaFor(CredentialType.BANK_ACCOUNT_CREDENTIAL);
                        final Optional<String> baCredDefId = findBACredentialDefinitionId(partnerId, s.getSeqNo());
                        if (baCredDefId.isPresent()) {
                            ac.issueCredentialSendProposal(
                                    CredentialProposalRequest
                                            .builder()
                                            .connectionId(dbPartner.get().getConnectionId())
                                            .schemaId(s.getSchemaId())
                                            .credentialProposal(
                                                    new CredentialPreview(
                                                            CredentialAttributes.from(bankAccount)))
                                            .credentialDefinitionId(baCredDefId.get())
                                            .build());
                        } else
                            throw new PartnerException("Found no matching credential definition id. "
                                    + "Partner can not issue bank account credentials");
                    } catch (IOException e) {
                        throw new NetworkException("No aries connection", e);
                    }
                } else {
                    throw new PartnerException("Currently only documents of type BANK_ACCOUNT are supported");
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
                .ifPresentOrElse(cred -> {
                    credRepo.updateState(cred.getId(), credEx.getState());
                }, () -> {
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
    public void handleStroreCredential(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    try {
                        credRepo.updateState(cred.getId(), credEx.getState());
                        // TODO should not be necessary with --auto-store-credential set
                        ac.issueCredentialRecordsStore(credEx.getCredentialExchangeId());
                    } catch (IOException e) {
                        log.error("aca-py not reachable", e);
                    }
                }, () -> log.error("Received store credential event without matching therad id"));
    }

    // credential, signed and stored in wallet
    public void handleCredentialAcked(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    cred
                            .setReferent(credEx.getCredential().getReferent())
                            .setCredential(conv.toMap(credEx.getCredential()))
                            .setType(CredentialType.fromSchemaId(credEx.getSchemaId()))
                            .setState(credEx.getState())
                            .setIssuer(resolveIssuer(credEx.getCredential()))
                            .setIssuedAt(Instant.now());
                    credRepo.update(cred);
                }, () -> log.error("Received credential without matching thread id, credential is not stored."));
    }

    @SuppressWarnings("boxing")
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
        if (dbCred.isPresent()) {
            return Optional.of(buildAriesCredential(dbCred.get()));
        }
        return Optional.empty();
    }

    private AriesCredential buildAriesCredential(MyCredential dbCred) {
        final AriesCredentialBuilder myCred = AriesCredential.fromMyCredential(dbCred);
        if (dbCred.getCredential() != null) {
            final Credential ariesCred = conv.fromMap(dbCred.getCredential(), Credential.class);
            myCred
                    .schemaId(ariesCred.getSchemaId())
                    .credentialDefinitionId(ariesCred.getCredentialDefinitionId())
                    .credentialData(ariesCred.getAttrs());
            // TODO only for backwards compatibility, can be removed at some point
            if (dbCred.getIssuer() == null) {
                myCred.issuer(resolveIssuer(ariesCred));
            }
        }
        return myCred.build();
    }

    public Optional<AriesCredential> updateCredentialById(@NonNull UUID id, @NonNull String label) {
        final Optional<AriesCredential> cred = getAriesCredentialById(id);
        if (cred.isPresent()) {
            credRepo.updateLabel(id, label);
            cred.get().setLabel(label);
        }
        return cred;
    }

    public void deleteCredentialById(@NonNull UUID id) {
        credRepo.findById(id).ifPresent(c -> {
            boolean isPublic = c.getIsPublic().booleanValue();
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
                if (issuer == null && p.get().getIncoming() != null && p.get().getIncoming().booleanValue()) {
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
