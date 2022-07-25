<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container justify-center>
    <v-card class="my-4 mx-auto">
      <v-card-title class="bg-light">{{
        $t("view.settings.title")
      }}</v-card-title>
      <v-list-item v-if="!isLoading">
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ $t("view.settings.language") }}
        </v-list-item-title>
        <v-select
          v-model="selectedLocale"
          :items="availableLocales"
          item-text="label"
          item-value="locale"
          @change="changeLanguage($event)"
        ></v-select>
      </v-list-item>
      <v-list-item v-if="!isLoading">
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ $t("view.settings.walletDID") }}
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          {{ myDid }}
        </v-list-item-subtitle>
        <v-tooltip top>
          <template v-slot:activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              icon
              x-small
              v-on="on"
              @click="copyDid"
              @mouseout="reset"
            >
              <v-icon dark>$vuetify.icons.copy</v-icon>
            </v-btn>
          </template>
          <span>{{ copyText }}</span>
        </v-tooltip>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
            >{{ $t("view.settings.tags") }}</v-list-item-title
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
          {{ $t("view.settings.frontendColor") }}
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          <text-field-color-picker
            id="uiColor"
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
      <v-list-item>
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ $t("view.settings.iconsColor") }}
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          <text-field-color-picker
            id="uiColorIcons"
            :base-color="$vuetify.theme.themes.light.icons"
            v-if="isEditingColorIcons"
            @on-save="onPickColorIcons"
            @on-cancel="isEditingColorIcons = false"
          >
          </text-field-color-picker>
          <span v-else>{{ $vuetify.theme.themes.light.icons }}</span>
        </v-list-item-subtitle>
        <v-list-item-action v-show="!isEditingColorIcons">
          <v-btn
            icon
            x-small
            @click="isEditingColorIcons = !isEditingColorIcons"
          >
            <v-icon dark>$vuetify.icons.pencil</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
            >{{ $t("view.settings.expertMode") }}</v-list-item-title
          >
        </v-list-item-content>
        <v-list-item-action>
          <v-switch v-model="expertMode"></v-switch>
        </v-list-item-action>
      </v-list-item>
      <v-list-item
        v-show="expertMode"
        v-for="setting in settings"
        :key="setting.text"
      >
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ setting.text }}
        </v-list-item-title>
        <v-list-item-subtitle align="end">
          {{ setting.value }}
        </v-list-item-subtitle>
      </v-list-item>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import TextFieldColorPicker from "@/components/helper/TextFieldColorPicker.vue";
import { LocaleMetaType } from "@/views/settings/locale-meta-type";
import { BPAStats } from "@/services";
import { VuetifyThemeItem } from "vuetify/types/services/theme";
import i18n from "@/plugins/i18n";

export default {
  name: "Settings",
  created() {
    EventBus.$emit("title", this.$t("view.settings.title"));
    this.getStatus();
    this.copyText = this.$t("button.clickToCopy");
  },
  data: () => {
    return {
      status: {} as BPAStats,
      isLoading: true,
      selectedLocale: {
        locale: i18n.global.locale,
      },
      isEditingColor: false,
      isEditingColorIcons: false,
      myDid: "",
      copyText: "",
    };
  },
  computed: {
    settingsHeader() {
      return [
        {
          text: this.$t("view.settings.header.agentName"),
          value: "agentName",
        },
        {
          text: this.$t("view.settings.header.host"),
          value: "host",
        },
        {
          text: this.$t("view.settings.header.uniResolverUrl"),
          value: "uniResolverUrl",
        },
        {
          text: this.$t("view.settings.header.ledgerBrowser"),
          value: "ledgerBrowser",
        },
        {
          text: this.$t("view.settings.header.ledgerPrefix"),
          value: "ledgerPrefix",
        },
        {
          text: this.$t("view.settings.header.uptime"),
          value: "uptime",
        },
        {
          text: this.$t("view.settings.header.buildVersion"),
          value: "buildVersion",
        },
      ];
    },
    availableLocales() {
      return this.$i18n.availableLocales.map((availableLocale: any) => {
        const { meta } = this.$i18n.getLocaleMessage(
          availableLocale
        ) as unknown as LocaleMetaType;

        let selectLabel = availableLocale;
        if (meta) {
          if (meta.label !== undefined && meta.label !== "") {
            selectLabel = meta.label;
          } else if (
            meta.langNameNative !== undefined &&
            meta.langNameNative !== ""
          ) {
            selectLabel = meta.langNameNative;
            if (
              meta.langNameEnglish !== undefined &&
              meta.langNameEnglish !== ""
            ) {
              selectLabel += ` (${meta.langNameEnglish})`;
            }
          }
        }

        return {
          locale: availableLocale,
          label: selectLabel,
        };
      });
    },
    expertMode: {
      set(body: boolean) {
        this.$store.dispatch("manuallySetExpertMode", body);
      },
      get() {
        return this.$store.getters.getExpertMode;
      },
    },
    settings: {
      get() {
        return this.settingsHeader.map(
          (setting: { text: string; value: string }) => {
            return {
              text: setting.text,
              value: this.$store.getters.getSettingByKey(setting.value),
            };
          }
        );
      },
    },
  },
  methods: {
    changeLanguage(locale: string) {
      this.$i18n.locale = locale;
      this.$vuetify.lang.current = locale;
      localStorage.setItem("locale", locale);
      EventBus.$emit("title", this.$t("view.settings.title"));
    },
    onPickColor(c: VuetifyThemeItem) {
      this.$vuetify.theme.themes.light.primary = c;
      localStorage.setItem("uiColor", c.toString());
      this.isEditingColor = false;
    },
    onPickColorIcons(c: VuetifyThemeItem) {
      this.$vuetify.theme.themes.light.icons = c;
      localStorage.setItem("uiColorIcons", c.toString());
      this.isEditingColorIcons = false;
    },
    getStatus() {
      this.status = this.$store.getters.getStatus;
      this.myDid = this.status.did;
      this.isLoading = false;
    },
    async copyDid() {
      await navigator.clipboard.writeText(this.myDid);
      this.copyText = this.$t("button.copied");
    },
    reset() {
      this.copyText = this.$t("button.clickToCopy");
    },
  },
  components: {
    "text-field-color-picker": TextFieldColorPicker,
  },
};
</script>
