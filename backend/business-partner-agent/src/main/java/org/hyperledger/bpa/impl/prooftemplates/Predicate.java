package org.hyperledger.bpa.impl.prooftemplates;

import lombok.Builder;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.prooftemplate.BPASchemaRestrictions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Builder
class Predicate {
    String schemaId;
    AtomicInteger sameSchemaCounter;
    NonRevocationApplicator revocationApplicator;
    BPASchemaRestrictions schemaRestrictions;
    String name;
    IndyProofReqPredSpec.PTypeEnum operator;
    Integer value;

    public void addToBuilder(
            BiConsumer<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> builderSink) {
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictionsBuilder = ProofTemplateElementVisitor.asProofRestrictionsBuilder(
                schemaRestrictions);
        PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder builder = PresentProofRequest.ProofRequest.ProofRequestedPredicates
                .builder()
                .name(name)
                .pType(operator)
                .pValue(value)
                .restriction(restrictionsBuilder.schemaId(schemaId).build().toJsonObject());
        String predicateName = schemaId + sameSchemaCounter.incrementAndGet();
        builderSink.accept(predicateName, revocationApplicator.applyOn(builder).build());

    }
}
