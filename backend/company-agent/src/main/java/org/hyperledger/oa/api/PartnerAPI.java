/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.api;

import java.util.List;

import org.hyperledger.aries.api.jsonld.VerifiablePresentation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PartnerAPI {
    private String id;
    private Long createdAt;
    private Long updatedAt;
    private Boolean valid;
    private Boolean ariesSupport;
    private Boolean incoming;
    private String state;
    private String alias;
    private String did;
    private List<PartnerCredential> credential;

    @JsonIgnore // internal use only
    private transient VerifiablePresentation verifiablePresentation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class PartnerCredential {
        private CredentialType type;
        private Boolean indyCredential;
        private String issuer;
        private String schemaId;
        @Schema(example = "{}")
        private JsonNode credentialData;
    }

}
