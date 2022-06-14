/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Vue from "vue";
import Vuex from "vuex";
import * as actions from "./actions";
import * as getters from "./getters";
import { StateBpa } from "@/store/state-type";
import {
  messages,
  notifications,
  socketEvents,
  taa,
  credDefSelectList,
  schemas,
  settings,
  partners,
  partnerSelectList,
} from "@/store/modules";

Vue.use(Vuex);

const state: StateBpa = {
  editedDocument: {}, //document currently being edited
  documents: [],
  credentials: [],
  proofTemplates: [],
  tags: [],
  busyStack: 0,
  expertMode: false,
};

const store = new Vuex.Store({
  state,
  getters: getters,
  actions: actions,
  mutations: {
    loadDocumentsFinished(state: StateBpa, payload) {
      state.documents = payload.documents;
    },
    loadCredentialsFinished(state: StateBpa, payload) {
      state.credentials = payload.credentials;
    },
    setTags(state: StateBpa, payload) {
      console.log(payload.tags);
      state.tags = payload.tags;
    },
    setExpertMode(state: StateBpa, payload) {
      state.expertMode = payload.isExpert;
    },
    setProofTemplates(state: StateBpa, payload) {
      state.proofTemplates = payload.proofTemplates;
    },
  },

  modules: {
    taa,
    socketEvents,
    messages,
    notifications,
    credDefSelectList,
    schemas,
    settings,
    partners,
    partnerSelectList,
  },
});

store.subscribeAction({
  before: (action, state) => {
    state.busyStack = state.busyStack + 1;
  },
  after: (action, state) => {
    state.busyStack = state.busyStack - 1;
  },
});

export default store;
