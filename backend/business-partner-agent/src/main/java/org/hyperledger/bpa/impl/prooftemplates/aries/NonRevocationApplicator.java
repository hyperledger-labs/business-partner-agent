package org.hyperledger.bpa.impl.prooftemplates.aries;

import io.micronaut.core.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.prooftemplates.RevocationTimeStampProvider;

import java.util.Objects;

public class NonRevocationApplicator {
    @Builder.Default
    Boolean applyNonRevocation = false;
    RevocationTimeStampProvider revocationTimeStampProvider;

    PresentProofRequest.ProofRequest.ProofNonRevoked nonRevocation;

    @Builder
    public NonRevocationApplicator(@NonNull Boolean applyNonRevocation,
            @Nullable RevocationTimeStampProvider revocationTimeStampProvider) {
        this.applyNonRevocation = applyNonRevocation;
        if (applyNonRevocation) {
            this.revocationTimeStampProvider = Objects.requireNonNull(revocationTimeStampProvider);
            Long longValue = revocationTimeStampProvider.get();
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
