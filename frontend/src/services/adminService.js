/*
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
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
};
