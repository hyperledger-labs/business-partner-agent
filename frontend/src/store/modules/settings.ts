/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateSettings } from "@/store/state-type";
import { RuntimeConfig, settingsService } from "@/services";

const state: IStateSettings = {
  config: {} as RuntimeConfig,
};

export default {
  state,
  getters: {
    getSettingByKey: (state: IStateSettings) => (key: string) => {
      if (
        state.config &&
        Object.prototype.hasOwnProperty.call(state.config, key)
      ) {
        return state.config[key as keyof RuntimeConfig];
      }
    },
    getSettingsConfig: (state: IStateSettings) => {
      return state.config;
    },
  },
  actions: {
    async loadSettings(context: any) {
      settingsService
        .getSettingsRuntimeConfig()
        .then((result) => {
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            const settings = result.data;
            context.commit("setSettings", settings);
          }
        })
        .catch((error) => {
          console.error(error);
          this.emitter.emit("error", error);
        });
    },
  },
  mutations: {
    setSettings: (state: IStateSettings, settings: RuntimeConfig) => {
      state.config = settings;
    },
  },
};
