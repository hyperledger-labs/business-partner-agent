package org.hyperledger.bpa.impl.aries;

import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.bpa.api.exception.PresentationConstructionException;

import java.util.*;
import com.google.gson.JsonObject;

public class PresentationRequestHelper {

    /**
     * Returns a valid PresentationRequest if possible (no preference)
     * 
     * @param presentationExchange {@link PresentationExchangeRecord}
     * @param validCredentials     {@link List<PresentationRequestCredentials>}
     * @return {@link PresentationRequest}
     */
    public static Optional<PresentationRequest> buildAny(
            PresentationExchangeRecord presentationExchange,
            List<PresentationRequestCredentials> validCredentials) {
        Optional<PresentationRequest> result = Optional.empty();
        try {
            result = Optional.of(PresentationRequest.builder()
                    .requestedAttributes(buildRequestedAttributes(
                            presentationExchange.getPresentationRequest().getRequestedAttributes(),
                            validCredentials))
                    .requestedPredicates(buildRequestedPredicates(
                            presentationExchange.getPresentationRequest().getRequestedPredicates(),
                            validCredentials))
                    .build());
        } catch (PresentationConstructionException e) {
            // unable to construct valid proof
        }
        return result;

    }

    private static Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> buildRequestedAttributes(
            Map<String, PresentProofRequest.ProofRequest.ProofAttributes> requestedAttributes,
            List<PresentationRequestCredentials> validCredentials)
            throws PresentationConstructionException {

        Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> result = new HashMap<>();

        for (Map.Entry<String, PresentProofRequest.ProofRequest.ProofAttributes> reqAttr : requestedAttributes
                .entrySet()) {
            List<JsonObject> restrictions = reqAttr.getValue().getRestrictions();
            Optional<PresentationRequestCredentials> cred = validCredentials.stream()
                    .filter(vc -> vc.getCredentialInfo().getSchemaId().equals(
                            restrictions.get(0).get("schema_id").getAsString()))
                    .findFirst();

            cred.ifPresentOrElse(c -> {
                result.put(reqAttr.getKey(), PresentationRequest.IndyRequestedCredsRequestedAttr.builder()
                        .credId(c.getCredentialInfo().getReferent())
                        .revealed(true)
                        .build());
            }, () -> {
                throw new PresentationConstructionException("Provided Credentials cannot satisfy proof request");
            });
        }

        return result;

    };

    private static Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> buildRequestedPredicates(
            Map<String, PresentProofRequest.ProofRequest.ProofAttributes> requestedPredicates,
            List<PresentationRequestCredentials> validCredentials)
            throws PresentationConstructionException {
        Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> result = new HashMap<>();

        for (Map.Entry<String, PresentProofRequest.ProofRequest.ProofAttributes> reqPred : requestedPredicates
                .entrySet()) {
            List<JsonObject> restrictions = reqPred.getValue().getRestrictions();
            Optional<PresentationRequestCredentials> cred = validCredentials.stream()
                    .filter(vc -> vc.getCredentialInfo().getSchemaId().equals(
                            restrictions.get(0).get("schema_id").getAsString()))
                    .findFirst();

            cred.ifPresentOrElse(c -> {
                result.put(reqPred.getKey(), PresentationRequest.IndyRequestedCredsRequestedPred.builder()
                        .credId(c.getCredentialInfo().getReferent())
                        .build());
            }, () -> {
                throw new PresentationConstructionException("Provided Credentials cannot satisfy proof request");
            });
        }

        return result;
    };

}
