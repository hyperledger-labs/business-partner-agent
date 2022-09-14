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
package org.hyperledger.bpa.impl.aries.proof;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.acy_py.generated.model.DIFOptions;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.present_proof_v2.*;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextHelper;
import org.hyperledger.bpa.impl.aries.wallet.Identity;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ProverLDManager extends BaseLDManager {

    @Inject
    AriesClient ac;

    @Inject
    Identity identity;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public V20PresProposalRequest prepareProposal(@NonNull String connectionId,
            @NonNull BPACredentialExchange credEx) {

        Map<UUID, DIFField> fields = buildDifFields(credEx.credentialAttributesToCredentialAttributesList());

        String descriptorId = credEx.getSchema() != null ? credEx.getSchema().resolveSchemaLabelEscaped()
                : UUID.randomUUID().toString();
        String descriptorName = credEx.getSchema() != null ? credEx.getSchema().resolveSchemaLabel()
                : descriptorId;
        V2DIFProofRequest.PresentationDefinition.InputDescriptors id1 = V2DIFProofRequest.PresentationDefinition.InputDescriptors
                .builder()
                .id(descriptorId)
                .name(descriptorName)
                .schema(referentToDescriptor(Objects.requireNonNull(credEx.getReferent())))
                .constraints(V2DIFProofRequest.PresentationDefinition.Constraints.builder()
                        .isHolder(buildDifHolder(fields.keySet()))
                        .fields(List.copyOf(fields.values()))
                        .build())
                .build();

        return V20PresProposalRequest.builder()
                .connectionId(connectionId)
                .autoPresent(Boolean.FALSE) // is ignored with aca-py 0.7.4
                .presentationProposal(V20PresProposalByFormat.builder()
                        .dif(V20PresProposalByFormat.DIFProofProposal.builder()
                                .inputDescriptor(id1)
                                .options(DIFOptions.builder()
                                        .challenge(UUID.randomUUID().toString())
                                        .domain(UUID.randomUUID().toString())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private List<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri> referentToDescriptor(
            @NonNull String referent) {
        List<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri> result = new ArrayList<>();
        try {
            ac.credentialW3C(referent).ifPresent(w3c -> w3c.getExpandedTypes().stream()
                    .map(u -> V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri
                            .builder().uri(u).build())
                    .forEach(result::add));
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    private Map<UUID, DIFField> buildDifFields(@NonNull ArrayList<CredentialAttributes> ldAttributes) {
        return ldAttributes
                .stream()
                .filter(e -> StringUtils.isNotEmpty(e.getValue()))
                .filter(e -> !StringUtils.equalsIgnoreCase("type", e.getName()))
                // TODO: Pass mime-type
                .map(e -> pair(e.getName(), DIFField.Filter.builder()
                        ._const(e.getValue())
                        .build()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    // respond to a presentation request that is based on a proposal this agent sent
    void acceptDifCredentialsFromProposal(@NonNull V20PresExRecord dif, @NonNull String referent) {
        V2DIFProofRequest pr = dif
                .resolveDifPresentationRequest()
                .resetHolderConstraints();
        if (pr.getPresentationDefinition() != null
                && CollectionUtils.isNotEmpty(pr.getPresentationDefinition().getInputDescriptors())) {
            // see prepareProposal(), proposals always use a single input descriptor
            V2DIFProofRequest.PresentationDefinition.InputDescriptors id = pr.getPresentationDefinition()
                    .getInputDescriptors().get(0);
            accept(
                    dif.getPresentationExchangeId(),
                    V20PresSpecByFormatRequest.builder()
                            .dif(DIFPresSpec.builder()
                                    .issuerId(identity.getMyDid())
                                    .presentationDefinition(pr.getPresentationDefinition())
                                    .recordIds(Map.of(id.getId(), List.of(referent)))
                                    .build())
                            .build());
        }
    }

    void acceptSelectedDifCredentials(@NonNull V20PresExRecord dif, @Nullable List<String> referents) {
        List<VerifiableCredential.VerifiableCredentialMatch> matches = matches(dif.getPresentationExchangeId());
        Map<String, List<String>> singleMatches = matches.stream()
                .filter(m -> CollectionUtils.isEmpty(referents) || referents.contains(m.getRecordId()))
                .map(m -> new AbstractMap.SimpleEntry<>(LDContextHelper.findSchemaId(m), List.of(m.getRecordId())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (CollectionUtils.isNotEmpty(singleMatches)) {
            // set holder to null
            V2DIFProofRequest presentationRequest = dif
                    .resolveDifPresentationRequest()
                    .resetHolderConstraints();
            accept(
                    dif.getPresentationExchangeId(),
                    V20PresSpecByFormatRequest.builder()
                            .dif(DIFPresSpec.builder()
                                    .issuerId(identity.getMyDid())
                                    .presentationDefinition(presentationRequest.getPresentationDefinition())
                                    .recordIds(singleMatches)
                                    .build())
                            .build());
        }
    }

    private void accept(@NonNull String presentationExchangeId, @NonNull V20PresSpecByFormatRequest req) {
        try {
            ac.presentProofV2RecordsSendPresentation(presentationExchangeId, req);
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        }
    }

    private List<VerifiableCredential.VerifiableCredentialMatch> matches(@NonNull String presentationExchangeId) {
        try {
            return ac.presentProofV2RecordsCredentialsDif(presentationExchangeId, null).orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        }
    }
}