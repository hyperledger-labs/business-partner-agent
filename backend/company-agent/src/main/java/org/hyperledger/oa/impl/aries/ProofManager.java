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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.proof.PresentProofConfig;
import org.hyperledger.aries.api.proof.PresentProofRequest;
import org.hyperledger.aries.api.proof.PresentProofRequest.ProofRequest.ProofAttributes.ProofRestrictions;
import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.AriesProof;
import org.hyperledger.oa.api.aries.BankAccount;
import org.hyperledger.oa.api.exception.NetworkException;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.impl.util.TimeUtil;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.PartnerProofRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import com.fasterxml.jackson.databind.JsonNode;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import lombok.NonNull;

@Singleton
@RequiresAries
public class ProofManager {

    @Value("${oagent.did.prefix}")
    private String didPrefix;

    @Inject
    private AriesClient ac;

    @Inject
    private PartnerRepository partnerRepo;

    @Inject
    private PartnerProofRepository pProofRepo;

    @Inject
    private Converter conv;

    // request proof from partner
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull String credDefId) {
        try {
            final Optional<Schema> schema = ac.schemasGetById(AriesStringUtil.credDefIdGetSquenceNo(credDefId));
            if (schema.isPresent()) {
                CredentialType type = CredentialType.fromSchemaId(schema.get().getId());
                if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(type)) {
                    final Optional<Partner> p = partnerRepo.findById(partnerId);
                    if (p.isPresent()) {
                        // only when aries partner
                        if (p.get().getConnectionId() != null) {
                            PresentProofConfig config = PresentProofConfig.builder()
                                    .connectionId(p.get().getConnectionId())
                                    .appendAttribute(BankAccount.class, ProofRestrictions.builder()
                                            .schemaId(schema.get().getId())
                                            .credentialDefinitionId(credDefId)
                                            .build())
                                    .build();
                            ac.presentProofSendRequest(PresentProofRequest.build(config));
                        } else {
                            throw new PartnerException("Partner has no aca-py connection");
                        }
                    } else {
                        throw new PartnerException("Partner not found");
                    }
                } else {
                    throw new PartnerException("Currently only BANK_ACCOUNT is supported");
                }
            } else {
                throw new PartnerException("Could not resolve schema for credential definition id");
            }
        } catch (IOException e) {
            throw new NetworkException("aca-py not reachable", e);
        }
    }

    // partner sent a proof to me
    // connectionless proofs are currently not handled
    public void handleProofEvent(PresentationExchangeRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId()).ifPresent(p -> {
            if (CollectionUtils.isNotEmpty(proof.getIdentifiers())) {
                // TODO first schema id for now
                String schemaId = proof.getIdentifiers().get(0).getSchemaId();
                String credDefId = proof.getIdentifiers().get(0).getCredentialDefinitionId();
                String issuer = null;
                if (StringUtils.isNotEmpty(credDefId)) {
                    issuer = didPrefix + AriesStringUtil.credDefIdGetDid(credDefId);
                }
                CredentialType type = CredentialType.fromSchemaId(schemaId);
                if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(type)) {
                    final PartnerProof pp = PartnerProof
                            .builder()
                            .partnerId(p.getId())
                            .issuedAt(TimeUtil.parseZonedTimestamp(proof.getCreatedAt()))
                            .type(type)
                            .valid(Boolean.valueOf(proof.isVerified()))
                            .state(proof.getState())
                            .presentationExchangeId(proof.getPresentationExchangeId())
                            .schemaId(schemaId)
                            .issuer(issuer)
                            .proof(conv.toMap(proof.from(BankAccount.class)))
                            .build();
                    pProofRepo.save(pp);
                }
            }
        });
    }

    public List<AriesProof> listPartnerProofs(UUID partnerId) {
        List<AriesProof> result = new ArrayList<>();
        pProofRepo.findByPartnerId(partnerId).forEach(p -> {
            result.add(AriesProof.from(p, conv.fromMap(p.getProof(), JsonNode.class)));
        });
        return result;
    }

    public Optional<AriesProof> getPartnerProofById(UUID id) {
        Optional<AriesProof> result = Optional.empty();
        final Optional<PartnerProof> proof = pProofRepo.findById(id);
        if (proof.isPresent()) {
            result = Optional.of(AriesProof.from(proof.get(), conv.fromMap(proof.get().getProof(), JsonNode.class)));
        }
        return result;
    }
}
