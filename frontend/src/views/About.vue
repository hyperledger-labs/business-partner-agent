<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <p>
      {{ $t("view.about.textLicense") }}
    </p>
    <p>
      {{ $t("view.about.textRepo") }}
      <a
        href="https://github.com/hyperledger-labs/business-partner-agent"
        target="_blank"
        >GitHub</a
      >.
    </p>
    <h2>{{ $t("view.about.titleImages") }}</h2>
    <p>
      {{ $t("view.about.illustrationsBy") }}
      <a href="https://undraw.co/illustrations" target="_blank">unDraw</a>.
    </p>
    <p>
      {{ $t("view.about.logoBy") }}
      <a href="https://logodust.com" target="_blank">Logodust</a>.
    </p>
    <h2>{{ $t("view.about.titlePackages") }}</h2>
    <p></p>
    <v-row v-for="(item, index) in Licenses" :key="index" class="mx-auto">
      <v-card
        v-if="item && !item.ignore"
        class="mx-auto my-2"
        style="width: 100%"
      >
        <v-card-title class="headline">
          {{ item.name }}
        </v-card-title>
        <v-card-subtitle class="mx-auto">
          <div>{{ item.authors }}</div>
          <div><a v-html="item.url" :href="item.url" target="_blank"></a></div>
          <div>{{ $t("view.about.labelVersion") }}: {{ item.version }}</div>
          <div>{{ $t("view.about.labelLicense") }}: {{ item.license }}</div>
          <v-expansion-panels accordion>
            <v-expansion-panel>
              <v-expansion-panel-header
                >{{
                  $t("view.about.labelLicenseText")
                }}:</v-expansion-panel-header
              >
              <v-expansion-panel-content>
                {{ item.licenseText }}
              </v-expansion-panel-content>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-card-subtitle>
      </v-card>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Licenses from "../../licenses/licenseInfos.json";
import { EventBus } from "@/main";
export default {
  name: "About",
  created() {
    EventBus.$emit("title", this.$t("view.about.title"));
  },
  data: () => {
    return {
      Licenses,
    };
  },
};
</script>
