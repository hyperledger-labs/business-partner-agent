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
import io.micronaut.core.util.CollectionUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.model.Partner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private Boolean trustPing;
    private Boolean valid;
    private Boolean ariesSupport;
    private Boolean incoming;
    private ConnectionState state;
    private String alias;
    private String label;
    private String did;
    private List<PartnerCredential> credential;
    private List<TagAPI> tag;

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
                .setTrustPing(from.getTrustPing())
                .setAlias(from.getAlias())
                .setLabel(from.getLabel())
                .setDid(from.getDid())
                .setIncoming(from.getIncoming() != null ? from.getIncoming() : Boolean.FALSE)
                .setTag(from.getTags() != null
                        ? from.getTags().stream().map(TagAPI::from).collect(Collectors.toList())
                        : null);
    }

    /**
     * Virtual partner name field that is calculated from the state of the Partner
     * in the following order: 1. Alias set by a user (set when creating the
     * connection, or editable when clicking on the pencil in the partners details)
     * 2. Legal Name from public profile if set 3. aca-py label, --label flag or
     * overwritten when creating the connection with the label option 4. did, public
     * or peer
     * 
     * @return the partners name or null if no match was found
     */
    public String getName() {
        if (StringUtils.isNotEmpty(alias)) {
            return alias;
        }
        if (CollectionUtils.isNotEmpty(credential)) {
            Optional<String> legalName = credential
                    .stream()
                    .filter(c -> CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(c.getType()))
                    .map(PartnerCredential::getCredentialData)
                    .map(json -> {
                        JsonNode nameNode = json.get("legalName");
                        if (nameNode != null) {
                            String ln = nameNode.asText();
                            if (StringUtils.isNotEmpty(ln)) {
                                return ln;
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst();
            if (legalName.isPresent()) {
                return legalName.get();
            }
        }
        if (StringUtils.isNotEmpty(label)) {
            return label;
        }
        if (StringUtils.isNotEmpty(did)) {
            return did;
        }
        return null;
    }

}
