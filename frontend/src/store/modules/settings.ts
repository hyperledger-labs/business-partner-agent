/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { IStateSettings } from "@/store/state-type";
import { RuntimeConfig, settingsService } from "@/services";
import { EventBus } from "@/main";

const state: IStateSettings = {
  settings: {} as RuntimeConfig,
};

export default {
  state,
  getters: {
    getSettingByKey: (state: IStateSettings) => (key: string) => {
      if (
        state.settings &&
        Object.prototype.hasOwnProperty.call(state.settings, key)
      ) {
        return state.settings[key];
      }
    },
    getSettings: (state: IStateSettings) => {
      return state.settings;
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
          EventBus.$emit("error", error);
        });
    },
  },
  mutations: {
    setSettings: (state: IStateSettings, settings: RuntimeConfig) => {
      state.settings = settings;
    },
  },
};
