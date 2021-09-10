/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/
import "@/assets/scss/style.scss";

import Vue from "vue";
import axios from "axios";
import VueNativeSock from "vue-native-websocket";
import App from "./App.vue";
import i18n from "./plugins/i18n";
import vuetify from "./plugins/vuetify";
import "@babel/polyfill";
import router from "./router";
import store from "./store";
import SortUtil from "./utils/sortUtils";
import "@/filters";

import VueJsonPretty from "vue-json-pretty";
import "vue-json-pretty/lib/styles.css";
Vue.component("vue-json-pretty", VueJsonPretty);

Vue.use(require("vue-moment"));
Vue.use(SortUtil);

var apiBaseUrl = process.env.VUE_APP_API_BASE_URL;
var eventsHost = process.env.VUE_APP_EVENTS_HOST
  ? process.env.VUE_APP_EVENTS_HOST
  : window.location.host;
var socketApi = `${
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
    let msg = event;
    let method = "commit";
    let target = eventName.toUpperCase();
    if (target === "SOCKET_ONMESSAGE") {
      if (this.format === "json" && event.data) {
        msg = JSON.parse(event.data);
        // method = 'dispatch';
        switch (msg.message.type) {
          case "ON_MESSAGE_RECEIVED":
            target = "onMessageReceived";
            break;
          default:
            target = "onNotification";
        }
      }
    }
    this.store[method](target, msg);
  },
});

Vue.prototype.$axios = axios;
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
  const result = await axios.get(`${apiBaseUrl}/admin/config`).catch((e) => {
    console.error(e);
  });
  if ({}.hasOwnProperty.call(result, "data")) {
    Vue.prototype.$config = result.data;
    let ledgerPrefix = Vue.prototype.$config.ledgerPrefix;
    let splitted = ledgerPrefix.split(":");
    Vue.prototype.$config.ledger = splitted[splitted.length - 2];
    if (result.data.ux) {
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

  Vue.prototype.$axiosErrorMessage = function (err) {
    console.error(err);
    if (!err) return "";
    // exceptions thrown from micronaut (ex. WrongApiUsageExceptionHandler)
    // will have the detail message in err.response.data._embedded.errors[N].message
    // check there first
    if (Array.isArray(err.response?.data?._embedded?.errors)) {
      return err.response?.data?._embedded?.errors.map(x => x.message).join(" ");
    }
    // what other error message structures will we encounter?
    // add logic here...

    // controller returning something like HttpResponse.notFound() sets err.message = "Request failed with status code 404"
    // but in err.response.statusText is a bit more understandable... "Not Found"
    // do we want to use the status text before the default message?
    if (err.response) {
      return i18n.t("error.axios", {statusText: err.response.statusText});
    }
    if (err.message) return err.message;
    return err.toString();
  };

  store.dispatch("loadSettings");
  store.dispatch("loadSchemas");
  store.dispatch("loadPartners");
  store.dispatch("loadTags");
  store.dispatch("loadProofTemplates");

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
export { EventBus, axios, apiBaseUrl };
