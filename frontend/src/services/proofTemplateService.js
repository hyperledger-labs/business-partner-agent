/*
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";

export default {
  getProofTemplates() {
    return appAxios().get(`${ApiRoutes.PROOF_TEMPLATES}`);
  },
  getProofTemplate(id) {
    return appAxios().get(`${ApiRoutes.PROOF_TEMPLATES}/${id}`);
  },
  getKnownConditionOperators() {
    return appAxios().get(
      `${ApiRoutes.PROOF_TEMPLATES}/known-condition-operators`
    );
  },
  createProofTemplate(data) {
    return appAxios().post(`${ApiRoutes.PROOF_TEMPLATES}`, data);
  },
  deleteProofTemplate(id) {
    return appAxios().delete(`${ApiRoutes.PROOF_TEMPLATES}/${id}`);
  },
  sendProofTemplate(id, partnerId) {
    return appAxios().put(
      `${ApiRoutes.PROOF_TEMPLATES}/${id}/proof-request/${partnerId}`
    );
  },
};
