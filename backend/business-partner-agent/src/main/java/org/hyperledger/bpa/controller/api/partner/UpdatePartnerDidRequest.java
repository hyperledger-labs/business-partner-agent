package org.hyperledger.bpa.controller.api.partner;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdatePartnerDidRequest {
    private String did;
}
