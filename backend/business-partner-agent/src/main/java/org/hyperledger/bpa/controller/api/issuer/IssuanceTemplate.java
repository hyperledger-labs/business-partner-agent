package org.hyperledger.bpa.controller.api.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuanceTemplate {
    private UUID id;
    private SchemaAPI schema;
    private String displayText;
    private CredentialType type;

    public static IssuanceTemplate from(BPACredentialDefinition db) {
        SchemaAPI schemaAPI = SchemaAPI.from(db.getSchema(), false, false);
        String displayText = String.format("%s (%s) - %s", schemaAPI.getLabel(), schemaAPI.getVersion(), db.getTag());
        return IssuanceTemplate
                .builder()
                .id(db.getId())
                .schema(schemaAPI)
                .displayText(displayText)
                .type(CredentialType.INDY)
                .build();
    }

    public static IssuanceTemplate from(BPASchema db) {
        // String displayText = String.format("%s (%s) - %s", schemaAPI.getLabel(),
        // schemaAPI.getVersion(), db.getTag());
        return IssuanceTemplate.builder().build();
    }
}
