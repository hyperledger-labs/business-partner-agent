/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { Page, PageOptions, ProofTemplate } from "@/services";
import proofTemplateService from "@/services/proof-template-service";
import { EventBus } from "@/main";
import { IStateProofTemplates } from "@/store/state-type";

const state: IStateProofTemplates = {
  proofTemplateList: new Array<ProofTemplate>(),
};

export default {
  state,
  getters: {
    getProofTemplates: (state: IStateProofTemplates) => {
      return state.proofTemplateList;
    },
  },
  actions: {
    async loadProofTemplates(context: any) {
      const params = PageOptions.toUrlSearchParams(this.$options);
      proofTemplateService
        .getProofTemplates("", params) // this has to be updated
        .then((result) => {
          const proofTemplates: Page<ProofTemplate[]> = result.data;

          context.commit("setProofTemplates", proofTemplates);
        })
        .catch((error) => {
          console.error(error);
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setProofTemplates: (
      state: IStateProofTemplates,
      proofTemplates: ProofTemplate[]
    ) => {
      state.proofTemplateList = proofTemplates;
    },
  },
};
