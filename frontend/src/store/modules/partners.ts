/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { PartnerAPI } from "@/services";
import { IStatePartners } from "@/store/state-type";
import partnerService from "@/services/partner-service";
import { EventBus } from "@/main";
// TODO: sever side paging makes this script useless: remove in future story if possible
const state: IStatePartners = {
  partnerList: new Array<PartnerAPI>(),
};

export default {
  state,
  getters: {
    getPartners: (state: IStatePartners) => {
      return state.partnerList;
    },
    partnersCount: (state: IStatePartners): number => {
      return state.partnerList ? state.partnerList.length : 0;
    },
    getPartnerByDID: (state: IStatePartners) => (did: string) => {
      return state.partnerList.find((partner) => {
        return partner.did === did;
      });
    },
  },
  actions: {
    async loadPartners(context: any) {
      try {
        const response = await partnerService.getAll();
        context.commit("loadPartnersFinished", response.data.content);
      } catch (error) {
        EventBus.$emit("error", error);
      }
    },
  },
  mutations: {
    loadPartnersFinished: (state: IStatePartners, partners: PartnerAPI[]) => {
      state.partnerList = partners;
    },
  },
};
