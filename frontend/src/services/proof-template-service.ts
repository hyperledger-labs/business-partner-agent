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
  CredentialType,
  PresentationRequestVersion,
  ProofTemplate,
} from "@/services/types-services";

export default {
  getProofTemplates(): Promise<AxiosResponse<ProofTemplate[]>> {
    return appAxios().get(`${ApiRoutes.PROOF_TEMPLATES}`);
  },

  getProofTemplate(id: string): Promise<AxiosResponse<ProofTemplate>> {
    return appAxios().get(`${ApiRoutes.PROOF_TEMPLATES}/${id}`);
  },

  getKnownConditionOperators(
    type: CredentialType
  ): Promise<AxiosResponse<string[]>> {
    const params = new URLSearchParams([["type", type]]);
    return appAxios().get(
      `${ApiRoutes.PROOF_TEMPLATES}/known-condition-operators`,
      { params }
    );
  },

  createProofTemplate(
    data: ProofTemplate
  ): Promise<AxiosResponse<ProofTemplate>> {
    return appAxios().post(`${ApiRoutes.PROOF_TEMPLATES}`, data);
  },

  deleteProofTemplate(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.PROOF_TEMPLATES}/${id}`);
  },

  sendProofTemplate(
    templateId: string,
    partnerId: string,
    data: PresentationRequestVersion
  ): Promise<AxiosResponse<void>> {
    return appAxios().put(
      `${ApiRoutes.PARTNERS}/${partnerId}/proof-request/${templateId}`,
      data
    );
  },
};
