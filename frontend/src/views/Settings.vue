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
            >Schema Settings</v-list-item-title
          >
          <v-list-item-subtitle>List and add schemas</v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn icon :to="{ name: 'SchemaSettings' }">
            <v-icon color="grey">mdi-chevron-right</v-icon>
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
            <v-icon dark>mdi-pencil</v-icon>
          </v-btn>
        </v-list-item-action>
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
  },
  data: () => {
    return {
      settingsHeader: [
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
        {
          text: "Aries Agent Url",
          value: "acaPyUrl",
        },
        {
          text: "Aries API Key",
          value: "acaPyApiKey",
        },
        {
          text: "BPA Name",
          value: "agentName",
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
  },
  components: {
    "text-field-color-picker": TextFieldColorPicker,
  },
};
</script>
