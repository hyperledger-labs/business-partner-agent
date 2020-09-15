/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AriesStringUtilTest {

    @Test
    void testGetSeqNo() {
        assertEquals("571", AriesStringUtil.credDefIdGetSquenceNo("M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account"));
    }

    @Test
    void testGetSchemaName() {
        assertEquals("bank_account", AriesStringUtil.schemaGetName("M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0"));
    }

    @Test
    void testGetDidAcyPyFormat() {
        assertEquals("44XuhwdnXMqxbQw9tFMQAp", AriesStringUtil.didGetLastSegment("did:sov:iil:44XuhwdnXMqxbQw9tFMQAp"));
        assertEquals("44XuhwdnXMqxbQw9tFMQAp", AriesStringUtil.didGetLastSegment("44XuhwdnXMqxbQw9tFMQAp"));
    }

    @Test
    void testGetTag() {
        assertEquals("IATF Certificate", AriesStringUtil
                .credDefIdGetTag("nJvGcV7hBSLRSUvwGk2hT:3:CL:734:IATF Certificate"));
    }

}
