/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.controller.api.issuer;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.api.CredentialType;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Introspected
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        defaultImpl = IssueCredentialRequest.IssueIndyCredentialRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = IssueCredentialRequest.IssueIndyCredentialRequest.class, names = { "INDY", "indy" }),
        @JsonSubTypes.Type(value = IssueCredentialRequest.IssueLDCredentialRequest.class,
                names = { "JSON_LD", "JSON-LD", "json_ld", "json-ld" }),
})
@Schema(anyOf = {
        IssueCredentialRequest.IssueIndyCredentialRequest.class,
        IssueCredentialRequest.IssueLDCredentialRequest.class,
})
public abstract class IssueCredentialRequest {

    @NotBlank
    private UUID partnerId;

    /** credential exchange type */
    private CredentialType type;

    /** credential body key value pairs */
    @JsonRawValue
    @Schema(example = "{}")
    private JsonNode document;

    @Introspected
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static final class IssueIndyCredentialRequest extends IssueCredentialRequest {

        @NotBlank
        private UUID credDefId;

        /** credential exchange api version */
        private ExchangeVersion exchangeVersion;

        public boolean exchangeIsV1() {
            return exchangeVersion == null || ExchangeVersion.V1.equals(exchangeVersion);
        }
    }

    @Introspected
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static final class IssueLDCredentialRequest extends IssueCredentialRequest {
        @NotBlank
        private UUID schemaId;
    }

    public boolean typeIsIndy() {
        return type == null || CredentialType.INDY.equals(type);
    }
}
