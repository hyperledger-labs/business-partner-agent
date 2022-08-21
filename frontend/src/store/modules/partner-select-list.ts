/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import partnerService from "@/services/partner-service";
import { EventBus } from "@/main";
import { IStatePartnerSelectList } from "@/store/state-type";
import { PartnerAPI } from "@/services";
// FIXME: sever side paging makes this script useless: remove in future story if possible

const state: IStatePartnerSelectList = {
  partnerSelectList: new Array<PartnerAPI>(),
};

export default {
  state,
  getters: {
    getPartnerSelectList: (state: IStatePartnerSelectList) => {
      return state.partnerSelectList;
    },
  },
  actions: {
    async loadPartnerSelectList(context: any) {
      partnerService
        .getPartnersByInvitationStatus(false)
        .then((result) => {
          if (result.status === 200) {
            // filter out partners that are only at the invitation stage, we can't do anything until they accept.
            const partners = result.data.content;
            context.commit("setPartnerSelectList", partners);
          }
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setPartnerSelectList: (
      state: IStatePartnerSelectList,
      partners: PartnerAPI[]
    ) => {
      state.partnerSelectList = partners;
    },
  },
};
