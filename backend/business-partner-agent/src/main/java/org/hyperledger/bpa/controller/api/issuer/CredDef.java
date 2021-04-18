package org.hyperledger.bpa.controller.api.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.model.BPACredentialDefinition;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredDef {
    private UUID id;
    private String schemaId;
    private String credentialDefinitionId;
    private String tag;
    private Boolean isSupportRevocation;
    private Integer revocationRegistrySize;
    private Date createdAt;

    public static CredDef from(BPACredentialDefinition db) {
        return CredDef
                .builder()
                .id(db.getId())
                .schemaId(db.getSchema().getSchemaId())
                .credentialDefinitionId(db.getCredentialDefinitionId())
                .tag(db.getTag())
                .isSupportRevocation(db.getIsSupportRevocation())
                .revocationRegistrySize(db.getRevocationRegistrySize())
                .createdAt(Date.from(db.getCreatedAt()))
                .build();
    }
}
