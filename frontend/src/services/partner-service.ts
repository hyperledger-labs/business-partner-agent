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
  AddPartnerRequest,
  AriesProofExchange,
  ChatMessage,
  Page,
  PartnerAPI,
  SendMessageRequest,
  SendProofRequest,
  UpdatePartnerRequest,
} from "@/services/types-services";

export default {
  //
  // Partner API
  //

  addPartner(partner: AddPartnerRequest): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().post(`${ApiRoutes.PARTNERS}`, partner);
  },

  // Query used only for partners that can issue credentials of specified schema
  _getAll(
    params: URLSearchParams,
    invitation?: boolean,
    schemaId?: string
  ): Promise<AxiosResponse<Page<PartnerAPI[]>>> {
    params.set("showInvitations", invitation.toString());
    if (schemaId && schemaId.length > 0) {
      params.set("schemaId", schemaId);
    }
    return appAxios().get(`${ApiRoutes.PARTNERS}`, {
      params: params,
    });
  },

  getAll(params: URLSearchParams = new URLSearchParams()) {
    return this._getAll(params, true, "");
  },

  getAllForSchemaId(
    schemaId: string,
    params: URLSearchParams = new URLSearchParams()
  ) {
    return this._getAll(params, false, schemaId);
  },

  getAllWithoutInvites(params: URLSearchParams = new URLSearchParams()) {
    return this._getAll(params, false, "");
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
    return appAxios().post(`${ApiRoutes.PARTNERS}/${id}/proof-send`, content);
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
    id: string,
    params: URLSearchParams = new URLSearchParams()
  ): Promise<AxiosResponse<Page<AriesProofExchange[]>>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/proof-exchanges`, {
      params: params,
    });
  },

  lookupPartner(did: string): Promise<AxiosResponse<PartnerAPI>> {
    return appAxios().get(`${ApiRoutes.PARTNERS}/lookup/${did}`);
  },
};
