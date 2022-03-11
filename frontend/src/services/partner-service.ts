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
import {
  AriesProofExchange,
  ChatMessage,
  PartnerAPI,
  SendMessageRequest,
  UpdatePartnerRequest,
} from "@/services/types-services";

export default {
  //
  // Partner API
  //

  listPartners(): Promise<AxiosResponse<PartnerAPI[]>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}`);
  },

  updatePartner(
    id: string,
    data: UpdatePartnerRequest
  ): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().put(`${ApiRoutes.PARTNERS}/${id}`, data);
  },

  sendMessage(
    id: string,
    content: SendMessageRequest
  ): Promise<AxiosResponse<void>> {
    return appAxios().post(`${ApiRoutes.PARTNERS}/${id}/messages`, {
      content: content,
    });
  },

  getMessages(id: string): Promise<AxiosResponse<ChatMessage[]>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/messages`);
  },

  getPresentationExRecords(
    id: string
  ): Promise<AxiosResponse<AriesProofExchange[]>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/proof-exchanges`);
  },
};
