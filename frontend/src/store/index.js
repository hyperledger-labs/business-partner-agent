/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import Vue from 'vue'
import Vuex from 'vuex'
import moment from 'moment'
import { EventBus, axios, apiBaseUrl } from "../main"
import { CredentialTypes } from "../constants"
import { getPartnerProfile } from "../utils/partnerUtils"

Vue.use(Vuex)

const store = new Vuex.Store({
  state: {
    partners: [],
    editedDocument: {}, //document currently being edited
    documents: [],
    credentials: [],
    schemas:  [],
    busyStack: 0,
    expertMode: false
  },

  getters: {
    isBusy: state => {
      return state.busyStack > 0
    },
    publicDocumentsAndCredentials: state => {
      var retval = state.credentials.concat(state.documents.filter(d => d.type != CredentialTypes.PROFILE.name)).filter(d => d.isPublic == true)
      return retval
    },
    organizationalProfile: state => {
      var documents = state.documents.filter(d => d.type == CredentialTypes.PROFILE.name)
      if (documents.length == 1) return documents[0]
      else return undefined
    },
    getPartnerByDID: (state) => (did) => {
      return state.partners.find(partner => {
        partner.did === did
      })
    },
  },

  actions: {

    async loadSchemas({ commit }) {
      axios
        .get(`${apiBaseUrl}/admin/schema`)
        .then(result => {
          if ({}.hasOwnProperty.call(result, "data")) {
            let schemas = result.data
            commit({
              type: "loadSchemasFinished",
              schemas: schemas

            })
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        })
    },     
    async loadPartners({ commit }) {
      axios
        .get(`${apiBaseUrl}/partners`)
        .then(result => {
          if ({}.hasOwnProperty.call(result, "data")) {
            let partners = result.data
            partners = partners.map(partner => {
              partner.profile = getPartnerProfile(partner)
              return partner
            })

            commit({
              type: "loadPartnersFinished",
              partners: partners

            })
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        })
    },
    async loadDocuments({ commit }) {
      axios
        .get(`${apiBaseUrl}/wallet/document`)
        .then(result => {
          console.log(result)
          if ({}.hasOwnProperty.call(result, "data")) {
            var documents = result.data
            documents.map(documentIn => {
              documentIn.createdDate = moment(documentIn.createdDate)
              documentIn.updatedDate = moment(documentIn.updatedDate)
            });

            commit({
              type: "loadDocumentsFinished",
              documents: documents
            });
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        })
    },

    async loadCredentials({ commit }) {
      axios
        .get(`${apiBaseUrl}/wallet/credential`)
        .then(result => {
          if ({}.hasOwnProperty.call(result, "data")) {
            var credentials = [];
            result.data.forEach(credentialRef => {
              axios
                .get(`${apiBaseUrl}/wallet/credential/${credentialRef.id}`)
                .then(result => {
                  credentials.push(result.data)
                })
                .catch(e => {
                  console.error(e);
                  EventBus.$emit("error", e);
                })

            });
            commit({
              type: "loadCredentialsFinished",
              credentials: credentials
            });
          }
        })
        .catch(e => {
          console.error(e);
          EventBus.$emit("error", e);
        })
    },
    // async completeEditDocument({ state }) {
    //   if (state.editedDocument.add) {
    //     axios
    //       .post(`${apiBaseUrl}/wallet/document`, {
    //         document: state.editedDocument.document,
    //         isPublic: true, //TODO 
    //         type: state.editedDocument.type
    //       })
    //       .then(() => {
    //         this.dispatch('loadDocuments')
    //         EventBus.$emit("success", "Success");
    //       })
    //       .catch((e) => {
    //         console.error(e);
    //         EventBus.$emit("error", e);
    //       });
    //   }
    //   else {
    //     axios
    //       .put(`${apiBaseUrl}/wallet/document/${state.editedDocument.id}`, {
    //         document: state.editedDocument.document,
    //         isPublic: true, //TODO 
    //         type: state.editedDocument.type
    //       })
    //       .then(() => {
    //         this.dispatch('loadDocuments')
    //         EventBus.$emit("success", "Success");
    //       })
    //       .catch((e) => {
    //         console.error(e);
    //         EventBus.$emit("error", e);
    //       });
    //   }
    // }
  },

  mutations: {
    // initEditDocument(state, payload) {
    //   state.editedDocument.type = payload.documentType
    //   state.editedDocument.add = payload.add
    //   if (payload.add) {
    //     state.editedDocument.document = { ...emptyDocument };
    //   }
    //   else {
    //     state.editedDocument.id = payload.id
    //     var documents = state.documents.filter(d => d.id == payload.id)
    //     if (documents.length == 1) state.editedDocument.document = documents[0].documentData
    //   }
    // },
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
    }
  }
})

store.subscribeAction({
  before: (action, state) => {
    state.busyStack = state.busyStack + 1
  },
  after: (action, state) => {
    state.busyStack = state.busyStack - 1
  }
})

export default store