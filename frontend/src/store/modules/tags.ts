/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import adminService from "@/services/admin-service";
import { EventBus } from "@/main";
import { IStateTags } from "@/store/state-type";
import { TagAPI } from "@/services";

const state: IStateTags = {
  tagList: new Array<TagAPI>(),
};

export default {
  state,
  getters: {
    getTags: (state: IStateTags) => {
      return state.tagList;
    },
  },
  actions: {
    async loadTags(context: any) {
      adminService
        .listTags()
        .then((result) => {
          const tags = result.data;
          console.log(tags);
          context.commit("setTags", tags);
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setTags: (state: IStateTags, tags) => {
      state.tagList = tags;
    },
  },
};
