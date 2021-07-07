/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import Vue from "vue";
import Vuex from "vuex";
import taa from "./modules/taa";
import socketEvents from "./modules/socketevents";
import * as actions from "./actions";
import * as getters from "./getters";

Vue.use(Vuex);

const store = new Vuex.Store({
  state: {
    partners: [],
    editedDocument: {}, //document currently being edited
    documents: [],
    credentials: [],
    schemas: [],
    tags: [],
    busyStack: 0,
    expertMode: false,
    settings: {},
  },

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
  },

  modules: {
    taa,
    socketEvents,
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
