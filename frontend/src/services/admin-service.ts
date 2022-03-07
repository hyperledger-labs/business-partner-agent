/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";

export default {
  //
  // Admin API
  //
  listSchemas() {
    return appAxios().get(`${ApiRoutes.ADMIN}/schema`);
  },
  addSchema(data) {
    return appAxios().post(`${ApiRoutes.ADMIN}/schema`, data);
  },
  deleteSchema(id) {
    return appAxios().delete(`${ApiRoutes.ADMIN}/schema/${id}`);
  },
  listTags() {
    return appAxios().get(`${ApiRoutes.ADMIN}/tag`);
  },
  addTag(data) {
    return appAxios().post(`${ApiRoutes.ADMIN}/tag`, data);
  },
  deleteTag(id, hardDelete) {
    let parameters;
    if (hardDelete) {
      parameters = new URLSearchParams([["force", "true"]]);
    }
    return appAxios().delete(`${ApiRoutes.ADMIN}/tag/${id}`, {
      params: parameters,
    });
  },
};
