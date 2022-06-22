/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import axios, { AxiosResponse } from "axios";
import { DIDDocument } from "@/services/types-services";

export default {
  getWellKnownDid(): Promise<AxiosResponse<DIDDocument>> {
    return axios.get(
      `${this.$apiBaseUrl.replace("/api", "")}/.well-known/did.json`
    );
  },
};
