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
        return partner.did === did;
      });
    },
    schemas: (state) => {
      return state.schemas;
    },
    getSchema: (state) => (schemaId) => {
      if (!schemaId) {
        return null;
      }
      return state.schemas.find((schema) => {
        return schema.schemaId === schemaId;
      });
    },
    getPreparedSchema: (state) => (schemaId) => {
      let schema = state.schemas.find((schema) => {
        return schema.schemaId === schemaId;
      });
      console.log(schema);
      return Object.assign(schema, {
        fields: schema.schemaAttributeNames.map((key) => {
          return {
            type: key,
            label: key,
          };
        }),
      });
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
    setSettings(state, payload) {
      state.expertMode = payload.isExpert;
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
