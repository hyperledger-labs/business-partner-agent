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
  //
  // Presentation/Proof Exchange API
  //

  declineProofRequest(id: string, reasonMessage: string) {
    const message =
      reasonMessage === undefined || "" ? undefined : reasonMessage;

    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/decline`, {
      message,
    });
  },
  approveProofRequest(id: string, referents: string[]) {
    return appAxios().post(`${ApiRoutes.PROOF_EXCHANGES}/${id}/prove`, {
      payload: {
        referents: referents,
      },
    });
  },
  getMatchingCredentials(id: string) {
    return appAxios().get(
      `${ApiRoutes.PROOF_EXCHANGES}/${id}/matching-credentials`
    );
  },
  getProofExRecord(id: string) {
    return appAxios().get(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },
  deleteProofExRecord(id: string) {
    return appAxios().delete(`${ApiRoutes.PROOF_EXCHANGES}/${id}`);
  },
};
