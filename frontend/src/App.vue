<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-app>
    <v-navigation-drawer
      v-model="drawer"
      app
      :disable-resize-watcher="hideSidebar"
    >
      <v-list dense>
        <router-link tag="span" :to="{ name: 'Dashboard' }">
          <v-list-item
            v-if="ux.navigation.avatar.agent.enabled"
            two-line
            class="pl-3 mt-n2 logo"
          >
            <v-list-item-content>
              <v-list-item-title v-if="ux.navigation.avatar.agent.default">
                <v-img
                  v-if="logo"
                  contain
                  max-height="100"
                  max-width="228"
                  :src="logo"
                ></v-img>
                <v-img
                  v-else
                  contain
                  max-height="100"
                  max-width="228"
                  src="@/assets/logo_default.svg"
                ></v-img>
              </v-list-item-title>
              <v-list-item-title v-else>
                <v-img
                  contain
                  max-height="100"
                  max-width="228"
                  :src="ux.navigation.avatar.agent.src"
                ></v-img
              ></v-list-item-title>
              <v-list-item-subtitle class="mt-2 text-wrap nav-display-name">{{
                getNavDisplayName
              }}</v-list-item-subtitle>
            </v-list-item-content>
          </v-list-item>
        </router-link>
        <v-list-item v-if="expertMode" link :to="{ name: 'Identity' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.identity</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t("nav.identity") }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Dashboard' }" exact>
          <v-list-item-action>
            <v-icon>$vuetify.icons.dashboard</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t("nav.dashboard") }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'Notifications' }">
          <v-list-item-action>
            <v-badge
              overlap
              bordered
              :content="notificationsCount"
              :value="notificationsCount"
              color="red"
              offset-x="10"
              offset-y="10"
            >
              <v-icon>$vuetify.icons.notifications</v-icon>
            </v-badge>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{
              $t("view.notifications.title")
            }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'PublicProfile' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.profile</v-icon>
          </v-list-item-action>
          <v-list-item-title>{{ $t("view.profile.title") }}</v-list-item-title>
        </v-list-item>
        <v-list-item link :to="{ name: 'Wallet' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.wallet</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t("view.wallet.title") }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item link :to="{ name: 'CredentialManagement' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.credentialManagement</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{
              $t("view.issueCredentials.title")
            }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item link :to="{ name: 'ProofTemplates' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.proofTemplates</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{
              $t("view.proofTemplates.title")
            }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>

        <v-list-item link :to="{ name: 'Partners' }">
          <v-list-item-action>
            <v-icon>$vuetify.icons.partners</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              {{ $t("view.partners.title") }}</v-list-item-title
            >
          </v-list-item-content>
        </v-list-item>
        <v-list-item
          v-if="ux.navigation.settings.location === 'top'"
          link
          :to="{ name: 'Settings' }"
        >
          <v-list-item-action>
            <v-icon>$vuetify.icons.settings</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>{{ $t("nav.settings") }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <template v-slot:append>
        <v-list dense>
          <v-list-item
            v-if="ux.navigation.settings.location === 'bottom'"
            bottom
            link
            :to="{ name: 'Settings' }"
          >
            <v-list-item-action>
              <v-icon>$vuetify.icons.settings</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>{{ $t("nav.settings") }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            v-if="ux.navigation.about.enabled"
            bottom
            link
            :to="{ name: 'About' }"
          >
            <v-list-item-action>
              <v-icon>$vuetify.icons.about</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>{{ $t("nav.about") }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            v-if="ux.navigation.logout.enabled"
            bottom
            @click="logout()"
          >
            <v-list-item-action>
              <v-icon>$vuetify.icons.signout</v-icon>
            </v-list-item-action>
            <v-list-item-content>
              <v-list-item-title>{{ $t("nav.signout") }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="primary" app flat dark>
      <v-badge
        v-show="!hideBurgerButton && !drawer"
        overlap
        bordered
        :content="notificationsCount"
        :value="notificationsCount"
        color="red"
        offset-x="53"
        offset-y="53"
      >
        <v-app-bar-nav-icon @click.stop="drawer = !drawer" />
      </v-badge>
      <v-app-bar-nav-icon
        v-show="drawer && !hideBurgerButton"
        @click.stop="drawer = !drawer"
      />
      <v-toolbar-title>{{ getTitle }}</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-btn v-if="ux.header.logout.enabled" icon @click="logout()">
        <v-icon>$vuetify.icons.signout</v-icon>
      </v-btn>
    </v-app-bar>

    <v-main>
      <app-taa v-if="!sessionDialog && $store.getters.taaRequired"></app-taa>
      <router-view
        v-if="!sessionDialog && !$store.getters.taaRequired"
      ></router-view>
      <v-btn
        color="primary"
        fab
        dark
        bottom
        right
        fixed
        @click="showChatWindow"
        style="text-decoration: none"
      >
        <v-badge
          overlap
          bordered
          :content="messagesReceivedCount"
          :value="messagesReceivedCount"
          color="red"
          offset-x="10"
          offset-y="10"
        >
          <v-icon v-if="chatWindow">$vuetify.icons.close</v-icon>
          <v-icon v-else>$vuetify.icons.chat</v-icon>
        </v-badge>
      </v-btn>
    </v-main>

    <v-snackbar
      v-model="snackbar"
      :bottom="true"
      :color="color"
      :multi-line="false"
      :right="true"
      :timeout="5000"
      :top="false"
      :vertical="true"
    >
      {{ snackbarMsg }}
      <v-btn dark text @click="snackbar = false">{{
        $t("app.snackBar.close")
      }}</v-btn>
    </v-snackbar>

    <v-dialog v-model="sessionDialog" max-width="290">
      <v-card>
        <v-card-title class="headline">{{
          $t("app.sessionDialog.headline")
        }}</v-card-title>

        <v-card-text>{{ $t("app.sessionDialog.text") }}</v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn color="warning" text @click="sessionDialog = false">{{
            $t("app.sessionDialog.no")
          }}</v-btn>

          <v-btn color="green darken-1" text @click="logout()">{{
            $t("app.sessionDialog.yes")
          }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <div
      class="chat-window"
      :class="{ opened: chatWindow, closed: !chatWindow }"
      style="z-index: 100"
    >
      <BasicMessages ref="basicMessages" />
    </div>

    <v-footer app>
      <v-col cols="12" class="text-center">
        <span v-if="showFooter && imprintUrl" class="mr-4 subtitle-2"
          ><a :href="imprintUrl">{{
            $t("app.footer.imprintUrl.text")
          }}</a></span
        ><span v-else class="mr-4 subtitle-2">&nbsp;</span>
        <span v-if="showFooter && privacyPolicyUrl" class="subtitle-2"
          ><a :href="privacyPolicyUrl">{{
            $t("app.footer.privacyPolicyUrl.text")
          }}</a></span
        ><span v-else class="mr-4 subtitle-2">&nbsp;</span>
      </v-col>
    </v-footer>
  </v-app>
</template>

<script lang="ts">
import { EventBus } from "./main";
import Taa from "./components/taa/TransactionAuthorAgreement.vue";
import BasicMessages from "@/components/messages/BasicMessages.vue";
import merge from "deepmerge";
import i18n from "@/plugins/i18n";
import { getBooleanFromString } from "@/utils/textUtils";

export default {
  components: {
    BasicMessages,
    "app-taa": Taa,
  },
  props: {
    source: String,
  },
  data: () => ({
    title: "",
    drawer: !getBooleanFromString(window.env.SIDEBAR_CLOSE_ON_STARTUP),
    logo: process.env.VUE_APP_LOGO_URL,

    // snackbar stuff
    snackbar: false,
    color: "",
    snackbarMsg: "",

    sessionDialog: false,

    chatWindow: false,
    // These are defaults, if no ux configuration passed in via $config.ux...
    ux: {
      header: {
        logout: {
          enabled: true,
        },
      },
      navigation: {
        avatar: {
          agent: {
            enabled: true,
            default: true,
            "show-name": true,
          },
        },
        about: {
          enabled: true,
        },
        logout: {
          enabled: false,
        },
        settings: {
          enabled: true,
          location: "top",
        },
      },
    },
  }),
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    showFooter() {
      return (this.$store.state.settings.imprint &&
        typeof this.$store.state.settings.imprint === "string") ||
        (this.$store.state.settings.dataPrivacyPolicy &&
          typeof this.$store.state.settings.dataPrivacyPolicy === "string")
        ? this.$store.state.settings.imprint.length +
            this.$store.state.settings.dataPrivacyPolicy.length >
            0
        : undefined;
    },
    imprintUrl() {
      return this.$store.state.settings.imprint;
    },
    privacyPolicyUrl() {
      return this.$store.state.settings.dataPrivacyPolicy;
    },
    messagesReceivedCount() {
      return this.$store.getters.messagesCount;
    },
    notificationsCount() {
      return (
        this.$store.getters.taskNotificationsCount +
        this.$store.getters.activityNotificationsCount
      );
    },
    hideBurgerButton() {
      return getBooleanFromString(window.env.SIDEBAR_HIDE_BURGER_BUTTON);
    },
    hideSidebar() {
      return getBooleanFromString(window.env.SIDEBAR_CLOSE_ON_STARTUP);
    },
    getAgentName() {
      let bpaName = this.$t("app.bpaDefaultName");
      const nameSettingValue = this.$store.getters.getSettingByKey("agentName");
      if (nameSettingValue) {
        bpaName = nameSettingValue;
      }
      return bpaName;
    },
    getOrganizationName() {
      let profile = this.$store.getters.getOrganizationalProfile;
      if (profile) {
        return profile["label"];
      }
      return "";
    },
    getNavDisplayName() {
      if (this.ux.navigation.avatar.agent["show-name"]) {
        const result = this.getOrganizationName;
        return result ? result : this.getAgentName;
      }
      return "";
    },
    getTitle() {
      return this.title;
    },
  },
  created() {
    // Set the browser/tab title...
    document.title = this.$config.title;
    if (this.$config.ux) {
      this.ux = merge(this.ux, this.$config.ux);

      // Copy the the configuration UX themes, this allows us to change primary color later...
      if (this.ux.theme) {
        this.$vuetify.theme.dark = this.ux.theme.dark
          ? this.ux.theme.dark
          : false;
        this.$vuetify.theme.themes.light = merge(
          this.$vuetify.theme.themes.light,
          this.ux.theme.themes.light
        );
      }

      const uiColor = localStorage.getItem("uiColor");

      if (uiColor) {
        // if the user stored an override of the primary color, load it.
        this.$vuetify.theme.themes.light.primary = uiColor;
      }

      const uiColorIcons = localStorage.getItem("uiColorIcons");

      if (uiColorIcons) {
        // if the user stored an override of the icons color, load it.
        this.$vuetify.theme.themes.light.icons = uiColorIcons;
      }

      const locale =
        localStorage.getItem("locale") ||
        navigator.language.split("-")[0] ||
        process.env.VUE_APP_I18N_LOCALE ||
        "en";
      i18n.locale = locale;
      this.$vuetify.lang.current = locale;

      // Load up an alternate favicon
      if (this.ux.favicon) {
        document
          .querySelector("#favicon")
          .setAttribute("href", this.ux.favicon.href);
      }
    }

    this.$store.dispatch("validateTaa");
    this.$store.dispatch("loadDocuments");

    // Global Error handling
    // Todo: Put in extra component

    EventBus.$on("title", (title) => {
      this.title = title;
    });

    EventBus.$on("success", (message) => {
      (this.snackbarMsg = message),
        (this.color = "green"),
        (this.snackbar = true);
    });

    EventBus.$on("error", (message) => {
      console.log(message.response);

      if (
        Object.prototype.hasOwnProperty.call(message, "response") &&
        Object.prototype.hasOwnProperty.call(message.response, "status")
      ) {
        switch (message.response.status) {
          case 401:
            this.sessionDialog = true;
        }

        if (
          Object.prototype.hasOwnProperty.call(message.response, "data") &&
          Object.prototype.hasOwnProperty.call(message.response.data, "message")
        ) {
          message = message.response.data.message;
        }
      }

      (this.snackbarMsg = message),
        (this.color = "red"),
        (this.snackbar = true);
    });
  },
  methods: {
    logout() {
      // logout must have get-allowed, get the browser to do all the logout redirects...
      location.href = `${this.$apiBaseUrl}/logout`;
    },
    async showChatWindow() {
      if (!this.chatWindow) {
        // we are opening it...
        // load the rooms first (may be new partners we haven't loaded)
        await this.$refs.basicMessages.loadRooms();
      }
      // now, open or close it
      this.chatWindow = !this.chatWindow;
    },
  },
};
</script>
<style>
.bg-light {
  background-color: #fafafa;
}
a {
  text-decoration: none;
}
</style>
