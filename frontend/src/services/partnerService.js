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
  sendMessage(id, content) {
    return appAxios().post(`${ApiRoutes.PARTNERS}/${id}/messages`, {
      content: content,
    });
  },
  getMessages(id) {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/messages`);
  },
  getPresentationExRecords(id) {
    return appAxios().get(`${ApiRoutes.PARTNERS}/${id}/proof-exchanges`);
  },
  declinePresentationRequest({ partnerId, proofId }) {
    return appAxios().post(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof-exchanges/${proofId}/decline`
    );
  },
  approvePresentationRequest({ partnerId, proofId }) {
    return appAxios().post(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof-exchanges/${proofId}/prove`
    );
  },
  getPresentationExRecord({ partnerId, proofId }) {
    return appAxios().post(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof/${proofId}`
    );
  },
  deletePresentationExRecord({ partnerId, proofId }) {
    return appAxios().delete(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof/${proofId}`
    );
  },
};
