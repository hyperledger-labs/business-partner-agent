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
package org.hyperledger.oa.api;

public class ApiConstants {

    public static final String DID_METHOD_WEB = "did:web:";

    public static final String DEFAULT_KEY_ID = "#key-1";

    public static final String DEFAULT_VERIFICATION_KEY_TYPE = "Ed25519VerificationKey2018";

    public static final String DEFAULT_SIGNATURE = "Ed25519Signature2018";

    public static final String CREDENTIALS_V1 = "https://www.w3.org/2018/credentials/v1";

    // Default fallback id's from the demo ledger
    public static final Integer BANK_ACCOUNT_SCHEMA_SEQ = Integer.valueOf("571");
    public static final String BANK_ACCOUNT_SCHEMA_ID = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";

    public static final String INDY_CREDENTIAL_SCHEMA = "https://raw.githubusercontent.com/iil-network/contexts/master/indycredential.jsonld";

}
