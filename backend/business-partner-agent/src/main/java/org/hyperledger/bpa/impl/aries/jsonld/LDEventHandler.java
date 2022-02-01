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
package org.hyperledger.bpa.impl.aries.jsonld;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;

@Singleton
public class LDEventHandler {

    @Inject
    HolderLDManager holder;

    @Inject
    IssuerLDManager issuer;

    public void dispatch(V20CredExRecord v2) {
        if (v2.roleIsHolder()) {
            if (v2.stateIsOfferReceived()) {
                holder.handleOfferReceived(v2, BPACredentialExchange.ExchangePayload.jsonLD(v2.resolveLDCredOffer()),
                        ExchangeVersion.V2);
            } else if (v2.stateIsCredentialReceived()) {
                holder.handleCredentialReceived(v2);
            } else {
                holder.handleStateChangesOnly(v2.getCredentialExchangeId(), v2.getState(), v2.getUpdatedAt(),
                        v2.getErrorMsg());
            }
        }
    }

    public void handleIssueCredentialV2LD(V2IssueLDCredentialEvent credentialInfo) {
        //
    }
}
