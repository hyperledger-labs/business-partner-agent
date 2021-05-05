package org.hyperledger.bpa.impl.aries;

import lombok.NonNull;

import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.pojo.PojoProcessor;

import javax.annotation.Nullable;
import java.util.*;
import com.google.gson.JsonObject;
import java.util.stream.Collectors;

public class PresentationRequestHelper {

    /**
     * Returns a valid PresentationRequest if possible (no preference)
     * 
     * @param presentationExchange {@link PresentationExchangeRecord}
     * @param validCredentials {@link List<PresentationRequestCredentials>}
     * @return {@link PresentationRequest}
     */
    public static Optional<PresentationRequest> buildAny(
            PresentationExchangeRecord presentationExchange,
            List<PresentationRequestCredentials> validCredentials) {
        return Optional.of(PresentationRequest.builder()
                .requestedAttributes(buildRequestedAttributes(
                        presentationExchange.getPresentationRequest().getRequestedAttributes(),
                        validCredentials))
                .requestedPredicates(buildRequestedPredicates(
                        presentationExchange.getPresentationRequest().getRequestedPredicates(), 
                        validCredentials))
                .build());
    }

    public static Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> buildRequestedAttributes(
            Map<String, PresentProofRequest.ProofRequest.ProofAttributes> requestedAttributes,
            List<PresentationRequestCredentials> validCredentials) {

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
                log.info("No cred found that satisfies proof request restrictions");
            });
        }

        return result;

    };

    public static Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> buildRequestedPredicates(
            Map<String, PresentProofRequest.ProofRequest.ProofAttributes> requestedPredicates,
            List<PresentationRequestCredentials> validCredentials) {
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
                log.info("No cred found that satisfies proof request restrictions");
            });
        }

        return result;
    };

}
