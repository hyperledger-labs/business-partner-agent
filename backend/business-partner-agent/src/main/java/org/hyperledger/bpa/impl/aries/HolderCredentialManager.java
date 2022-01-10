/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.*;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.ProfileVC;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.BaseCredentialManager;
import org.hyperledger.bpa.impl.BaseHolderManager;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.activity.VPManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.HolderCredExRepository;
import org.hyperledger.bpa.repository.IssuerCredExRepository;
import org.hyperledger.bpa.repository.MyDocumentRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class HolderCredentialManager extends BaseHolderManager implements BaseCredentialManager {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    @Getter
    AriesClient ac;

    @Inject
    @Getter
    PartnerRepository partnerRepo;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    @Getter
    IssuerCredExRepository issuerCredExRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    SchemaService schemaService;

    @Inject
    Converter conv;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    @Getter
    BPAMessageSource.DefaultMessageSource msg;

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId,
            @Nullable ExchangeVersion version) {
        Partner dbPartner = partnerRepo.findById(partnerId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.partner.not.found", Map.of("id", partnerId))));
        MyDocument dbDoc = docRepo.findById(myDocId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.document.not.found", Map.of("id", myDocId))));
        if (!CredentialType.INDY.equals(dbDoc.getType())) {
            throw new PartnerException(msg.getMessage("api.schema.credential.document.conversion.failure"));
        }
        try {
            BPASchema s = schemaService.getSchemaFor(dbDoc.getSchemaId())
                    .orElseThrow(
                            () -> new PartnerException(msg.getMessage("api.schema.restriction.schema.not.found",
                                    Map.of("id", dbDoc.getSchemaId()))));
            V1CredentialProposalRequest v1CredentialProposalRequest = V1CredentialProposalRequest
                    .builder()
                    .connectionId(Objects.requireNonNull(dbPartner.getConnectionId()))
                    .schemaId(s.getSchemaId())
                    .credentialProposal(
                            new CredentialPreview(
                                    CredentialAttributes.from(
                                            Objects.requireNonNull(dbDoc.getDocument()))))
                    .build();
            BPACredentialExchange.BPACredentialExchangeBuilder dbCredEx = BPACredentialExchange
                    .builder()
                    .partner(dbPartner)
                    .schema(s)
                    .state(CredentialExchangeState.PROPOSAL_SENT)
                    .pushStateChange(CredentialExchangeState.PROPOSAL_SENT, Instant.now())
                    .role(CredentialExchangeRole.HOLDER);
            if (version == null || ExchangeVersion.V1.equals(version)) {
                ac.issueCredentialSendProposal(v1CredentialProposalRequest).ifPresent(v1 -> dbCredEx
                        .threadId(v1.getThreadId())
                        .credentialExchangeId(v1.getCredentialExchangeId())
                        .credentialProposal(BPACredentialExchange.ExchangePayload
                                .indy(v1.getCredentialProposalDict().getCredentialProposal())));
            } else {
                ac.issueCredentialV2SendProposal(v1CredentialProposalRequest).ifPresent(v2 -> dbCredEx
                        .threadId(v2.getThreadId())
                        .credentialExchangeId(v2.getCredentialExchangeId())
                        .exchangeVersion(ExchangeVersion.V2)
                        .credentialProposal(BPACredentialExchange.ExchangePayload
                                .indy(V2ToV1IndyCredentialConverter.INSTANCE().toV1Proposal(v2)
                                        .getCredentialProposalDict()
                                        .getCredentialProposal())));
            }
            holderCredExRepo.save(dbCredEx.build());
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    // credential visible in public profile
    public void toggleVisibility(UUID id) {
        BPACredentialExchange cred = holderCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        holderCredExRepo.updateIsPublic(id, !cred.checkIfPublic());
        vpMgmt.recreateVerifiablePresentation();
    }

    // credential CRUD operations

    public List<AriesCredential> listCredentials() {
        return holderCredExRepo.findByRoleEqualsAndStateIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE))
                .stream()
                .map(this::buildCredential)
                .collect(Collectors.toList());
    }

    public AriesCredential getCredentialById(@NonNull UUID id) {
        return holderCredExRepo.findById(id).map(this::buildCredential).orElseThrow(EntityNotFoundException::new);
    }

    private AriesCredential buildCredential(@NonNull BPACredentialExchange dbCred) {
        String typeLabel = null;
        if (dbCred.getIndyCredential() != null) {
            typeLabel = schemaService.getSchemaLabel(dbCred.getIndyCredential().getSchemaId());
        }
        return AriesCredential.fromBPACredentialExchange(dbCred, typeLabel);
    }

    /**
     * Updates the credentials label
     *
     * @param id    the credential id
     * @param label the credentials label
     * @return the updated credential if found
     */
    public AriesCredential updateCredentialById(@NonNull UUID id, @Nullable String label) {
        final AriesCredential cred = getCredentialById(id);
        String mergedLabel = labelStrategy.apply(label, cred);
        holderCredExRepo.updateLabel(id, mergedLabel);
        cred.setLabel(label);
        return cred;
    }

    public void deleteCredentialById(@NonNull UUID id) {
        holderCredExRepo.findById(id).ifPresent(c -> {
            boolean isPublic = c.checkIfPublic();
            try {
                if (c.getReferent() != null) {
                    ac.credentialRemove(c.getReferent());
                }
            } catch (AriesException | IOException e) {
                // if we fail here it's not good, but also no deal-breaker, so log and continue
                log.error("Could not delete aca-py credential for referent: {}", c.getReferent(), e);
            }
            holderCredExRepo.deleteById(id);
            if (isPublic) {
                vpMgmt.recreateVerifiablePresentation();
            }
        });
    }

    public void declineCredentialOffer(@NonNull UUID id, @Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            message = msg.getMessage("api.holder.credential.exchange.declined");
        }
        BPACredentialExchange dbEx = getCredentialExchange(id);
        dbEx.pushStates(CredentialExchangeState.DECLINED, Instant.now());
        holderCredExRepo.updateStates(dbEx.getId(), dbEx.getState(), dbEx.getStateToTimestamp(), message);
        declineCredentialExchange(dbEx, message);
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
                        ProfileVC pVC = GsonConfig.jacksonBehaviour().fromJson(profile.get().getCredentialSubject(),
                                ProfileVC.class);
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
    @Scheduled(fixedDelay = "5m", initialDelay = "1m")
    void checkRevocationStatus() {
        log.trace("Running revocation checks");
        holderCredExRepo.findNotRevoked().parallelStream().forEach(cred -> {
            try {
                log.trace("Running revocation check for credential exchange: {}", cred.getReferent());
                ac.credentialRevoked(Objects.requireNonNull(cred.getReferent())).ifPresent(isRevoked -> {
                    if (isRevoked.getRevoked() != null && isRevoked.getRevoked()) {
                        cred.pushStates(CredentialExchangeState.REVOKED, Instant.now());
                        holderCredExRepo.updateRevoked(cred.getId(), Boolean.TRUE, cred.getStateToTimestamp());
                        log.debug("Credential with referent id: {} has been revoked", cred.getReferent());
                    }
                });
            } catch (AriesException e) {
                if (e.getCode() == 404) {
                    log.error("aca-py has no credential with referent id: {}", cred.getReferent());
                    holderCredExRepo.updateReferent(cred.getId(), null);
                }
            } catch (Exception e) {
                log.error("Revocation check failed", e);
            }
        });
    }

    // credential event handling

    @Override
    public BPASchema checkSchema(BaseCredExRecord credEx) {
        String schemaId = ((V1CredentialExchange) credEx).getSchemaId();
        BPASchema bpaSchema = schemaService.getSchemaFor(schemaId).orElse(null);
        if (bpaSchema == null) {
            SchemaAPI schemaAPI = schemaService.addIndySchema(schemaId, null, null);
            if (schemaAPI != null) {
                bpaSchema = BPASchema.builder().id(schemaAPI.getId()).build();
            }
        }
        return bpaSchema;
    }

    // v1 credential, signed and stored in wallet
    public void handleV1CredentialExchangeAcked(@NonNull V1CredentialExchange credEx) {
        String label = labelStrategy.apply(credEx.getCredential());
        holderCredExRepo.findByCredentialExchangeId(credEx.getCredentialExchangeId()).ifPresent(db -> {
            db
                    .setReferent(credEx.getCredential() != null ? credEx.getCredential().getReferent() : null)
                    .setIndyCredential(credEx.getCredential())
                    .setLabel(label)
                    .setIssuer(resolveIssuer(credEx.getCredential()))
                    .pushStates(credEx.getState(), TimeUtil.fromISOInstant(credEx.getUpdatedAt()));
            holderCredExRepo.update(db);
            fireCredentialAddedEvent(db);
        });
    }

    // v2 credential, signed and stored in wallet
    public void handleV2CredentialReceived(@NonNull V20CredExRecord credEx) {
        holderCredExRepo.findByCredentialExchangeId(credEx.getCredentialExchangeId()).ifPresent(
                dbCred -> V2ToV1IndyCredentialConverter.INSTANCE().toV1Credential(credEx)
                        .ifPresent(c -> {
                            String label = labelStrategy.apply(c);
                            dbCred
                                    .pushStates(credEx.getState(), credEx.getUpdatedAt())
                                    .setIndyCredential(c)
                                    .setLabel(label)
                                    .setIssuer(resolveIssuer(c));
                            BPACredentialExchange dbCredential = holderCredExRepo.update(dbCred);
                            fireCredentialAddedEvent(dbCredential);
                        }));
    }
}
