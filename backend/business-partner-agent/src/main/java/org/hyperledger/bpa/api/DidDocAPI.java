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
package org.hyperledger.bpa.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.*;
import org.hyperledger.aries.api.ledger.EndpointType;

import java.util.List;
import java.util.Optional;

/**
 * API Representation of a did document.
 *
 * @see <a href="https://www.w3.org/TR/did-core">did-core</a>
 * @see <a href="https://w3c-ccg.github.io/did-method-web">did-method-web</a>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({ "context", "id", "verificationMethod", "authentication", "service" })
public class DidDocAPI {

    private static final TypeReference<List<VerificationMethod>> VM_TYPEREF = new TypeReference<>() {
    };
    private static final TypeReference<List<Authentication>> AU_TYPEREF = new TypeReference<>() {
    };

    @JsonProperty("@context")
    private final String context = "https://www.w3.org/ns/did/v1";

    private String id;

    private JsonNode verificationMethod;

    private JsonNode authentication;

    private List<Service> service;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class VerificationMethod {
        private String id;
        private String type;
        private String publicKeyBase58;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Authentication {
        private String id;
        private String type;
        private String verificationMethod;
    }

    /**
     * @see <a href=
     *      "https://www.w3.org/TR/did-core/#service-endpoints">did-core/#service-endpoints</a>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Service {
        private String id;
        private String type;
        private String serviceEndpoint;
    }

    @Deprecated
    private List<PublicKey> publicKey;

    /**
     * Not part of the did doc's w3c specification any more, but the universal
     * resolver still seems to need this for the did:web driver:
     * 
     * @deprecated {@link VerificationMethod}
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated
    public static final class PublicKey {
        private String id;
        private String type;
        private String publicKeyBase58;
    }

    // workaround for
    // https://github.com/decentralized-identity/uni-resolver-driver-did-sov/issues/2
    public List<VerificationMethod> getVerificationMethod(@NonNull ObjectMapper mapper) {
        List<VerificationMethod> result = List.of();
        if (verificationMethod != null) {
            if (JsonNodeType.OBJECT.equals(verificationMethod.getNodeType())) {
                VerificationMethod meth = mapper.convertValue(verificationMethod, VerificationMethod.class);
                result = List.of(meth);
            } else if (JsonNodeType.ARRAY.equals(verificationMethod.getNodeType())) {
                result = mapper.convertValue(verificationMethod, VM_TYPEREF);
            }
        }
        return result;
    }

    public List<Service> getService() {
        if (service == null) {
            return List.of();
        }
        return service;
    }

    public Optional<String> findPublicProfileUrl() {
        String url = null;
        Optional<Service> service = getService()
                .stream()
                .filter(s -> EndpointType.PROFILE.getLedgerName().equals(s.getType()))
                .findFirst();
        if (service.isPresent()) {
            url = service.get().getServiceEndpoint();
        }
        return Optional.ofNullable(url);
    }

    public boolean hasAriesEndpoint() {
        return getService().stream()
                .anyMatch(s -> EndpointType.PROFILE.getLedgerName().equals(s.getType()));
    }
}