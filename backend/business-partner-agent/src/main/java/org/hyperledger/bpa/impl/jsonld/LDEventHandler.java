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
package org.hyperledger.bpa.impl.jsonld;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;

@Singleton
public class LDEventHandler {

    @Inject
    HolderLDManager holder;

    @Inject
    IssuerLDManager issuer;

    public void dispatch(V20CredExRecord v20CredExRecord) {
        if (v20CredExRecord.roleIsHolder()) {
            if (v20CredExRecord.stateIsOfferReceived()) {
                holder.handleOfferReceived(v20CredExRecord);
            } else {
                holder.handleStateChanges(v20CredExRecord);
            }
        }
    }
}
