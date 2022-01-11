/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import Vue, { VueConstructor } from "vue";
import Vuetify from "vuetify";
import {
  ConfigurationCallback,
  render,
  RenderOptions,
} from "@testing-library/vue";
import VueI18n from "vue-i18n";
import localeEnglish from "@/locales/en.json";
import "@/filters";

Vue.use(Vuetify);

// Custom container to integrate Vuetify with Vue Testing Library.
// Vuetify requires you to wrap your app with a v-app component that provides
// a <div data-app="true"> node.
export const renderWithVuetify = (
  component: any,
  options?: RenderOptions<Vue>,
  callback?: ConfigurationCallback<Vue>
) => {
  const root = document.createElement("div");
  // eslint-disable-next-line unicorn/prefer-dom-node-dataset
  root.setAttribute("data-app", "true");

  // Use English as default locale when not defined otherwise
  const finalCallback =
    callback === undefined
      ? (vue: VueConstructor) => {
          vue.use(VueI18n);

          const i18n = new VueI18n({
            locale: "en",
            fallbackLocale: "en",
            messages: { en: localeEnglish },
          });

          return { i18n };
        }
      : callback;

  return render(
    component,
    {
      // eslint-disable-next-line unicorn/prefer-dom-node-append
      container: document.body.appendChild(root),
      // for Vuetify components that use the $vuetify instance property
      vuetify: new Vuetify(),
      ...options,
    },
    finalCallback
  );
};
