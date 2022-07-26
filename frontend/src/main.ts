/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import "@/assets/scss/style.scss";

import Vue, { createApp } from "vue";
import { AxiosResponse } from "axios";
import VueNativeSock from "vue-native-websocket";
import App from "./App.vue";
import i18n from "./plugins/i18n";
import vuetify from "./plugins/vuetify";
import router from "./router";
import store from "./store";
import filters from "@/filters";
import VueJsonPretty from "vue-json-pretty";
import "vue-json-pretty/lib/styles.css";
import { RuntimeConfig, settingsService } from "@/services";
import axios from "axios";
import VueAxios from "vue-axios";

const app = createApp(App)
  .use(i18n)
  .use(router)
  .use(store)
  .use(VueAxios, axios);
//.use(vuetify);

app.component("vue-json-pretty", VueJsonPretty);

app.config.globalProperties.$filters = filters;

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

app.config.globalProperties.$apiBaseUrl = apiBaseUrl;
app.config.globalProperties.$config = {
  ledger: "iil",
  title: process.env.VUE_APP_TITLE || "Business Partner Agent",
  locale: process.env.VUE_APP_I18N_LOCALE || "en",
  fallbackLocale: process.env.VUE_APP_I18N_FALLBACK_LOCALE || "en",
};

app.use(VueNativeSock, socketApi, {
  store: store,
  format: "json",
  reconnection: true,
  passToStoreHandler: function (eventName: string, event: any) {
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

// removed in vue3
// Vue.config.productionTip = false;

// We need to load the configuration before the Vue application, so we can use the UX configuration
(async () => {
  console.log("Loading configuration...");

  let result: AxiosResponse<RuntimeConfig>;

  try {
    result = await settingsService.getSettingsRuntimeConfig();
  } catch (error) {
    console.error(error);
  }
  let config;
  if (Object.prototype.hasOwnProperty.call(result, "data")) {
    config = result?.data;

    const ledgerPrefix = config.ledgerPrefix;
    const splitted = ledgerPrefix.split(":");
    config.ledger = splitted[splitted.length - 2];
    if (result?.data.ux) {
      Object.assign(config.ux, result.data.ux);
      console.log("...Configuration loaded");
    }
    app.config.globalProperties.$config = config;
  }

  // this.$vuetify.lang.current = locale; TODO

  console.log("setting i18n...");
  i18n.global.locale.value = app.config.globalProperties.$config.locale;
  i18n.global.fallbackLocale.value =
    app.config.globalProperties.$config.fallbackLocale;
  console.log(
    `i18n.locale = ${i18n.global.locale.value}, i18n.fallbackLocale = ${i18n.global.fallbackLocale.value}`
  );

  app.provide("$axiosErrorMessage", function (error: any) {
    console.error(error);
    if (!error) return "";
    // exceptions thrown from micronaut (ex. WrongApiUsageExceptionHandler)
    // will have the detail message in err.response.data._embedded.errors[N].message
    // check there first
    if (Array.isArray(error.response?.data?._embedded?.errors)) {
      return error.response?.data?._embedded?.errors
        .map((x: any) => x.message)
        .join(" ");
    }
    // what other error message structures will we encounter?
    // add logic here...

    // controller returning something like HttpResponse.notFound() sets err.message = "Request failed with status code 404"
    // but in err.response.statusText is a bit more understandable... "Not Found"
    // do we want to use the status text before the default message?
    if (error.response) {
      return i18n.global.t("error.axios", {
        statusText: error.response.statusText,
      });
    }
    if (error.message) return error.message;
    return error.toString();
  });

  store.dispatch("loadSchemas");
  store.dispatch("loadPartners");
  store.dispatch("loadTags");
  store.dispatch("loadProofTemplates");
  store.dispatch("loadPartnerSelectList");
  store.dispatch("loadCredDefSelectList");
  await store.dispatch("loadSettings");
  await store.dispatch("loadStatus");

  console.log("Create the Vue application");

  app.mount("#app");
})();

const EventBus = new Vue();
export { EventBus, apiBaseUrl, app };

export { default as axios } from "axios";
