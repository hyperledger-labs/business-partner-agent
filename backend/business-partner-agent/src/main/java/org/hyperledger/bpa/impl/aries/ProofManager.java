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

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
public class ProofManager {

    private static final String ACA_PY_ERROR_MSG = "aca-py not available";

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository pProofRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    Converter conv;

    @Inject
    SchemaService schemaService;

    @Inject
    DidResolver didRes;

    @Inject
    MessageService messageService;

    // request proof from partner
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull RequestProofRequest req) {
        try {
            final Optional<Partner> p = partnerRepo.findById(partnerId);
            if (p.isPresent()) {
                // only when aries partner
                if (p.get().hasConnectionId()) {
                    if (req.isRequestBySchema()) {
                        final Optional<Schema> schema = ac.schemasGetById(req.getRequestBySchema().getSchemaId());
                        if (schema.isPresent()) {
                            PresentProofRequest proofRequest = PresentProofRequestHelper
                                    .buildForAllAttributes(p.get().getConnectionId(),
                                            schema.get().getAttrNames(), req.buildRestrictions());
                            ac.presentProofSendRequest(proofRequest).ifPresent(proof -> {
                                final PartnerProof pp = PartnerProof
                                        .builder()
                                        .partnerId(partnerId)
                                        .state(proof.getState())
                                        .presentationExchangeId(proof.getPresentationExchangeId())
                                        .role(proof.getRole())
                                        .schemaId(schema.get().getId())
                                        .issuer(req.getFirstIssuerDid())
                                        .build();
                                pProofRepo.save(pp);
                            });
                        } else {
                            throw new PartnerException("Could not find any schema on the ledger for id: "
                                    + req.getRequestBySchema().getSchemaId());
                        }
                    } else {
                        ac.presentProofSendRequest(req.getRequestRaw().toString()).ifPresent(exchange -> {
                            final PartnerProof pp = PartnerProof
                                    .builder()
                                    .partnerId(partnerId)
                                    .state(exchange.getState())
                                    .presentationExchangeId(exchange.getPresentationExchangeId())
                                    .role(exchange.getRole())
                                    .build();
                            pProofRepo.save(pp);
                        });
                    }
                } else {
                    throw new PartnerException("Partner has no aca-py connection");
                }
            } else {
                throw new PartnerException("Partner not found");
            }
        } catch (IOException e) {
            throw new NetworkException(ACA_PY_ERROR_MSG, e);
        }
    }

    public void rejectPresentProofRequest(@NotNull PartnerProof proofEx, String explainString) {
        if (PresentationExchangeState.REQUEST_RECEIVED.equals(proofEx.getState())) {
            try {
                sendPresentProofProblemReport(proofEx.getPresentationExchangeId(), explainString);
                deletePartnerProof(proofEx.getId());
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            } catch (AriesException e) {
                log.error("aca-py wallet item not found, attempting BPA delete");
            }
        } else {
            throw new WrongApiUsageException("PresentationExchangeState != 'request-received'");
        }
    }

    public void presentProof(@NotNull PartnerProof proofEx) {
        if (PresentationExchangeRole.PROVER.equals(proofEx.getRole())
                && PresentationExchangeState.REQUEST_RECEIVED.equals(proofEx.getState())) {
            try {
                ac.presentProofRecordsGetById(proofEx.getPresentationExchangeId()).ifPresent(this::presentProof);
            } catch (IOException e) {
                log.error(ACA_PY_ERROR_MSG, e);
            }
        } else {
            throw new WrongApiUsageException(
                    "PresentationExchangeRole!= 'prover' or PresentationExchangeState != 'request-received'");
        }
    }

    void presentProof(@NonNull PresentationExchangeRecord presentationExchangeRecord) {
        if (PresentationExchangeState.REQUEST_RECEIVED.equals(presentationExchangeRecord.getState())) {
            try {
                ac.presentProofRecordsCredentials(presentationExchangeRecord.getPresentationExchangeId())
                        .ifPresentOrElse(creds -> {
                            if (CollectionUtils.isNotEmpty(creds)) {
                                PresentationRequestBuilder.acceptAll(presentationExchangeRecord, creds)
                                        .ifPresent(pr -> {
                                            try {
                                                ac.presentProofRecordsSendPresentation(
                                                        presentationExchangeRecord.getPresentationExchangeId(),
                                                        pr);
                                            } catch (IOException e) {
                                                log.error(ACA_PY_ERROR_MSG, e);
                                            }
                                        });
                            } else {
                                String msg = "No matching credentials found for proof request: "
                                        + presentationExchangeRecord.getPresentationExchangeId();
                                log.warn(msg);
                                pProofRepo.findByPresentationExchangeId(
                                        presentationExchangeRecord.getPresentationExchangeId())
                                        .ifPresent(pp -> pProofRepo.updateProblemReport(pp.getId(), msg));
                            }
                        }, () -> log.error("Could not load matching credentials from aca-py"));
            } catch (IOException e) {
                log.error(ACA_PY_ERROR_MSG, e);
            }
        }
    }

    PartnerProof handleAckedOrVerifiedProofEvent(@NonNull PresentationExchangeRecord proof, @NonNull PartnerProof pp) {
        // TODO first schema id for now as the UI can not handle more
        String schemaId = proof.getIdentifiers().get(0).getSchemaId();
        String credDefId = proof.getIdentifiers().get(0).getCredentialDefinitionId();
        String issuer = resolveIssuer(credDefId);
        pp
                .setIssuedAt(TimeUtil.parseZonedTimestamp(proof.getCreatedAt()))
                .setValid(proof.isVerified())
                .setState(proof.getState())
                .setSchemaId(schemaId)
                .setCredentialDefinitionId(credDefId)
                .setIssuer(issuer)
                .setProof(proof.from(schemaService.getSchemaAttributeNames(schemaId)));
        final PartnerProof savedProof = pProofRepo.update(pp);
        didRes.resolveDid(savedProof);
        return savedProof;
    }

    public void sendProofProposal(@NonNull UUID partnerId, @NonNull UUID myCredentialId) {
        partnerRepo.findById(partnerId).ifPresent(p -> credRepo.findById(myCredentialId).ifPresent(c -> {
            Credential cred = conv.fromMap(c.getCredential(), Credential.class);
            final PresentProofProposal req = PresentProofProposalBuilder.fromCredential(p.getConnectionId(), cred);
            try {
                ac.presentProofSendProposal(req).ifPresent(proof -> {
                    final PartnerProof pp = PartnerProof
                            .builder()
                            .partnerId(partnerId)
                            .state(proof.getState())
                            .presentationExchangeId(proof.getPresentationExchangeId())
                            .role(proof.getRole())
                            .credentialDefinitionId(cred.getCredentialDefinitionId())
                            .schemaId(cred.getSchemaId())
                            .issuer(resolveIssuer(cred.getCredentialDefinitionId()))
                            .build();
                    pProofRepo.save(pp);
                });

            } catch (IOException e) {
                log.error("aca-py not reachable.", e);
            }
        }));
    }

    public List<AriesProofExchange> listPartnerProofs(@NonNull UUID partnerId) {
        List<AriesProofExchange> result = new ArrayList<>();
        pProofRepo.findByPartnerIdOrderByRole(partnerId).forEach(p -> result.add(toApiProof(p)));
        return result;
    }

    public Optional<AriesProofExchange> getPartnerProofById(@NonNull UUID id) {
        Optional<AriesProofExchange> result = Optional.empty();
        final Optional<PartnerProof> proof = pProofRepo.findById(id);
        if (proof.isPresent()) {
            result = Optional.of(toApiProof(proof.get()));
        }
        return result;
    }

    public void deletePartnerProof(@NonNull UUID id) {
        pProofRepo.findById(id).ifPresent(pp -> {
            try {
                ac.presentProofRecordsRemove(pp.getPresentationExchangeId());
            } catch (IOException e) {
                log.error(ACA_PY_ERROR_MSG, e);
            } catch (AriesException e) {
                if (e.getCode() == 404) {
                    log.warn("ACA-py PresentationExchange not found, still deleting BPA Partner Proof");
                } else {
                    throw e;
                }
            }
            pProofRepo.deleteById(id);
        });
    }

    void sendMessage(
            @NonNull WebSocketMessageBody.WebSocketMessageState state,
            @NonNull WebSocketMessageBody.WebSocketMessageType type,
            @NonNull PartnerProof pp) {
        messageService.sendMessage(WebSocketMessageBody.proof(state, type, toApiProof(pp)));
    }

    private @Nullable String resolveIssuer(String credDefId) {
        String issuer = null;
        if (StringUtils.isNotEmpty(credDefId)) {
            issuer = didPrefix + AriesStringUtil.credDefIdGetDid(credDefId);
        }
        return issuer;
    }

    private AriesProofExchange toApiProof(@NonNull PartnerProof p) {
        AriesProofExchange proof = AriesProofExchange.from(p,
                p.getProof() != null ? conv.fromMap(p.getProof(), JsonNode.class) : null);
        if (StringUtils.isNotEmpty(p.getSchemaId())) {
            proof.setTypeLabel(schemaService.getSchemaLabel(p.getSchemaId()));
        }
        return proof;
    }

    private void sendPresentProofProblemReport(@NonNull String PresentationExchangeId, @NonNull String problemString)
            throws IOException {
        PresentationProblemReportRequest request = PresentationProblemReportRequest.builder()
                .explainLtxt(problemString)
                .build();
        ac.presentProofRecordsProblemReport(PresentationExchangeId, request);
    }
}
