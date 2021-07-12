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
import com.google.gson.annotations.SerializedName;

public enum ActivityRole {
    @JsonProperty("connection_invitation_sender")
    @SerializedName("connection_invitation_sender")
    CONNECTION_INVITATION_SENDER,
    @JsonProperty("connection_invitation_recipient")
    @SerializedName("connection_invitation_recipient")
    CONNECTION_INVITATION_RECIPIENT,
    @JsonProperty("presentation_exchange_prover")
    @SerializedName("presentation_exchange_prover")
    PRESENTATION_EXCHANGE_PROVER,
    @JsonProperty("presentation_exchange_verifier")
    @SerializedName("presentation_exchange_verifier")
    PRESENTATION_EXCHANGE_VERIFIER;

    ActivityRole() {
    }
}
