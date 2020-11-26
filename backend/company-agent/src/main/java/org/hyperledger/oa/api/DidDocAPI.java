/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
public class DidDocAPI {

    @JsonProperty("@context")
    private final String context = "https://www.w3.org/ns/did/v1";

    private String id;

    private VerificationMethod verificationMethod;

    // TODO not compatible with did:evan, because they use a different context,
    // needs context sensitive parsing
    private Authentication authentication;

    private List<Service> service;

    // TODO not in the did document returned from the universal resolver
    private List<PublicKey> publicKey;

    /**
     * @see <a href=
     *      "https://www.w3.org/TR/did-core/#public-keys">did-core/#public-keys"</a>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class PublicKey {
        private String id;
        private String type;
        private String controller;
        private String publicKeyBase58;
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Authentication {
        private String id;
        private String type;
        private String verificationMethod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class VerificationMethod {
        private String id;
        private String type;
        private String publicKeyBase58;
    }

    public List<PublicKey> getPublicKey() {
        if (publicKey == null) {
            return List.of();
        }
        return publicKey;
    }

    public List<Service> getService() {
        if (service == null) {
            return List.of();
        }
        return service;
    }
}