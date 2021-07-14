package org.hyperledger.bpa.impl.prooftemplates;

import lombok.Builder;
import lombok.Singular;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.prooftemplate.BPASchemaRestrictions;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Builder
class Attributes {
    String schemaId;
    NonRevocationApplicator revocationApplicator;
    BPASchemaRestrictions schemaRestrictions;
    @Singular
    List<String> names;
    @Singular
    Map<String, String> equals;

    public void addToBuilder(
            BiConsumer<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> builderSink) {
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictionsBuilder = ProofTemplateElementVisitor.asProofRestrictionsBuilder(
                schemaRestrictions);
        equals.forEach(restrictionsBuilder::addAttributeValueRestriction);

        PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder builder = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                .builder()
                .names(names)
                .restriction(restrictionsBuilder.schemaId(schemaId).build().toJsonObject());

        builderSink.accept(schemaId, revocationApplicator.applyOn(builder).build());

    }
}
