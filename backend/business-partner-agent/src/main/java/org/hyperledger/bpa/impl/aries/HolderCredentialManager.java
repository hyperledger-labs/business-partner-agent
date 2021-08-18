/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl.aries;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.V20CredExRecord;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.ExchangeVersion;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.AriesCredential.AriesCredentialBuilder;
import org.hyperledger.bpa.api.aries.ProfileVC;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.activity.VPManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.notification.CredentialAddedEvent;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MyCredential;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.MyDocumentRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class HolderCredentialManager {

    @Value("${bpa.did.prefix}")
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
    ObjectMapper mapper;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    ApplicationEventPublisher eventPublisher;

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId) {
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (dbPartner.isPresent()) {
            final Optional<MyDocument> dbDoc = docRepo.findById(myDocId);
            if (dbDoc.isPresent()) {
                if (!CredentialType.INDY.equals(dbDoc.get().getType())) {
                    throw new PartnerException("Only documents that are based on a " +
                            "schema can be converted into a credential");
                }
                try {
                    final Optional<org.hyperledger.bpa.model.BPASchema> s = schemaService
                            .getSchemaFor(dbDoc.get().getSchemaId());
                    if (s.isPresent()) {
                        ac.issueCredentialSendProposal(
                                V1CredentialProposalRequest
                                        .builder()
                                        .connectionId(Objects.requireNonNull(dbPartner.get().getConnectionId()))
                                        .schemaId(s.get().getSchemaId())
                                        .credentialProposal(
                                                new CredentialPreview(
                                                        CredentialAttributes.from(
                                                                Objects.requireNonNull(dbDoc.get().getDocument()))))
                                        .build());
                    } else {
                        throw new PartnerException("No configured schema found for id: " + dbDoc.get().getSchemaId());
                    }
                } catch (IOException e) {
                    throw new NetworkException("No aries connection", e);
                }
            } else {
                throw new PartnerException("No document found for id: " + myDocId);
            }
        } else {
            throw new PartnerException("No partner found for id: " + partnerId);
        }
    }

    // credential visible in public profile
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

    private AriesCredential buildAriesCredential(@NonNull MyCredential dbCred) {
        final AriesCredentialBuilder myCred = AriesCredential.fromMyCredential(dbCred);
        if (dbCred.getCredential() != null) {
            final Credential ariesCred = conv.fromMap(dbCred.getCredential(), Credential.class);
            myCred
                    .schemaId(ariesCred.getSchemaId())
                    .credentialDefinitionId(ariesCred.getCredentialDefinitionId())
                    .revocable(StringUtils.isNotEmpty(ariesCred.getRevRegId()))
                    .typeLabel(schemaService.getSchemaLabel(ariesCred.getSchemaId()))
                    .credentialData(ariesCred.getAttrs());
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
                // if we fail here it's not good, but also no deal-breaker, so log and continue
                log.error("Could not delete aca-py credential for referent: {}", c.getReferent(), e);
            }
            credRepo.deleteById(id);
            if (isPublic) {
                vpMgmt.recreateVerifiablePresentation();
            }
        });
    }

    /**
     * Tries to resolve the issuers DID into a human-readable name. Resolution order
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
                            .fromMap(Objects.requireNonNull(p.get().getVerifiablePresentation()), Converter.VP_TYPEREF);
                    Optional<VerifiableIndyCredential> profile = vp.getVerifiableCredential()
                            .stream().filter(ic -> ic.getType().contains("OrganizationalProfileCredential")).findAny();
                    if (profile.isPresent() && profile.get().getCredentialSubject() != null) {
                        ProfileVC pVC = mapper.convertValue(profile.get().getCredentialSubject(), ProfileVC.class);
                        issuer = pVC.getLegalName();
                    }
                }
                if (issuer == null && p.get().getIncoming() != null && Boolean.TRUE.equals(p.get().getIncoming())) {
                    issuer = p.get().getLabel();
                }
            }
            if (issuer == null) {
                issuer = did;
            }
        }
        return issuer;
    }

    /**
     * Scheduled task that checks the revocation status of all credentials issued to
     * this BPA.
     */
    @Scheduled(fixedRate = "5m", initialDelay = "1m")
    public void checkRevocationStatus() {
        log.trace("Running revocation checks");
        credRepo.findNotRevoked().forEach(cred -> {
            try {
                log.trace("Running revocation check for credential exchange: {}", cred.getReferent());
                ac.credentialRevoked(Objects.requireNonNull(cred.getReferent())).ifPresent(isRevoked -> {
                    if (isRevoked.getRevoked() != null && isRevoked.getRevoked()) {
                        credRepo.updateRevoked(cred.getId(), Boolean.TRUE);
                        log.debug("Credential with referent id: {} has been revoked", cred.getReferent());
                    }
                });
            } catch (Exception e) {
                log.error("Revocation check failed", e);
            }
        });
    }

    // credential event handling

    // credential, signed and stored in wallet
    public void handleV1CredentialExchangeAcked(@NonNull V1CredentialExchange credEx) {
        String label = labelStrategy.apply(credEx.getCredential());
        MyCredential dbCred = MyCredential.defaultCredentialBuilder()
                .connectionId(credEx.getConnectionId())
                .threadId(credEx.getThreadId())
                .credentialExchangeId(credEx.getCredentialExchangeId())
                .referent(credEx.getCredential().getReferent())
                .state(credEx.getState())
                .credential(conv.toMap(credEx.getCredential()))
                .label(label)
                .issuer(resolveIssuer(credEx.getCredential()))
                .exchangeVersion(ExchangeVersion.V1)
                .build();
        MyCredential dbCredential = credRepo.save(dbCred);
        fireCredentialAddedEvent(dbCredential);
    }

    public void handleV2CredentialExchangeReceived(@NonNull V20CredExRecord credEx) {
        V2ToV1IndyCredentialConverter.INSTANCE().toV1(credEx).ifPresent(c -> {
            MyCredential dbCred = MyCredential.defaultCredentialBuilder()
                    .connectionId(credEx.getConnectionId())
                    .threadId(credEx.getThreadId())
                    .credentialExchangeId(credEx.getCredExId())
                    .referent(credEx.getCredExId())
                    .state(CredentialExchangeState.fromV2(credEx.getState()))
                    .exchangeVersion(ExchangeVersion.V2)
                    .build();
            credRepo.save(dbCred);
        });
    }

    public void handleV2CredentialExchangeDone(@NonNull V20CredExRecord credEx) {
        credRepo.findByCredentialExchangeId(credEx.getCredExId()).ifPresent(
                dbCred -> V2ToV1IndyCredentialConverter.INSTANCE().toV1(credEx)
                        .ifPresent(c -> {
                            String label = labelStrategy.apply(c);
                            dbCred
                                    .setState(CredentialExchangeState.fromV2(credEx.getState()))
                                    .setCredential(conv.toMap(c))
                                    .setLabel(label)
                                    .setIssuer(resolveIssuer(c));
                            MyCredential dbCredential = credRepo.update(dbCred);
                            fireCredentialAddedEvent(dbCredential);
                        }));
    }

    /**
     * This handler maps the 'stored credential id' from the event to the referent
     * as only this event has this id
     * 
     * @param credentialEvent {@link V2IssueIndyCredentialEvent}
     */
    public void handleIssueCredentialV2Indy(V2IssueIndyCredentialEvent credentialEvent) {
        credRepo.findByCredentialExchangeId(credentialEvent.getCredExId()).ifPresent(bpaEx -> {
            bpaEx.setReferent(credentialEvent.getCredIdStored());
            credRepo.update(bpaEx);
        });
    }

    private void fireCredentialAddedEvent(@NonNull MyCredential updated) {
        AriesCredential ariesCredential = buildAriesCredential(updated);
        eventPublisher.publishEventAsync(CredentialAddedEvent.builder()
                .credential(ariesCredential)
                .build());
    }
}
