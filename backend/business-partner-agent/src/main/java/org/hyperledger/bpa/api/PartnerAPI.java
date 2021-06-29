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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.model.Partner;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PartnerAPI {
    private String id;
    private Long createdAt;
    private Long updatedAt;
    private Long lastSeen;
    private Boolean valid;
    private Boolean ariesSupport;
    private Boolean incoming;
    private ConnectionState state;
    private String alias;
    private String did;
    private List<PartnerCredential> credential;

    // begin: internal use only
    @JsonIgnore
    private transient VerifiablePresentation<VerifiableIndyCredential> verifiablePresentation;
    @JsonIgnore
    private transient DIDDocument didDocAPI;
    // end: internal use only

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class PartnerCredential {
        private CredentialType type;
        private String typeLabel;
        private Boolean indyCredential;
        private String issuer;
        private String schemaId;
        @Schema(example = "{}")
        private JsonNode credentialData;
    }

    public static PartnerAPI from(Partner p) {
        PartnerAPI result = new PartnerAPI();
        copyFrom(result, p);
        return result;
    }

    public static void copyFrom(@NonNull PartnerAPI to, @NonNull Partner from) {
        to
                .setCreatedAt(from.getCreatedAt() != null ? from.getCreatedAt().toEpochMilli() : null)
                .setUpdatedAt(from.getUpdatedAt() != null ? from.getUpdatedAt().toEpochMilli() : null)
                .setLastSeen(from.getLastSeen() != null ? from.getLastSeen().toEpochMilli() : null)
                .setId(from.getId() != null ? from.getId().toString() : null)
                .setValid(from.getValid())
                .setAriesSupport(from.getAriesSupport())
                .setState(from.getState())
                .setAlias(from.getAlias())
                .setDid(from.getDid())
                .setIncoming(from.getIncoming() != null ? from.getIncoming() : Boolean.FALSE);
    }

}
