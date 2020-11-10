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
import * as actions from "./actions";

Vue.use(Vuex);

const store = new Vuex.Store({
  state: {
    partners: [],
    newPartners: [],
    editedDocument: {}, //document currently being edited
    documents: [],
    credentials: [],
    newCredentials: [],
    schemas: [],
    busyStack: 0,
    expertMode: false,
    socket: {
      isConnected: false,
      message: "",
      reconnectError: false,
    },
  },

  getters: {
    isBusy: (state) => {
      return state.busyStack > 0;
    },
    publicDocumentsAndCredentials: (state) => {
      var retval = state.credentials
        .concat(
          state.documents.filter((d) => d.type != CredentialTypes.PROFILE.name)
        )
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
  },

  actions: actions,

  mutations: {
    SOCKET_ONOPEN(state, event) {
      console.log(event);
      Vue.prototype.$socket = event.currentTarget;
      state.socket.isConnected = true;
    },
    SOCKET_ONCLOSE(state, event) {
      console.log(event);
      state.socket.isConnected = false;
    },
    SOCKET_ONERROR(state, event) {
      console.error(state, event);
    },
    // default handler called for all methods
    SOCKET_ONMESSAGE(state, message) {
      console.log(message);
      state.socket.message = message;
    },
    // mutations for reconnect methods
    SOCKET_RECONNECT(state, count) {
      console.info(state, count);
    },
    SOCKET_RECONNECT_ERROR(state) {
      state.socket.reconnectError = true;
    },
    newPartner(state, payload) {
      console.log(payload);
      state.newPartners.push(payload);
    },
    newCredential(state, payload) {
      console.log(payload);
      state.newCredentials.push(payload);
    },
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
    setSettings(state, payload) {
      state.expertMode = payload.isExpert;
    },
  },

  modules: {
    taa,
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
