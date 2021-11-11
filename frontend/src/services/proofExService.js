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
  // Presentation/Proof Exchange API
  //

  declineProofRequest(id, reasonMessage = undefined) {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/decline`, {
      message,
    });
  },
  approveProofRequest(id, payload) {
    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/prove`, payload);
  },
  getMatchingCredentials(id) {
    return appAxios().get(
      `${ApiRoutes.PROOF_EXCHANGES}/${id}/matching-credentials`
    );
  },
  getProofExRecord(id) {
    return appAxios().get(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },
  deleteProofExRecord(id) {
    return appAxios().delete(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },
};
