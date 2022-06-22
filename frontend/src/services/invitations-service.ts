/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { AxiosResponse } from "axios";
import { appAxios } from "@/services/interceptors";
import { ApiRoutes } from "@/constants";
import {
  AcceptInvitationRequest,
  APICreateInvitationResponse,
  CheckInvitationRequest,
  CreatePartnerInvitationRequest,
} from "@/services/types-services";

export default {
  requestConnectionInvitation(
    partner: CreatePartnerInvitationRequest
  ): Promise<AxiosResponse<APICreateInvitationResponse>> {
    return appAxios().post(`${ApiRoutes.INVITATIONS}`, partner);
  },
  acceptInvitation(
    invitation: AcceptInvitationRequest
  ): Promise<AxiosResponse<any>> {
    return appAxios().post(`${ApiRoutes.INVITATIONS}/accept`, invitation);
  },
  checkInvitation(
    invitation: CheckInvitationRequest
  ): Promise<AxiosResponse<any>> {
    return appAxios().post(`${ApiRoutes.INVITATIONS}/check`, invitation);
  },
};
