package org.hyperledger.bpa.controller.api.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.model.BPACredentialDefinition;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialDefinitionConfiguration {
    private UUID id;
    private String label;
    private String credentialDefinitionId;

    public static CredentialDefinitionConfiguration from(BPACredentialDefinition db) {
        return CredentialDefinitionConfiguration
                .builder()
                .id(db.getId())
                .label(db.getLabel())
                .credentialDefinitionId(db.getCredentialDefinitionId())
                .build();
    }
}
