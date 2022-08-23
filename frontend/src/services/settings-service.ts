/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { AxiosResponse } from "axios";
import { RuntimeConfig } from "@/services/types-services";
import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";

export default {
  async getSettingsRuntimeConfig(): Promise<AxiosResponse<RuntimeConfig>> {
    return appAxios().get(`${ApiRoutes.ADMIN}/config`);
  },
};
