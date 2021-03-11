package org.hyperledger.bpa.controller.api.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.model.BPARestrictions;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestrictionResponse {
    private UUID id;
    private String label;
    private String issuerDid;

    public static RestrictionResponse from(BPARestrictions db) {
        return RestrictionResponse
                .builder()
                .id(db.getId())
                .label(db.getLabel())
                .issuerDid(db.getIssuerDid())
                .build();
    }
}
