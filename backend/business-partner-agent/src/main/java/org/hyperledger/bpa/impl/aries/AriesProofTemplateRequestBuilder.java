package org.hyperledger.bpa.impl.aries;

import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateRequestBuilder;

class AriesProofTemplateRequestBuilder extends ProofTemplateRequestBuilder<
        PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
        PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> {
    AriesProofTemplateRequestBuilder() {
        super(name -> PresentProofRequest.ProofRequest.ProofRequestedPredicates.builder().name(name),
                name -> PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder().name(name),
                PresentProofRequest.ProofRequest.ProofRestrictions::builder);
    }

    public PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder joinAttributeWithRestrictions(PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder attribute, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictions) {
        return attribute.restriction(restrictions.build().toJsonObject());
    }

    public PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder joinAttributeWithRestrictions(PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder predicate, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictions) {
        return predicate.restriction(restrictions.build().toJsonObject());
    }
}
