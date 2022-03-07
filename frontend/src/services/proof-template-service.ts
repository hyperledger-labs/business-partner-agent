/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
  getProofTemplate(id: string) {
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
  deleteProofTemplate(id: string) {
    return appAxios().delete(`${ApiRoutes.PROOF_TEMPLATES}/${id}`);
  },
  sendProofTemplate(id: string, partnerId: string, data) {
    return appAxios().put(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof-request/${id}`,
      data
    );
  },
};
