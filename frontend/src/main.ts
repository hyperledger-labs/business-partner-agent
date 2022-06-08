/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import "@/assets/scss/style.scss";

import Vue from "vue";
import { AxiosResponse } from "axios";
import VueNativeSock from "vue-native-websocket";
import App from "./App.vue";
import i18n from "./plugins/i18n";
import vuetify from "./plugins/vuetify";
import router from "./router";
import store from "./store";
import SortUtil from "./utils/sortUtils";
import "@/filters";

import VueJsonPretty from "vue-json-pretty";
import "vue-json-pretty/lib/styles.css";
import vue_moment from "vue-moment";
import { RuntimeConfig, settingsService } from "@/services";

Vue.component("vue-json-pretty", VueJsonPretty);

Vue.use(vue_moment);
// @ts-ignore
Vue.use(SortUtil);

const apiBaseUrl = process.env.VUE_APP_API_BASE_URL
  ? process.env.VUE_APP_API_BASE_URL
  : "/api";
const eventsHost = process.env.VUE_APP_EVENTS_HOST
  ? process.env.VUE_APP_EVENTS_HOST
  : window.location.host;
const socketApi = `${
  window.location.protocol === "https:" ? "wss" : "ws"
}://${eventsHost}/${process.env.VUE_APP_EVENTS_PATH}`;

if (process.env.NODE_ENV === "development") {
  store.commit({
    type: "setExpertMode",
    isExpert: true,
  });
}

Vue.use(VueNativeSock, socketApi, {
  store: store,
  format: "json",
  reconnection: true,
  passToStoreHandler: function (eventName, event) {
    if (!eventName.startsWith("SOCKET_")) {
      return;
    }
    console.log(event);
    let message = event;
    const method = "commit";
    let target = eventName.toUpperCase();
    if (target === "SOCKET_ONMESSAGE" && this.format === "json" && event.data) {
      message = JSON.parse(event.data);
      // method = 'dispatch';
      switch (message.message.type) {
        case "ON_MESSAGE_RECEIVED":
          target = "onMessageReceived";
          break;
        default:
          target = "onNotification";
      }
    }
    this.store[method](target, message);
  },
});

Vue.prototype.$apiBaseUrl = apiBaseUrl;
Vue.config.productionTip = false;
Vue.prototype.$config = {
  ledger: "iil",
  title: process.env.VUE_APP_TITLE || "Business Partner Agent",
  locale: process.env.VUE_APP_I18N_LOCALE || "en",
  fallbackLocale: process.env.VUE_APP_I18N_FALLBACK_LOCALE || "en",
};

// We need to load the configuration before the Vue application, so we can use the UX configuration
(async () => {
  console.log("Loading configuration...");

  let result: AxiosResponse<RuntimeConfig>;

  try {
    result = await settingsService.getSettingsRuntimeConfig();
  } catch (error) {
    console.error(error);
  }

  if (Object.prototype.hasOwnProperty.call(result, "data")) {
    Vue.prototype.$config = result?.data;
    const ledgerPrefix = Vue.prototype.$config.ledgerPrefix;
    const splitted = ledgerPrefix.split(":");
    Vue.prototype.$config.ledger = splitted[splitted.length - 2];
    if (result?.data.ux) {
      Object.assign(Vue.prototype.$config.ux, result.data.ux);
      console.log("...Configuration loaded");
    }
  }

  console.log("setting i18n...");
  i18n.locale = Vue.prototype.$config.locale;
  i18n.fallbackLocale = Vue.prototype.$config.fallbackLocale;
  console.log(
    `i18n.locale = ${i18n.locale}, i18n.fallbackLocale = ${i18n.fallbackLocale}`
  );

  await store.dispatch("loadSettings");
  await store.dispatch("loadSchemas");
  await store.dispatch("loadPartners");
  await store.dispatch("loadTags");
  await store.dispatch("loadProofTemplates");
  await store.dispatch("loadPartnerSelectList");
  await store.dispatch("loadCredDefSelectList");

  console.log("Create the Vue application");
  new Vue({
    vuetify,
    router,
    store,
    i18n,
    render: (h) => h(App),
  }).$mount("#app");
})();

const EventBus = new Vue();
export { EventBus, apiBaseUrl };

export { default as axios } from "axios";
