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

import jakarta.inject.Singleton;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueLDCredentialEvent;

@Singleton
public record LDEventHandler(HolderLDManager holder, IssuerLDManager issuer) {
    public void handleIssueCredentialV2LD(V2IssueLDCredentialEvent credentialInfo) {
    }
}
