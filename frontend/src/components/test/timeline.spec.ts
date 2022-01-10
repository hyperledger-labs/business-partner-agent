/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import "@testing-library/jest-dom";
import Timeline from "@/components/Timeline.vue";
import VueI18n from "vue-i18n";
import localeGerman from "@/locales/de.json";
import { renderWithVuetify } from "@/setup-jest";

test("should set [data-app] attribute on outer most div.", () => {
  const { container } = renderWithVuetify(Timeline, {});

  expect(container).toHaveAttribute("data-app", "true");
});

test("should have 'Timeline' as title when rendering the timeline component with defaults.", () => {
  const { getByText } = renderWithVuetify(Timeline, {});

  expect(getByText("Timeline")).toBeInTheDocument();
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
