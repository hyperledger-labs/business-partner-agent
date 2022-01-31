<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-tabs fixed-tabs v-model="tab">
      <v-tab v-for="item in tabItems" :key="item.key" :href="`#${item.key}`">{{
        item.title
      }}</v-tab>
    </v-tabs>
    <v-tabs-items v-model="tab">
      <v-tab-item value="indy" transition="false">
        <slot name="indy"></slot>
      </v-tab-item>
      <v-tab-item value="json-ld" transition="false">
        <slot name="json-ld"></slot>
      </v-tab-item>
    </v-tabs-items>
  </v-container>
</template>
<script lang="ts">
export default {
  name: "credential-type-tabs",
  data: () => {
    return {
      tab: undefined,
    };
  },
  computed: {
    tabItems() {
      return [
        {
          title: this.$t("credentialType.indy"),
          key: "indy",
        },
        {
          title: this.$t("credentialType.jsonLd"),
          key: "json-ld",
        },
      ];
    },
  },
};
</script>
