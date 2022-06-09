/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { AxiosResponse } from "axios";
import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import {
  AriesCredential,
  DeclineExchangeRequest,
  MyDocumentAPI,
  Page,
  PaginationCommand,
  WalletCredentialRequest,
  WalletDocumentRequest,
} from "@/services/types-services";

export default {
  getCredentialById(id: string): Promise<AxiosResponse<AriesCredential>> {
    return appAxios().get(`${ApiRoutes.WALLET}/credential/${id}`);
  },

  deleteCredential(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.WALLET}/credential/${id}`);
  },

  updateCredential(
    id: string,
    content: WalletCredentialRequest
  ): Promise<AxiosResponse<void>> {
    return appAxios().put(`${ApiRoutes.WALLET}/credential/${id}`, content);
  },

  getCredentials(
    pc?: PaginationCommand
  ): Promise<AxiosResponse<Page<AriesCredential[]>>> {
    let query = "";

    if (pc) {
      query =
        "?" +
        Object.entries(pc)
          .map(([key, value]) => `${key}=${value}`)
          .join("&");
    }
    return appAxios().get(`${ApiRoutes.WALLET}/credential${query}`);
  },

  toggleCredentialVisibility(id: string): Promise<AxiosResponse<void>> {
    return appAxios().get(
      `${ApiRoutes.WALLET}/credential/${id}/toggle-visibility`
    );
  },

  acceptCredentialOffer(id: string): Promise<AxiosResponse<void>> {
    return appAxios().put(`${ApiRoutes.WALLET}/credential/${id}/accept-offer`);
  },

  async declineCredentialOffer(
    id: string,
    reasonMessage: string
  ): Promise<AxiosResponse<void>> {
    const body: DeclineExchangeRequest = {
      message: reasonMessage === undefined || "" ? undefined : reasonMessage,
    };

    return appAxios().put(
      `${ApiRoutes.WALLET}/credential/${id}/decline-offer`,
      body
    );
  },

  getDocuments(
    pc?: PaginationCommand
  ): Promise<AxiosResponse<Page<MyDocumentAPI[]>>> {
    let query = "";

    if (pc) {
      query =
        "?" +
        Object.entries(pc)
          .map(([key, value]) => `${key}=${value}`)
          .join("&");
    }
    return appAxios().get(`${ApiRoutes.WALLET}/document${query}`);
  },

  addDocument(
    document: WalletDocumentRequest
  ): Promise<AxiosResponse<MyDocumentAPI>> {
    return appAxios().post(`${ApiRoutes.WALLET}/document`, document);
  },

  getDocumentById(documentId: string): Promise<AxiosResponse<MyDocumentAPI>> {
    return appAxios().get(`${ApiRoutes.WALLET}/document/${documentId}`);
  },

  updateDocument(
    documentId: string,
    document: WalletDocumentRequest
  ): Promise<AxiosResponse<MyDocumentAPI>> {
    return appAxios().put(
      `${ApiRoutes.WALLET}/document/${documentId}`,
      document
    );
  },

  deleteDocument(documentId: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.WALLET}/document/${documentId}`);
  },
};
