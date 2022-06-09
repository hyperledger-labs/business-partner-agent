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
  AddSchemaRequest,
  AddTagRequest,
  AddTrustedIssuerRequest,
  SchemaAPI,
  TagAPI,
  TrustedIssuer,
  UpdateTrustedIssuerRequest,
} from "@/services/types-services";

export default {
  //
  // Admin API
  //
  listSchemas(): Promise<AxiosResponse<SchemaAPI[]>> {
    return appAxios().get(`${ApiRoutes.ADMIN}/schema`);
  },

  addSchema(data: AddSchemaRequest): Promise<AxiosResponse<SchemaAPI>> {
    return appAxios().post(`${ApiRoutes.ADMIN}/schema`, data);
  },

  getSchema(id: string): Promise<AxiosResponse<SchemaAPI>> {
    return appAxios().get(`${ApiRoutes.ADMIN}/schema/${id}`);
  },

  removeSchema(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.ADMIN}/schema/${id}`);
  },

  deleteTrustedIssuer(
    id: string,
    trustedIssuerId: string
  ): Promise<AxiosResponse<void>> {
    return appAxios().delete(
      `${ApiRoutes.ADMIN}/schema/${id}/trustedIssuer/${trustedIssuerId}`
    );
  },

  addTrustedIssuer(
    id: string,
    data: AddTrustedIssuerRequest
  ): Promise<AxiosResponse<TrustedIssuer>> {
    return appAxios().post(
      `${ApiRoutes.ADMIN}/schema/${id}/trustedIssuer`,
      data
    );
  },

  updateTrustedIssuer(
    id: string,
    trustedIssuerId: string,
    data: UpdateTrustedIssuerRequest
  ): Promise<AxiosResponse<TrustedIssuer>> {
    return appAxios().put(
      `${ApiRoutes.ADMIN}/schema/${id}/trustedIssuer/${trustedIssuerId}`,
      data
    );
  },

  listTags(): Promise<AxiosResponse<TagAPI[]>> {
    return appAxios().get(`${ApiRoutes.ADMIN}/tag`);
  },

  addTag(data: AddTagRequest): Promise<AxiosResponse<TagAPI>> {
    return appAxios().post(`${ApiRoutes.ADMIN}/tag`, data);
  },

  deleteTag(id: string, hardDelete: boolean) {
    let parameters;
    if (hardDelete) {
      parameters = new URLSearchParams([["force", "true"]]);
    }
    return appAxios().delete(`${ApiRoutes.ADMIN}/tag/${id}`, {
      params: parameters,
    });
  },
};
