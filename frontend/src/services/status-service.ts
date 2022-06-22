/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { AxiosResponse } from "axios";
import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import { BPAStats } from "@/services/types-services";

export default {
  async getStatus(): Promise<AxiosResponse<BPAStats>> {
    return appAxios().get(`${ApiRoutes.STATUS}`);
  },
};
