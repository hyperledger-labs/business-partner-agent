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
  SendProofRequest,
  UpdatePartnerRequest,
} from "@/services/types-services";

export default {
  //
  // Partner API
  //

  // Query used only for partners that can issue credentials of specified schema
  getPartners(schemaId?: string): Promise<AxiosResponse<PartnerAPI[]>> {
    let query = "";

    if (schemaId && schemaId.length > 0) {
      query = `?schemaId=${schemaId}`;
    }

    return appAxios().get(`${ApiRoutes.PARTNERS}${query}`);
  },

  getPartnerById(id: string): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}`);
  },

  removePartner(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.PARTNERS}/${id}`);
  },

  acceptPartnerRequest(id: string): Promise<AxiosResponse<void>> {
    return appAxios().put(`${ApiRoutes.PARTNERS}/${id}/accept`);
  },

  refreshPartner(id: string): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/refresh`);
  },

  updatePartner(
    id: string,
    data: UpdatePartnerRequest
  ): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().put(`${ApiRoutes.PARTNERS}/${id}`, data);
  },

  sendProof(
    id: string,
    content: SendProofRequest
  ): Promise<AxiosResponse<void>> {
    return appAxios().post(`${ApiRoutes.PARTNERS}/${id}/proof-send`, {
      content: content,
    });
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
