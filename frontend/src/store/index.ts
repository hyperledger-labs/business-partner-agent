/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Vue from "vue";
import Vuex from "vuex";
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
  proofTemplates,
  credentials,
} from "@/store/modules";

Vue.use(Vuex);

const state: StateBpa = {
  editedDocument: {}, //document currently being edited
  busyStack: 0,
  expertMode: false,
};

const store = new Vuex.Store({
  state,
  getters: getters,
  mutations: {
    setExpertMode(state: StateBpa, payload) {
      state.expertMode = payload.isExpert;
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
    proofTemplates,
    credentials,
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
