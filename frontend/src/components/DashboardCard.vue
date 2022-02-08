<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-card class="mx-auto dashboard-card">
    <v-card-title
      class="justify-center bg-light card-title"
      style="text-transform: capitalize"
      >{{ this.title }}</v-card-title
    >
    <v-card-text>
      <v-container>
        <v-row>
          <v-col class="col-6">
            <div class="container">
              <v-icon class="graphic">{{ this.icon }}</v-icon>
            </div>
          </v-col>
          <v-col class="col-6">
            <div class="container">
              <v-row>
                <v-col class="count">{{ this.count }}</v-col>
              </v-row>
              <v-row>
                <v-col class="new-count">{{ this.newCountDisplay }}</v-col>
              </v-row>
            </div>
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions class="justify-center">
      <v-btn v-if="this.destination" icon :to="{ name: this.destination }"
        ><v-icon color="primary">$vuetify.icons.dashboardGo</v-icon></v-btn
      >
      <v-btn v-else icon disabled
        ><!-- this is only in place to keep the size of the cards consistent --></v-btn
      >
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
export default {
  name: "DashboardCard",
  components: {},
  props: {
    title: {
      type: String,
      required: true,
    },
    icon: {
      type: String,
      default: () => "$vuetify.icons.user",
    },
    destination: {
      type: String,
    },
    count: {
      type: Number,
      default: () => 0,
    },
    newCount: {
      type: Number,
      default: () => 0,
    },
  },
  data: () => {
    return {};
  },
  computed: {
    newCountDisplay() {
      return this.newCount ? `(+ ${this.newCount})` : "";
    },
  },
  methods: {},
};
</script>
