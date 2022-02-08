<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <span>
    <v-tooltip right>
      <template v-slot:activator="{ on, attrs }">
        <v-icon x-small class="ml-2" :color="color" v-bind="attrs" v-on="on"
          >$vuetify.icons.partnerState</v-icon
        >
      </template>
      <span
        >{{ $t("component.partnerStateIndicator.connectionState") }}:
        {{ this.state }}</span
      >
    </v-tooltip>
  </span>
</template>

<script lang="ts">
import * as partnerUtils from "@/utils/partnerUtils";

export default {
  name: "PartnerStateIndicator",
  props: {
    state: String,
  },
  data: () => {
    return {};
  },
  computed: {
    color: function () {
      return partnerUtils.getPartnerStateColor(this.state);
    },
  },
};
</script>
