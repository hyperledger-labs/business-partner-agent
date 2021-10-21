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
package org.hyperledger.bpa.controller.api.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public enum ActivityRole {
    @JsonProperty("connection_request_sender")
    CONNECTION_REQUEST_SENDER,
    @JsonProperty("connection_request_recipient")
    CONNECTION_REQUEST_RECIPIENT,
    @JsonProperty("credential_exchange_holder")
    CREDENTIAL_EXCHANGE_HOLDER,
    @JsonProperty("credential_exchange_issuer")
    CREDENTIAL_EXCHANGE_ISSUER,
    @JsonProperty("presentation_exchange_prover")
    PRESENTATION_EXCHANGE_PROVER,
    @JsonProperty("presentation_exchange_verifier")
    PRESENTATION_EXCHANGE_VERIFIER,
}
