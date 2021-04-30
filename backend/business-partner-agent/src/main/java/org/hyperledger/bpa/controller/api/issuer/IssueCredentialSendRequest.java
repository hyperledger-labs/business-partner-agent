package org.hyperledger.bpa.controller.api.issuer;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueCredentialSendRequest {
    // bpa ids
    private String credDefId;
    private String partnerId;

    @JsonRawValue
    @Schema(example = "{}")
    private JsonNode document;
}
