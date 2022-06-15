/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  AriesCredential,
  MyDocumentAPI,
  Page,
  walletService,
} from "@/services";
import { apiBaseUrl, axios, EventBus } from "@/main";
import { AxiosResponse } from "axios";
import { IStateCredentialsAndDocuments } from "@/store/state-type";
import { CredentialTypes } from "@/constants";

const state: IStateCredentialsAndDocuments = {
  credentials: new Array<AriesCredential>(),
  documents: new Array<MyDocumentAPI>(),
};

export default {
  state,
  getters: {
    getCredentials: (state: IStateCredentialsAndDocuments) => {
      return state.credentials;
    },
    publicDocumentsAndCredentials: (state: IStateCredentialsAndDocuments) => {
      return [...state.credentials, ...state.documents].filter(
        (d) => d.isPublic === true
      );
    },
    getOrganizationalProfile: (state: IStateCredentialsAndDocuments) => {
      const documents = state.documents.filter(
        (d) => d.type === CredentialTypes.PROFILE.type
      );
      return documents.length === 1 ? documents[0] : undefined;
    },
  },
  actions: {
    async loadCredentials(context: any) {
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
          context.commit("loadCredentialsFinished", credentials);
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
    async loadDocuments(context: any) {
      walletService
        .getDocuments()
        .then((result) => {
          context.commit("loadDocumentsFinished", result.data.content);
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    loadCredentialsFinished: (
      state: IStateCredentialsAndDocuments,
      credentials: AriesCredential[]
    ) => {
      state.credentials = credentials;
    },
    loadDocumentsFinished(
      state: IStateCredentialsAndDocuments,
      documents: MyDocumentAPI[]
    ) {
      state.documents = documents;
    },
  },
};
