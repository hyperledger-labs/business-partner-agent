/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Vue from "vue";
import Vuex from "vuex";
import taa from "./modules/taa";
import socketEvents from "./modules/socketevents";
import * as actions from "./actions";
import * as getters from "./getters";
import messages from "@/store/modules/messages";
import notifications from "@/store/modules/notifications";
import { StateBpa } from "@/store/state-type";

Vue.use(Vuex);

const state: StateBpa = {
  partners: [],
  editedDocument: {}, //document currently being edited
  documents: [],
  credentials: [],
  schemas: [],
  proofTemplates: [],
  tags: [],
  partnerSelectList: [],
  credDefSelectList: [],
  busyStack: 0,
  expertMode: false,
  settings: {},
};

const store = new Vuex.Store({
  state,

  getters: getters,
  actions: actions,

  mutations: {
    loadDocumentsFinished(state, payload) {
      state.documents = payload.documents;
    },
    loadCredentialsFinished(state, payload) {
      state.credentials = payload.credentials;
    },
    loadPartnersFinished(state, payload) {
      state.partners = payload.partners;
    },
    setSchemas(state, payload) {
      state.schemas = payload.schemas;
    },
    setTags(state, payload) {
      console.log(payload.tags);
      state.tags = payload.tags;
    },
    setExpertMode(state, payload) {
      state.expertMode = payload.isExpert;
    },
    setSettings(state, payload) {
      state.settings = payload.settings;
    },
    setProofTemplates(state, payload) {
      state.proofTemplates = payload.proofTemplates;
    },
    setPartnerSelectList(state, payload) {
      state.partnerSelectList = payload.list;
    },
    setCredDefSelectList(state, payload) {
      state.credDefSelectList = payload.list;
    },
  },

  modules: {
    taa,
    socketEvents,
    messages,
    notifications,
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
