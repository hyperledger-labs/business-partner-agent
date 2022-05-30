/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import {
  CreateCredDefRequest,
  SchemaApi,
  CreateSchemaRequest,
  CredentialOfferRequest,
  IssueCredentialRequestIndy,
  IssueOobCredentialRequest,
  CredDef,
  ApiCreateInvitation,
  CredEx,
  Page,
  IssueCredentialRequestJsonLd,
} from "@/services/types-services";
import { AxiosResponse } from "axios";

export default {
  //
  // Issuer API
  //
  createSchema(data: CreateSchemaRequest): Promise<AxiosResponse<SchemaApi>> {
    return appAxios().post(`${ApiRoutes.ISSUER}/schema`, data);
  },

  createCredDef(data: CreateCredDefRequest): Promise<AxiosResponse<CredDef>> {
    return appAxios().post(`${ApiRoutes.ISSUER}/creddef`, data);
  },

  deleteCredDef(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.ISSUER}/creddef/${id}`);
  },

  listCredDefs(): Promise<AxiosResponse<CredDef[]>> {
    return appAxios().get(`${ApiRoutes.ISSUER}/creddef`);
  },

  issueCredentialSendIndy(
    data: IssueCredentialRequestIndy
  ): Promise<AxiosResponse<string>> {
    return appAxios().post(`${ApiRoutes.ISSUER}/issue-credential/send`, data);
  },

  issueCredentialSendJsonLd(
    data: IssueCredentialRequestJsonLd
  ): Promise<AxiosResponse<string>> {
    return appAxios().post(`${ApiRoutes.ISSUER}/issue-credential/send`, data);
  },

  /**
   * Issue OOB credential - prepares credential offer and returns URL for use within the barcode
   * @param data
   */
  issueOobCredentialOfferCreate(
    data: IssueOobCredentialRequest
  ): Promise<AxiosResponse<ApiCreateInvitation>> {
    return appAxios().post(
      `${ApiRoutes.ISSUER}/issue-credential/oob-attachment`,
      data
    );
  },

  getCredExRecord(id: string): Promise<AxiosResponse<CredEx>> {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges/${id}`);
  },

  listCredentialExchanges(
    id?: string,
    params: URLSearchParams = new URLSearchParams()
  ): Promise<AxiosResponse<Page<CredEx[]>>> {
    if (id) {
      params.set("partnerId", id);
    }
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: params,
    });
  },

  revokeCredential(id: string): Promise<AxiosResponse<CredEx>> {
    return appAxios().put(`${ApiRoutes.ISSUER}/exchanges/${id}/revoke`);
  },

  async declineCredentialProposal(
    id: string,
    reasonMessage: string
  ): Promise<AxiosResponse<void>> {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/decline-proposal`,
      {
        message,
      }
    );
  },

  sendCredentialOffer(
    id: string,
    counterOfferData: CredentialOfferRequest
  ): Promise<AxiosResponse<CredEx>> {
    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/send-offer`,
      counterOfferData
    );
  },

  reIssueCredential(id: string): Promise<AxiosResponse<void>> {
    return appAxios().post(`${ApiRoutes.ISSUER}/exchanges/${id}/re-issue`);
  },
};
