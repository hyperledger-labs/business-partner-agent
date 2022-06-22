/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { CredDef } from "@/services";
import { IStateCredentialDefinitions } from "@/store/state-type";
import issuerService from "@/services/issuer-service";
import { EventBus } from "@/main";

const state: IStateCredentialDefinitions = {
  credDefSelectList: new Array<CredDef>(),
};

export default {
  state,
  getters: {
    getCredDefSelectList: (state: IStateCredentialDefinitions): CredDef[] => {
      return state.credDefSelectList;
    },
  },
  actions: {
    async loadCredDefSelectList(context: any) {
      issuerService
        .listCredDefs()
        .then((result) => {
          if (result.status === 200) {
            context.commit("setCredDefSelectList", result.data);
          }
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setCredDefSelectList: (
      state: IStateCredentialDefinitions,
      credDefs: CredDef[]
    ) => {
      state.credDefSelectList = credDefs;
    },
  },
};
