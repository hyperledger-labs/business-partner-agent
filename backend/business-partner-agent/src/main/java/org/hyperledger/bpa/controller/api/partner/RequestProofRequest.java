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
package org.hyperledger.bpa.controller.api.partner;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.util.AriesStringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@Introspected
public class RequestProofRequest {

    private UUID partnerId;

    @JsonAlias("request_by_schema")
    private RequestBySchema requestBySchema;

    @JsonRawValue
    @JsonAlias("request_raw")
    @Schema(example = "{}", description = "Any valid proof request")
    private JsonNode requestRaw;

    @Data
    @NoArgsConstructor
    public static final class RequestBySchema {
        @Schema(required = true)
        private String schemaId;
        @Nullable
        private List<String> issuerDid;
    }

    public boolean isRequestBySchema() {
        return requestBySchema != null;
    }

    @JsonIgnore
    public @Nullable String getFirstIssuerDid() {
        String issuerDid = null;
        if (isRequestBySchema() && CollectionUtils.isNotEmpty(requestBySchema.getIssuerDid())) {
            issuerDid = requestBySchema.getIssuerDid().get(0);
        }
        return issuerDid;
    }

    public List<PresentProofRequest.ProofRequest.ProofRestrictions> buildRestrictions() {
        List<PresentProofRequest.ProofRequest.ProofRestrictions> restrictions = new ArrayList<>();
        if (isRequestBySchema()) {
            if (CollectionUtils.isNotEmpty(requestBySchema.getIssuerDid())) {
                requestBySchema.getIssuerDid().forEach(iss -> {
                    PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder builder = PresentProofRequest.ProofRequest.ProofRestrictions
                            .builder();
                    builder.issuerDid(AriesStringUtil.getLastSegment(iss));
                    if (StringUtils.isNotEmpty(requestBySchema.getSchemaId())) {
                        builder.schemaId(requestBySchema.getSchemaId());
                    }
                    restrictions.add(builder.build());
                });
            } else {
                restrictions.add(PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                        .schemaId(requestBySchema.getSchemaId())
                        .build());
            }
        }
        return restrictions;
    }
}
