package org.hyperledger.bpa.controller.api.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemaRequest {
    // BPA fields
    private String schemaLabel;

    private String defaultAttributeName;

    // aries fields...
    private String schemaName;

    private String schemaVersion;

    private List<String> attributes;

}
