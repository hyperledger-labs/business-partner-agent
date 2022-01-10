/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import "@testing-library/jest-dom";
import { render } from "@testing-library/vue";
import Vue from "vue";
import Timeline from "@/components/Timeline.vue";
import VueI18n from "vue-i18n";
import localeEnglish from "@/locales/en.json";
import localeGerman from "@/locales/de.json";
import "@/filters";
import Vuetify from "vuetify";

// can be moved to global instance in jest test setup
Vue.use(Vuetify);

// Custom container to integrate Vuetify with Vue Testing Library.
// Vuetify requires you to wrap your app with a v-app component that provides
// a <div data-app="true"> node.
const renderWithVuetify = (component, options?, callback?) => {
  const root = document.createElement("div");
  // eslint-disable-next-line unicorn/prefer-dom-node-dataset
  root.setAttribute("data-app", "true");

  return render(
    component,
    {
      // eslint-disable-next-line unicorn/prefer-dom-node-append
      container: document.body.appendChild(root),
      // for Vuetify components that use the $vuetify instance property
      vuetify: new Vuetify(),
      ...options,
    },
    callback
  );
};

test("should set [data-app] attribute on outer most div.", () => {
  const { container } = renderWithVuetify(Timeline, {}, (vue) => {
    vue.use(VueI18n);

    const i18n = new VueI18n({
      locale: "en",
      fallbackLocale: "en",
      messages: { en: localeEnglish },
    });

    return { i18n };
  });

  expect(container).toHaveAttribute("data-app", "true");
});

test("should have 'Zeitleiste' as title when rendering the timeline component in German.", () => {
  const { getByText } = renderWithVuetify(Timeline, {}, (vue) => {
    vue.use(VueI18n);

    const i18n = new VueI18n({
      locale: "de",
      fallbackLocale: "de",
      messages: { de: localeGerman },
    });

    return { i18n };
  });

  expect(getByText("Zeitleiste")).toBeInTheDocument();
});
