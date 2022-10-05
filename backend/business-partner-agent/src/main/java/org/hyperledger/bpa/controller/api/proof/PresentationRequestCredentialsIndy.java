/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.controller.api.proof;

import lombok.*;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationRequestCredentialsIndy {

    private CredentialInfo credentialInfo;

    private org.hyperledger.aries.api.present_proof.PresentationRequestCredentials.Interval interval;

    private List<String> presentationReferents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class CredentialInfo {

        /**
         * internal credentialId {@link BPACredentialExchange} - matched via referent
         */
        private UUID credentialId;
        private String credentialLabel;

        private String referent;
        private Boolean revoked;

        private List<CredentialAttributes> attrs;

        private String schemaId;
        private String schemaLabel;

        private String credentialDefinitionId;
        private String issuerLabel;

        static CredentialInfo mergeInfo(
                @NonNull org.hyperledger.aries.api.present_proof.PresentationRequestCredentials.CredentialInfo aca,
                @NonNull AriesCredential.BPACredentialInfo bpa) {
            return CredentialInfo
                    .builder()
                    .credentialId(bpa.getCredentialId())
                    .referent(aca.getReferent())
                    .revoked(bpa.getRevoked())
                    .attrs(aca.getAttrs())
                    .schemaId(aca.getSchemaId())
                    .schemaLabel(bpa.getSchemaLabel())
                    .credentialDefinitionId(aca.getCredentialDefinitionId())
                    .issuerLabel(bpa.getIssuerLabel())
                    .credentialLabel(bpa.getCredentialLabel())
                    .build();
        }
    }

    public static PresentationRequestCredentialsIndy from(
            @NonNull org.hyperledger.aries.api.present_proof.PresentationRequestCredentials aca,
            @NonNull AriesCredential.BPACredentialInfo bpa) {
        return PresentationRequestCredentialsIndy
                .builder()
                .credentialInfo(CredentialInfo.mergeInfo(aca.getCredentialInfo(), bpa))
                .interval(aca.getInterval())
                .presentationReferents(aca.getPresentationReferents())
                .build();
    }
}
