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
  sendCredentialRequest(partnerId, data) {
    return appAxios().post(
      `${ApiRoutes.PARTNERS}/${partnerId}/credential-request`,
      data
    );
  },
};
