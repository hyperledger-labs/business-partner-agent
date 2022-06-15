/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateExpertMode } from "@/store/state-type";

const state: IStateExpertMode = {
  isExpert: false,
};

export default {
  root: true,
  state,
  getters: {
    getExpertMode: (state: IStateExpertMode): boolean => {
      return state.isExpert;
    },
  },
  actions: {
    manuallySetExpertMode(context: any, isExpert: boolean) {
      context.commit("setExpertMode", isExpert);
    },
  },
  mutations: {
    setExpertMode: (state: IStateExpertMode, isExpert: boolean) => {
      state.isExpert = isExpert;
    },
  },
};
