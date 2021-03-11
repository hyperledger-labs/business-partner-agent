package org.hyperledger.bpa.controller.api.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRestrictionRequest {
    private String label;
    private String issuerDid;
}
