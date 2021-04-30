<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

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
      <span>Aries connection state: {{ this.state }}</span>
    </v-tooltip>
  </span>
</template>

<script>
export default {
  name: "PartnerStateIndicator",
  props: {
    state: String,
  },
  created() {},
  data: () => {
    return {};
  },
  computed: {
    color: function () {
      if (this.state === "request") {
        return "yellow";
      } else if (this.state === "inactive") {
        return "red";
      } else if (this.state === "active" || this.state === "response") {
        return "green";
      } else {
        return "grey";
      }
    },
  },
  methods: {},
};
</script>
