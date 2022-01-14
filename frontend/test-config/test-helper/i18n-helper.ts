/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  createLocalVue,
  shallowMount,
  ThisTypedShallowMountOptions,
  Wrapper,
} from "@vue/test-utils";
import VueI18n from "vue-i18n";
import localeEnglish from "@/locales/en.json";
import localeGerman from "@/locales/de.json";
import localePolish from "@/locales/pl.json";
import Vue from "vue";

export function shallowMountWithI18n(
  component: any,
  options?: ThisTypedShallowMountOptions<Vue>
): Wrapper<Vue, Element> {
  const localVue = createLocalVue();
  localVue.use(VueI18n);

  const i18n = new VueI18n({
    locale: "en",
    fallbackLocale: "en",
    messages: {
      en: localeEnglish,
      de: localeGerman,
      pl: localePolish,
    },
  });

  return shallowMount(component, {
    i18n,
    localVue,
    ...options,
  });
}
