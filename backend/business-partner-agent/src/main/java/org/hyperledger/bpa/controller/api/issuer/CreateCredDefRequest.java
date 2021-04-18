package org.hyperledger.bpa.controller.api.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCredDefRequest {

    // aries cred def fields
    private String schemaId;

    private String tag;

    private int revocationRegistrySize;

    private boolean supportRevocation;

}
