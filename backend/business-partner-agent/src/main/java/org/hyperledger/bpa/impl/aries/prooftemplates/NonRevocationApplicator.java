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
package org.hyperledger.bpa.impl.aries.prooftemplates;

import io.micronaut.core.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;

import java.util.Objects;

public class NonRevocationApplicator {

    private final Boolean applyNonRevocation;

    private PresentProofRequest.ProofRequest.ProofNonRevoked nonRevocation;

    @Builder
    public NonRevocationApplicator(@NonNull Boolean applyNonRevocation,
            @Nullable RevocationTimeStampProvider revocationTimeStampProvider) {
        this.applyNonRevocation = applyNonRevocation;
        if (applyNonRevocation) {
            Long longValue = Objects.requireNonNull(revocationTimeStampProvider).get();
            nonRevocation = PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                    .from(longValue)
                    .to(longValue)
                    .build();
        }
    }

    public PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder applyOn(
            PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder attributes) {
        if (applyNonRevocation) {
            return attributes.nonRevoked(nonRevocation);
        }
        return attributes;
    }

    public PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder applyOn(
            PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder predicates) {
        if (applyNonRevocation) {
            return predicates.nonRevoked(nonRevocation);
        }
        return predicates;
    }
}
