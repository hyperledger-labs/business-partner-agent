/*
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import { appAxios } from "@/services/interceptors";
import { ApiRoutes, CredentialExchangeRoles } from "@/constants";

export default {
  //
  // Issuer API
  //

  listSchemas() {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema`);
  },

  createSchema(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/schema`, data);
  },

  readSchema(id) {
    return appAxios().get(`${ApiRoutes.ISSUER}/schema/${id}`);
  },

  createCredDef(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/creddef`, data);
  },

  deleteCredDef(id) {
    return appAxios().delete(`${ApiRoutes.ISSUER}/creddef/${id}`);
  },

  listCredDefs() {
    return appAxios().get(`${ApiRoutes.ISSUER}/creddef`);
  },

  issueCredentialSend(data) {
    return appAxios().post(`${ApiRoutes.ISSUER}/issue-credential/send`, data);
  },

  listCredentialExchanges(id) {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { partnerId: id },
    });
  },

  listCredentialExchangesAsIssuer(id) {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { role: CredentialExchangeRoles.ISSUER, partnerId: id },
    });
  },

  listCredentialExchangesAsHolder() {
    return appAxios().get(`${ApiRoutes.ISSUER}/exchanges`, {
      params: { role: CredentialExchangeRoles.HOLDER },
    });
  },
  revokeCredential(id) {
    return appAxios().put(`${ApiRoutes.ISSUER}/exchanges/${id}/revoke`);
  },
  acceptCredentialOffer(id) {
    return appAxios().put(`${ApiRoutes.WALLET}/credential/${id}/accept-offer`);
  },
  declineCredentialOffer(id) {
    return appAxios().put(`${ApiRoutes.WALLET}/credential/${id}/decline-offer`);
  },
  declineCredentialProposal(id) {
    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/decline-proposal`
    );
  },
  sendCredentialOffer(id, counterOfferData) {
    return appAxios().put(
      `${ApiRoutes.ISSUER}/exchanges/${id}/send-offer`,
      counterOfferData
    );
  },
};
