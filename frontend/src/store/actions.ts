/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { EventBus, axios, apiBaseUrl } from "../main";
import adminService from "@/services/admin-service";
import { AriesCredential, Page, walletService } from "@/services";
import { AxiosResponse } from "axios";

export const loadTags = async ({ commit }) => {
  adminService
    .listTags()
    .then((result) => {
      const tags = result.data;
      console.log(tags);
      commit({
        type: "setTags",
        tags: tags,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadDocuments = async ({ commit }) => {
  walletService
    .getDocuments()
    .then((result) => {
      commit({
        type: "loadDocumentsFinished",
        documents: result.data.content,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};

export const loadCredentials = async ({ commit }) => {
  axios
    .get(`${apiBaseUrl}/wallet/credential`)
    .then((result: AxiosResponse<Page<AriesCredential[]>>) => {
      const credentials: AriesCredential[] = [];
      for (const credentialReference of result.data.content) {
        axios
          .get(`${apiBaseUrl}/wallet/credential/${credentialReference.id}`)
          .then((result: AxiosResponse<AriesCredential>) => {
            credentials.push(result.data);
          })
          .catch((error) => {
            console.error(error);
            EventBus.$emit("error", error);
          });
      }
      commit({
        type: "loadCredentialsFinished",
        credentials: credentials,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};
