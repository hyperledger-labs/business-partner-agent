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
  // Partner API
  //

  listPartners() {
    return appAxios().get(`${ApiRoutes.PARTNERS}`);
  },
  updatePartner({ id, data }) {
    return appAxios().put(`${ApiRoutes.PARTNERS}/${id}`, data);
  },
};
