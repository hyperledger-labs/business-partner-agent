/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import { UpdatePartnerRequest } from "@/services/partner-types";

export default {
  //
  // Partner API
  //

  listPartners() {
    return appAxios().get(`${ApiRoutes.PARTNERS}`);
  },
  updatePartner(id: string, data: UpdatePartnerRequest) {
    return appAxios().put(`${ApiRoutes.PARTNERS}/${id}`, data);
  },
  sendMessage(id: string, content: string): Promise<void> {
    return appAxios().post(`${ApiRoutes.PARTNERS}/${id}/messages`, {
      content: content,
    });
  },
  getMessages(id: string) {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/messages`);
  },
  getPresentationExRecords(id: string) {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/proof-exchanges`);
  },
};
