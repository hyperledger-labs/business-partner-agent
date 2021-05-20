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
import org.hyperledger.bpa.api.aries.AriesProof;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.api.exception.PresentationConstructionException;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.controller.api.partner.ProofRequestsRequest;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPAPresentationExchange;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.BPAPresentationExchangeRepository;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class ProofManager {

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
    BPAPresentationExchangeRepository peRepo;

    @Inject
    Converter conv;

    @Inject
    SchemaService schemaService;

    @Inject
    DidResolver didRes;

    @Inject
    MessageService messageService;

    public List<BPAPresentationExchange> getProofRequests(Optional<UUID> partnerId,
            @Nullable ProofRequestsRequest req) {
        List<BPAPresentationExchange> result = new ArrayList<>();
        try {
            partnerId.ifPresentOrElse((pId) -> {
                peRepo.findByPartnerId(pId).forEach(result::add);
            }, () -> {
                peRepo.findAll().forEach(result::add);
            });
        } catch (EntityNotFoundException e) {
            throw new PartnerException("Partner not found");
        }
        return result;
    }

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
            throw new NetworkException("aca-py not available", e);
        }
    }

    public void rejectPresentProofRequest(UUID BPAPresentationExchangeId, String explainString) {
        peRepo.findById(BPAPresentationExchangeId).ifPresentOrElse(pe -> {
            try {
                sendPresentProofProblemReport(pe.getPresentationExchangeId(), explainString);
                // after sending rejection notice, delete aries copy.
                ac.presentProofRecordsRemove(pe.getPresentationExchangeId());
                // after sending removing aries copy, remove BPA copy

            } catch (IOException e) {
                log.error("aca-py not reachable.", e);
                return;
            } catch (AriesException e) {
                log.error("aca-py wallet item not found, attempting BPA delete");
            }
            // TODO preserve rejected presentations.
            // rejected is currently not a valid state in the aries present_proof_v1.0
            // protocol
            peRepo.delete(pe);
        }, () -> {
            log.error("BPA has no record of presentationexchange with id: {]", BPAPresentationExchangeId);
        });

    }

    public void presentProof(UUID BPAPresentationExchangeId) {
        peRepo.findById(BPAPresentationExchangeId).ifPresentOrElse(pe -> {
            try {
                ac.presentProofRecordsGetById(pe.getPresentationExchangeId()).ifPresentOrElse(
                        (record) -> {
                            presentProof(record);
                        },
                        () -> {
                            log.error("PresentationExchangeRecord not found with Id: {}",
                                    pe.getPresentationExchangeId());
                        });
            } catch (IOException e) {
                log.error("aca-py not reachable.", e);
                return;
            }
            peRepo.updateState(pe.getId(), PresentationExchangeState.PRESENTATIONS_SENT);
            // copy this into a partnerproof? but the aries ACK should do that i think?
        }, () -> {
            log.error("No BPAPresentationExchange with id: {}", BPAPresentationExchangeId);
        });
        // TODO, update controller to take BPA id (primary key) instead of acapy
        // presentationExchangeId

    }

    // respond to proof request with valid proof
    private void presentProof(PresentationExchangeRecord presentationExchangeRecord) {
        // verify correct state = `request_received`
        if (presentationExchangeRecord.getState() == PresentationExchangeState.REQUEST_RECEIVED) {
            // find vc's in wallet that satisfy proof
            Optional<List<PresentationRequestCredentials>> validCredentials = Optional.empty();

            try {
                validCredentials = ac
                        .presentProofRecordsCredentials(presentationExchangeRecord.getPresentationExchangeId());
            } catch (IOException e) {
                log.error("Could not create aries connection invitation", e);
                return;
            }

            try {
                Optional<PresentationRequest> presentation = PresentationRequestHelper.buildAny(
                        presentationExchangeRecord,
                        validCredentials.get());
                presentation.ifPresentOrElse((pres) -> {
                    try {
                        ac.presentProofRecordsSendPresentation(presentationExchangeRecord.getPresentationExchangeId(),
                                pres);
                    } catch (IOException e) {
                        log.error("Could not create aries connection invitation", e);
                        return;
                    }
                }, () -> {
                    log.error("Could not construct valid proof");
                });
            } catch (PresentationConstructionException e) {
                // unable to construct valid proof
                log.error("Unable to construct valid proof");
                return;
            }

        }

    }

    // handles all proof events to track state changes
    public void handleProofEvent(PresentationExchangeRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId())
                .ifPresent(p -> pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId())
                        .ifPresentOrElse(pp -> pProofRepo.updateState(pp.getId(), proof.getState()), () -> {
                            if (PresentationExchangeState.PROPOSAL_RECEIVED.equals(proof.getState())) {
                                final PartnerProof pp = PartnerProof
                                        .builder()
                                        .partnerId(p.getId())
                                        .state(proof.getState())
                                        .presentationExchangeId(proof.getPresentationExchangeId())
                                        .role(proof.getRole())
                                        .build();
                                pProofRepo.save(pp);
                            }
                        }));
    }

    // handles all proof request
    public void handleProofRequestEvent(PresentationExchangeRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId()).ifPresentOrElse(
                p -> {
                    peRepo.findByPresentationExchangeId(proof.getPresentationExchangeId())
                            .ifPresentOrElse(pe -> {
                                // already a BPA record for this presentation exchange
                            }, () -> {
                                // new presentationExchangeID
                                if (PresentationExchangeRole.PROVER.equals(proof.getRole())
                                        && PresentationExchangeState.REQUEST_RECEIVED.equals(proof.getState())) {
                                    // brand new receive
                                    final BPAPresentationExchange pe = BPAPresentationExchange.builder()
                                            .partner(p)
                                            .state(PresentationExchangeState.REQUEST_RECEIVED)
                                            .presentationExchangeId(proof.getPresentationExchangeId())
                                            .role(proof.getRole())
                                            .presentationRequest(proof.getPresentationRequest())
                                            .threadId(proof.getThreadId())
                                            .build();
                                    peRepo.save(pe);
                                } else {
                                    // some other initial
                                    log.warn("Found some unexpected initial state for PresentationExchangeRecord");
                                }
                            });
                }, () -> {
                    // error, connection ID doesn't have BPA level partner record (really bad)
                });

        messageService.sendMessage(WebSocketMessageBody.proofRequestReceived(proof));
    }

    // handle all acked or verified proof events
    // connectionless proofs are currently not handled
    public void handleAckedOrVerifiedProofEvent(PresentationExchangeRecord proof) {
        pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId()).ifPresent(pp -> {
            if (CollectionUtils.isNotEmpty(proof.getIdentifiers())) {
                // TODO first schema id for now
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
                messageService.sendMessage(WebSocketMessageBody.proofReceived(toApiProof(savedProof)));
            }
        });
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

    public List<AriesProof> listPartnerProofs(@NonNull UUID partnerId) {
        List<AriesProof> result = new ArrayList<>();
        pProofRepo.findByPartnerIdOrderByRole(partnerId).forEach(p -> result.add(toApiProof(p)));
        return result;
    }

    public Optional<AriesProof> getPartnerProofById(@NonNull UUID id) {
        Optional<AriesProof> result = Optional.empty();
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
                log.error("aca-py not reachable", e);
            }
            pProofRepo.deleteById(id);
        });
    }

    private @Nullable String resolveIssuer(String credDefId) {
        String issuer = null;
        if (StringUtils.isNotEmpty(credDefId)) {
            issuer = didPrefix + AriesStringUtil.credDefIdGetDid(credDefId);
        }
        return issuer;
    }

    private AriesProof toApiProof(@NonNull PartnerProof p) {
        AriesProof proof = AriesProof.from(p, p.getProof() != null ? conv.fromMap(p.getProof(), JsonNode.class) : null);
        if (StringUtils.isNotEmpty(p.getSchemaId())) {
            proof.setTypeLabel(schemaService.getSchemaLabel(p.getSchemaId()));
        }
        return proof;
    }

    private void sendPresentProofProblemReport(@NotNull String PresentationExchangeId, @NotNull String problemString)
            throws IOException {
        PresentationProblemReportRequest request = PresentationProblemReportRequest.builder()
                .explainLtxt(problemString)
                .build();
        ac.presentProofRecordsProblemReport(PresentationExchangeId, request);
    }
}
