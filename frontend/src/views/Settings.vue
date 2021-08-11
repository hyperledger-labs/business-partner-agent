<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">Settings</v-card-title>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
            >Expert mode</v-list-item-title
          >
        </v-list-item-content>
        <v-list-item-action>
          <v-switch v-model="expertMode"></v-switch>
        </v-list-item-action>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
            >Schemas and Trusted Issuers</v-list-item-title
          >
        </v-list-item-content>
        <v-list-item-action>
          <v-btn icon :to="{ name: 'SchemaSettings' }">
            <v-icon color="grey">$vuetify.icons.next</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
            >Tags</v-list-item-title
          >
        </v-list-item-content>
        <v-list-item-action>
          <v-btn icon :to="{ name: 'TagManagement' }">
            <v-icon color="grey">$vuetify.icons.next</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-list-item>
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          Frontend Color
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          <text-field-color-picker
            v-if="isEditingColor"
            @on-save="onPickColor"
            @on-cancel="isEditingColor = false"
          >
          </text-field-color-picker>
          <span v-else>{{ $vuetify.theme.themes.light.primary }}</span>
        </v-list-item-subtitle>
        <v-list-item-action v-show="!isEditingColor">
          <v-btn icon x-small @click="isEditingColor = !isEditingColor">
            <v-icon dark>$vuetify.icons.pencil</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>

      <v-list-item v-if="!isLoading">
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium"
          >Wallet DID
        </v-list-item-title>
        <v-list-item-subtitle align="end" id="did">
          {{ this.status.did }}
        </v-list-item-subtitle>
        <v-btn icon x-small @click="copyDid">
          <v-icon dark>$vuetify.icons.copy</v-icon>
        </v-btn>
      </v-list-item>
      <v-list-item
        v-show="expertMode"
        v-for="setting in settings"
        :key="setting.text"
      >
        <!-- <v-list-item-content> -->
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ setting.text }}
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          {{ setting.value }}
        </v-list-item-subtitle>
        <!-- </v-list-item-content> -->
      </v-list-item>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import TextFieldColorPicker from "@/components/helper/TextFieldColorPicker";

export default {
  name: "Settings",
  created() {
    EventBus.$emit("title", "Settings");
    this.getStatus();
  },
  data: () => {
    return {
      isLoading: true,
      settingsHeader: [
        {
          text: "BPA Name",
          value: "agentName",
        },
        {
          text: "Host",
          value: "host",
        },
        {
          text: "Universal Resolver",
          value: "uniResolverUrl",
        },
        {
          text: "Ledger Browser",
          value: "ledgerBrowser",
        },
        {
          text: "Ledger DID Prefix",
          value: "ledgerPrefix",
        },
      ],
      isEditingColor: false,
    };
  },
  computed: {
    expertMode: {
      set(body) {
        this.$store.commit({
          type: "setExpertMode",
          isExpert: body,
        });
      },
      get() {
        return this.$store.state.expertMode;
      },
    },
    settings: {
      get() {
        return this.settingsHeader.map((setting) => {
          return {
            text: setting.text,
            value: this.$store.getters.getSettingByKey(setting.value),
          };
        });
      },
    },
  },
  methods: {
    onPickColor(c) {
      this.$vuetify.theme.themes.light.primary = c;
      localStorage.setItem("uiColor", c);
      this.isEditingColor = false;
    },
    getStatus() {
      console.log("Getting status...");
      this.$axios
        .get(`${this.$apiBaseUrl}/status`)
        .then((result) => {
          console.log(result);
          this.isWelcome = !result.data.profile;
          this.status = result.data;
          this.isLoading = false;
        })
        .catch((e) => {
          console.error(e);
          this.isLoading = false;
          EventBus.$emit("error", e);
        });
    },
    copyDid() {
      let didEl = document.querySelector("#did");
      const el = document.createElement("textarea");
      el.value = didEl.innerHTML.trim();
      document.body.appendChild(el);
      el.select();

      let successful;
      try {
        successful = document.execCommand("copy");
      } catch (err) {
        successful = false;
      }
      successful
        ? EventBus.$emit("success", "DID copied")
        : EventBus.$emit("error", "Can't copy DID");
      document.body.removeChild(el);
      window.getSelection().removeAllRanges();
    },
  },
  components: {
    "text-field-color-picker": TextFieldColorPicker,
  },
};
</script>
