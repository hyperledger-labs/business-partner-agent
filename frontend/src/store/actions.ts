/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { EventBus } from "@/main";
import adminService from "@/services/admin-service";

export const loadTags = async ({ commit }) => {
  adminService
    .listTags()
    .then((result) => {
      const tags = result.data;
      console.log(tags);
      commit({
        type: "setTags",
        tags: tags,
      });
    })
    .catch((error) => {
      console.error(error);
      EventBus.$emit("error", error);
    });
};
