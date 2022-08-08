/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateTransactionAuthorAgreement } from "@/store/state-type";
import { adminService } from "@/services";

const state: IStateTransactionAuthorAgreement = {
  taaRequired: false,
  taaText: "No Transaction Author Agreement loaded",
  taaDigest: "",
  taaVersion: "-",
  taaLoaded: false,
};

const getters = {
  taaRequired: (state: IStateTransactionAuthorAgreement) => {
    return state.taaRequired;
  },
  taaText: (state: IStateTransactionAuthorAgreement) => {
    return state.taaText;
  },
  taaVersion: (state: IStateTransactionAuthorAgreement) => {
    return state.taaVersion;
  },
  taaLoaded: (state: IStateTransactionAuthorAgreement) => {
    return state.taaLoaded;
  },
};

const mutations = {
  setTaa(state: IStateTransactionAuthorAgreement, payload: any) {
    if (payload) {
      state.taaText = payload.taa.text;
      state.taaDigest = payload.taa.digest;
      state.taaVersion = payload.taa.version;
    }
    state.taaLoaded = true;
  },
  setTaaRequired(state: IStateTransactionAuthorAgreement, payload: any) {
    state.taaRequired = payload.taaRequired;
  },
};

const actions = {
  validateTaa(context: any) {
    adminService
      .isEndpointsWriteRequired()
      .then((result) => {
        const required = result.data;
        if (required) {
          context.dispatch("getTaa");
        }
        context.commit({ type: "setTaaRequired", taaRequired: required });
      })
      .catch((error) => {
        console.error(error);
        this.emitter.emit("error", error);
      });
  },

  getTaa(context: any) {
    adminService
      .getTAARecord()
      .then((result) => {
        context.commit({ type: "setTaa", taa: result.data });
      })
      .catch((error) => {
        context.commit({
          type: "setTaa",
          taa: { digest: "", version: state.taaVersion, text: state.taaText },
        });
        console.error(error);
        this.emitter.emit("error", error);
      });
  },

  registerTaa(context: any) {
    adminService
      .registerEndpoints({
        digest: state.taaDigest,
      })
      .then(() => {
        this.emitter.emit("success", "TAA registration fired");
        context.dispatch("validateTaa");
      })
      .catch((error) => {
        console.error(error);
        this.emitter.emit("error", error);
      });
  },
};

export default {
  state,
  mutations,
  actions,
  getters,
};
