<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <p>
      The Business Partner Agent is a Hyperledger Labs Open Source Project under
      Apache 2.0 license.
    </p>
    <p>
      If you find an issue you can file it at
      <a
        href="https://github.com/hyperledger-labs/business-partner-agent"
        target="_blank"
        >Github</a
      >.
    </p>
    <h2>Images</h2>
    <p>
      Illustrations are provided by
      <a href="https://undraw.co/illustrations" target="_blank">unDraw</a>.
    </p>
    <p>
      The default logo is provided by
      <a href="https://logodust.com" target="_blank">Logodust</a>.
    </p>
    <h2>Libraries and Packages</h2>
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
          <div>Version: {{ item.version }}</div>
          <div>License: {{ item.license }}</div>
        </v-card-subtitle>
      </v-card>
    </v-row>
    <v-row
      v-for="(item, index) in backendLicenses"
      :key="index"
      class="mx-auto"
    >
      <v-card v-if="item" class="mx-auto my-2" style="width: 100%">
        <v-card-title class="headline">
          {{ item.name.text }}
        </v-card-title>
        <v-card-subtitle class="mx-auto">
          <div>
            <a
              v-html="item.licenses.license.url.text"
              :href="item.licenses.license.url.text"
              target="_blank"
            ></a>
          </div>
          <div>Version: {{ item.version.text }}</div>
        </v-card-subtitle>
        <v-card-text>
          <h3>{{ item.licenses.license.name.text }}</h3>
          <div>
            License text:
            <a
              v-html="item"
              :href="item.licenses.license.url.text"
              target="_blank"
            ></a>
          </div>
        </v-card-text>
      </v-card>
    </v-row>
  </v-container>
</template>

<script>
import Licenses from "../../licenses/licenseInfos.json";
import { EventBus } from "../main";
export default {
  name: "About",
  components: {},
  created() {
    EventBus.$emit("title", "About");
    let backendJson = require("../../licenses/attribution.json");
    if (backendJson) {
      backendJson = backendJson.attributionReport.dependencies.dependency;
      this.backendLicenses = backendJson;
    }
  },
  data: () => {
    return {
      Licenses,
      backendLicenses: {},
    };
  },
  methods: {},
};
</script>
