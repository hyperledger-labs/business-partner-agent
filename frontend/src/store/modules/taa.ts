/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { EventBus, axios, apiBaseUrl } from "../../main";

const state = {
  taaRequired: false,
  taaText: "No Transaction Author Agreement loaded",
  taaDigest: String,
  taaVersion: "-",
  taaLoaded: false,
};

const getters = {
  taaRequired: (state) => {
    return state.taaRequired;
  },
  taaText: (state) => {
    return state.taaText;
  },
  taaVersion: (state) => {
    return state.taaVersion;
  },
  taaLoaded: (state) => {
    return state.taaLoaded;
  },
};

const mutations = {
  setTaa(state, payload) {
    if (payload) {
      state.taaText = payload.taa.text;
      state.taaDigest = payload.taa.digest;
      state.taaVersion = payload.taa.version;
    }
    state.taaLoaded = true;
  },
  setTaaRequired(state, payload) {
    state.taaRequired = payload.taaRequired;
  },
};

const actions = {
  validateTaa({ commit, dispatch }) {
    axios
      .get(`${apiBaseUrl}/admin/endpoints/registrationRequired`)
      .then((result) => {
        const required = result.data;
        if (required) {
          dispatch("getTaa");
        }
        commit({ type: "setTaaRequired", taaRequired: required });
      })
      .catch((error) => {
        console.error(error);
        EventBus.$emit("error", error);
      });
  },

  getTaa({ commit }) {
    axios
      .get(`${apiBaseUrl}/admin/taa/get`)
      .then((result) => {
        commit({ type: "setTaa", taa: result.data });
      })
      .catch((error) => {
        commit({
          type: "setTaa",
          taa: { digest: "", version: state.taaVersion, text: state.taaText },
        });
        console.error(error);
        EventBus.$emit("error", error);
      });
  },

  registerTaa({ dispatch }) {
    axios
      .post(`${apiBaseUrl}/admin/endpoints/register`, {
        digest: state.taaDigest,
      })
      .then(() => {
        EventBus.$emit("success", "TAA registration fired");
        dispatch("validateTaa");
      })
      .catch((error) => {
        console.error(error);
        EventBus.$emit("error", error);
      });
  },
};

export default {
  state,
  mutations,
  actions,
  getters,
};
