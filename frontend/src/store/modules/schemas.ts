/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateSchemas } from "@/store/state-type";
import { CredentialTypes } from "@/constants";
import adminService from "@/services/admin-service";
import { EventBus } from "@/main";
import { CredentialType, SchemaAPI } from "@/services";
import i18n from "@/plugins/i18n";

const state: IStateSchemas = {
  schemaList: new Array<SchemaAPI>(),
};

export default {
  state,
  getters: {
    getSchemas: (state: IStateSchemas): SchemaAPI[] => {
      return state.schemaList;
    },
    getSchemaBasedSchemas: (state: IStateSchemas) => {
      return state.schemaList.filter((schema) => {
        return (
          schema.type === CredentialTypes.INDY.type ||
          schema.type === CredentialTypes.JSON_LD.type
        );
      });
    },
    getSchemaById: (state: IStateSchemas) => (schemaId: string) => {
      if (!schemaId) {
        return;
      }
      return state.schemaList.find((schema) => {
        return schema.schemaId === schemaId;
      });
    },
    // schemaType is CredentialType
    getSchemaByType: (state: IStateSchemas) => (schemaType: string) => {
      if (!schemaType) {
        return;
      }
      return state.schemaList.find((schema) => {
        return schema.type === schemaType;
      });
    },
  },
  actions: {
    async loadSchemas(context: any) {
      adminService
        .listSchemas()
        .then((result) => {
          if (result.status === 200) {
            const schemas = result.data;

            // Create empty organizational profile - necessary for initial setup
            schemas.unshift({
              type: CredentialTypes.PROFILE.type as CredentialType,
              label: i18n
                .t("component.profile.organizationalProfile.title")
                .toString(),
            });

            context.commit("setSchemas", result.data);
          }
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setSchemas: (state: IStateSchemas, schemas: SchemaAPI[]) => {
      state.schemaList = schemas;
    },
  },
};
