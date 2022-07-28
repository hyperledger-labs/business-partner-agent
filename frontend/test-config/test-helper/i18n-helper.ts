// /*
//  * Copyright (c) 2020-2022 - for information on the respective copyright owner
//  * see the NOTICE file and/or the repository at
//  * https://github.com/hyperledger-labs/business-partner-agent
//  *
//  * SPDX-License-Identifier: Apache-2.0
//  */
// import {
//   mount,
//   shallowMount,
//   ThisTypedShallowMountOptions,
//   Wrapper,
// } from "@vue/test-utils";
// import VueI18n from "vue-i18n";
// import localeEnglish from "@/locales/en.json";
// import localeGerman from "@/locales/de.json";
// import localePolish from "@/locales/pl.json";
// import Vue, { createApp, useAttrs } from "vue";
// import Vuetify from "vuetify";
// import { createI18n } from "vue-i18n";
// import { config } from '@vue/test-utils'

// config.renderStubDefaultSlot = true

// export function shallowMountWithI18n(
//   // eslint-disable-next-line @typescript-eslint/no-explicit-any
//   component: any,
//   options?: ThisTypedShallowMountOptions<Vue>
// ): Wrapper<Vue> {
//   const localVue = createApp(App);

//   const i18n = createI18n({
//     allowComposition: true,
//     legacy: false,
//     locale: "en",
//     fallbackLocale: "en",
//     messages: {
//       en: localeEnglish,
//       de: localeGerman,
//       pl: localePolish,
//     },
//   });

//   localVue.use(Vuetify).use(i18n);

//   return shallowMount(component, {
//     i18n,
//     localVue,
//     ...options,
//   });
// }

// export function mountWithI18n(
//   // eslint-disable-next-line @typescript-eslint/no-explicit-any
//   component: any,
//   options?: ThisTypedShallowMountOptions<Vue>
// ): Wrapper<Vue> {
//   const localVue = createLocalVue();
//   localVue.use(VueI18n);
//   localVue.use(Vuetify);

//   const i18n = new VueI18n({
//     locale: "en",
//     fallbackLocale: "en",
//     messages: {
//       en: localeEnglish,
//       de: localeGerman,
//       pl: localePolish,
//     },
//   });

//   return mount(component, {
//     i18n,
//     localVue,
//     ...options,
//   });
// }
