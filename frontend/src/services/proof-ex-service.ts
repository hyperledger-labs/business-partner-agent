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
  ApproveProofRequest,
  AriesProofExchange,
  DeclineExchangeRequest,
  PresentationRequestCredentials,
  PresentationRequestCredentialsDif,
} from "@/services/types-services";

export default {
  //
  // Presentation/Proof Exchange API
  //

  declineProofRequest(
    id: string,
    reasonMessage: string
  ): Promise<AxiosResponse<void>> {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    const body: DeclineExchangeRequest = {
      message,
    };

    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/decline`, body);
  },

  approveProofRequest(
    id: string,
    referents: string[]
  ): Promise<AxiosResponse<void>> {
    const body: ApproveProofRequest = {
      referents: referents,
    };

    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/prove`, body);
  },

  getMatchingCredentials(
    id: string
  ): Promise<AxiosResponse<PresentationRequestCredentials[]>> {
    return appAxios().get(
      `${ApiRoutes.PROOF_EXCHANGES}/${id}/matching-credentials`
    );
  },

  getMatchingDifCredentials(
    id: string
  ): Promise<AxiosResponse<PresentationRequestCredentialsDif>> {
    return appAxios().get(
      `${ApiRoutes.PROOF_EXCHANGES}/${id}/matching-credentials-ld`
    );
  },

  getProofExRecord(id: string): Promise<AxiosResponse<AriesProofExchange>> {
    return appAxios().get(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },

  deleteProofExRecord(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },
};
