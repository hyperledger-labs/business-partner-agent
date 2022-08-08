/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateStatus } from "@/store/state-type";
import { statusService, BPAStats } from "@/services";

const state: IStateStatus = {
  status: {} as BPAStats,
};

export default {
  state,
  getters: {
    getStatus: (state: IStateStatus): BPAStats => {
      return state.status;
    },
  },
  actions: {
    async loadStatus(context: any): Promise<void> {
      console.log("Getting status...");
      return statusService
        .getStatus()
        .then((result) => {
          console.log(result);

          context.commit("setStatus", result.data);
        })
        .catch((error) => {
          console.error(error);
          this.emitter.emit("error", error);
        });
    },
  },
  mutations: {
    setStatus: (state: IStateStatus, status: BPAStats) => {
      state.status = status;
    },
  },
};
