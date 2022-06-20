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
  UpdateSchemaRequest,
  AddTagRequest,
  SchemaApi,
  TagApi,
} from "@/services/types-services";

export default {
  //
  // Admin API
  //
  listSchemas(): any /*Promise<AxiosResponse<SchemaApi[]>> TODO: actions.ts needs to be refactored*/ {
    return appAxios().get(`${ApiRoutes.ADMIN}/schema`);
  },

  addSchema(data: AddSchemaRequest): Promise<AxiosResponse<SchemaApi>> {
    return appAxios().post(`${ApiRoutes.ADMIN}/schema`, data);
  },

  updateSchema(
    id: string,
    data: UpdateSchemaRequest
  ): Promise<AxiosResponse<SchemaApi>> {
    return appAxios().put(`${ApiRoutes.ADMIN}/schema/${id}`, data);
  },

  deleteSchema(id: string): Promise<AxiosResponse<void>> {
    return appAxios().delete(`${ApiRoutes.ADMIN}/schema/${id}`);
  },

  listTags(): Promise<AxiosResponse<TagApi[]>> {
    return appAxios().get(`${ApiRoutes.ADMIN}/tag`);
  },

  addTag(data: AddTagRequest): Promise<AxiosResponse<TagApi>> {
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
