/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import { AxiosResponse } from "axios";
import { ActivityItem, Page } from "@/services/types-services";

export default {
  //
  // Activities API
  //

  listActivities(
    tasks: string,
    activities: string,
    filter: { name: string; value: string },
    params: URLSearchParams = new URLSearchParams()
  ): Promise<AxiosResponse<Page<ActivityItem[]>>> {
    let qs = `task=${tasks}&activity=${activities}`;
    if (filter && filter.name && filter.value) {
      qs = `${qs}&${filter.name}=${filter.value}`;
    }
    return appAxios().get(`${ApiRoutes.ACTIVITIES}?${qs}`, {
      params: params,
    });
  },
};
