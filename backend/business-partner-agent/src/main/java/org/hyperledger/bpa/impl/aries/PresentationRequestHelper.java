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
            List<PresentationRequestCredentials> validCredentials)
            throws PresentationConstructionException {
        Optional<PresentationRequest> result = Optional.empty();
        result = Optional.of(PresentationRequest.builder()
                .requestedAttributes(buildRequestedAttributes(
                        presentationExchange.getPresentationRequest().getRequestedAttributes(),
                        validCredentials))
                .requestedPredicates(buildRequestedPredicates(
                        presentationExchange.getPresentationRequest().getRequestedPredicates(),
                        validCredentials))
                .build());
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
