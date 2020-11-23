/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import Vue from "vue";
import Vuex from "vuex";
import { CredentialTypes } from "../constants";
import taa from "./modules/taa";
import socketEvents from "./modules/socketevents";
import * as actions from "./actions";

Vue.use(Vuex);

const store = new Vuex.Store({
  state: {
    partners: [],
    editedDocument: {}, //document currently being edited
    documents: [],
    credentials: [],
    schemas: [],
    busyStack: 0,
    expertMode: false,
    settings: {},
  },

  getters: {
    isBusy: (state) => {
      return state.busyStack > 0;
    },
    publicDocumentsAndCredentials: (state) => {
      var retval = state.credentials
        .concat(state.documents)
        .filter((d) => d.isPublic == true);
      return retval;
    },
    organizationalProfile: (state) => {
      var documents = state.documents.filter(
        (d) => d.type == CredentialTypes.PROFILE.name
      );
      if (documents.length == 1) return documents[0];
      else return undefined;
    },
    getPartnerByDID: (state) => (did) => {
      return state.partners.find((partner) => {
        partner.did === did;
      });
    },
    getSettingByKey: (state) => (key) => {
      if (state.settings && {}.hasOwnProperty.call(state.settings, key)) {
        return state.settings[key];
      }
    },
    getSettings: (state) => {
      return state.settings;
    },
  },

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
    loadSchemasFinished(state, payload) {
      state.schemas = payload.schemas;
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
