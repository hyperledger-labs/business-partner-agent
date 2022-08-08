/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { ProofTemplate } from "@/services";
import proofTemplateService from "@/services/proof-template-service";
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
      proofTemplateService
        .getProofTemplates()
        .then((result) => {
          const proofTemplates: ProofTemplate[] = result.data;

          context.commit("setProofTemplates", proofTemplates);
        })
        .catch((error) => {
          console.error(error);
          this.emitter.emit("error", error);
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
