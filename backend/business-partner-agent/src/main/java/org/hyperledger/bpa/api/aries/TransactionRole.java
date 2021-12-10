/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/acapy-java-client
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.bpa.api.aries;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public enum TransactionRole {
    @JsonProperty("Author")
    @SerializedName("Author")
    AUTHOR,

    @JsonProperty("Endorser")
    @SerializedName("Endorser")
    ENDORSER,
}
