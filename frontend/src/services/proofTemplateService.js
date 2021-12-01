/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
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
  sendProofTemplate(id, partnerId, data) {
    return appAxios().put(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof-request/${id}`,
      data
    );
  },
};
