/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { appAxios } from "@/services/interceptors";
import { ApiRoutes, CredentialExchangeRoles } from "@/constants";
import {
  CreateCredDefRequest,
  CreateSchemaRequest,
  CredentialOfferRequest,
  IssueCredentialRequest,
  IssueOobCredentialRequest,
} from "@/services/types-services";

export default {
  //
  // Issuer API
  //

  listSchemas() {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema`);
  },

  createSchema(data: CreateSchemaRequest) {
    return appAxios().post(`${ApiRoutes.ISSUER}/schema`, data);
  },

  readSchema(id: string) {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema/${id}`);
  },

  createCredDef(data: CreateCredDefRequest) {
    return appAxios().post(`${ApiRoutes.ISSUER}/creddef`, data);
  },

  deleteCredDef(id: string) {
    return appAxios().delete(`${ApiRoutes.ISSUER}/creddef/${id}`);
  },

  listCredDefs() {
    return appAxios().get(`${ApiRoutes.ISSUER}/creddef`);
  },

  issueCredentialSend(data: IssueCredentialRequest) {
    return appAxios().post(`${ApiRoutes.ISSUER}/issue-credential/send`, data);
  },

  /**
   * Issue OOB credential step 1 - prepares credential offer and returns URL for use within the barcode
   * @param data
   */
  issueOobCredentialOfferCreate(data: IssueOobCredentialRequest) {
    return appAxios().post(
      `${ApiRoutes.ISSUER}/issue-credential/oob-attachment`,
      data
    );
  },

  /**
   * Issue OOB credential step 2 - redirect with encoded offer
   * @param id UUID
   */
  issueOobCredentialOfferRedirect(id: string) {
    return appAxios().get(
      `${ApiRoutes.ISSUER}/issue-credential/oob-attachment/${id}`
    );
  },

  listCredentialExchanges(id: string) {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { partnerId: id },
    });
  },

  getCredExRecord(id: string) {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges/${id}`);
  },

  listCredentialExchangesAsIssuer(id?: string) {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { role: CredentialExchangeRoles.ISSUER, partnerId: id },
    });
  },

  listCredentialExchangesAsHolder() {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { role: CredentialExchangeRoles.HOLDER },
    });
  },
  revokeCredential(id: string) {
    return appAxios().put(`${ApiRoutes.ISSUER}/exchanges/${id}/revoke`);
  },
  acceptCredentialOffer(id: string) {
    return appAxios().put(`${ApiRoutes.WALLET}/credential/${id}/accept-offer`);
  },
  async declineCredentialOffer(id: string, reasonMessage: string) {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    return appAxios().put(
      `${ApiRoutes.WALLET}/credential/${id}/decline-offer`,
      {
        message,
      }
    );
  },
  async declineCredentialProposal(id: string, reasonMessage: string) {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/decline-proposal`,
      {
        message,
      }
    );
  },
  sendCredentialOffer(id: string, counterOfferData: CredentialOfferRequest) {
    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/send-offer`,
      counterOfferData
    );
  },
  reIssueCredential(id: string) {
    return appAxios().post(`${ApiRoutes.ISSUER}/exchanges/${id}/re-issue`);
  },
};
