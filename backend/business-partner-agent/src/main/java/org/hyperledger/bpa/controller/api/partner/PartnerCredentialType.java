/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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
package org.hyperledger.bpa.controller.api.partner;

import org.hyperledger.bpa.impl.util.AriesStringUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCredentialType {
    /** The credential definition id */
    private String credentialDefinitionId;
    /** The credential definitions tag */
    private String type;
    /** The schemas TRX id */
    private String seqno;

    public static PartnerCredentialType fromCredDefId(String credDefId) {
        return new PartnerCredentialType(
                credDefId,
                AriesStringUtil.getLastSegment(credDefId),
                AriesStringUtil.credDefIdGetSquenceNo(credDefId));
    }
}
