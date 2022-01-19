<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container text-center>
    <div v-if="isWelcome && !isLoading">
      <!-- Image from undraw.co -->
      <v-img
        class="mx-auto"
        src="@/assets/undraw_welcome_3gvl_grey.png"
        max-width="300"
        aspect-ratio="1"
      ></v-img>
      <p
        v-bind:style="{ fontSize: `180%` }"
        class="grey--text text--darken-2 font-weight-medium"
      >
        {{ $t("view.dashboard.setupMessage") }}
      </p>
      <!-- <p v-bind:style="{ fontSize: `140%` }" class="grey--text text--darken-2 font-weight-medium">Start by adding a public profile that your business partners will see</p> -->
      <br />
      <v-bpa-button
        color="primary"
        :to="{
          name: 'DocumentAdd',
          params: { type: CredentialTypes.PROFILE.type },
        }"
        >{{ $t("view.dashboard.setupProfile") }}</v-bpa-button
      >
    </div>
    <div v-if="!isWelcome && !isLoading">
      <v-row>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.credentialsReceived')"
            icon="$vuetify.icons.wallet"
            :count="this.status.totals.credentialsReceived"
            :new-count="this.status.periodTotals.credentialsReceived"
            destination="Wallet"
          ></dashboard-card>
        </v-col>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.presentationRequestsSent')"
            icon="$vuetify.icons.proofRequests"
            :count="this.status.totals.presentationRequestsSent"
            :new-count="this.status.periodTotals.presentationRequestsSent"
          ></dashboard-card>
        </v-col>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.partners')"
            icon="$vuetify.icons.partners"
            :count="this.status.totals.partners"
            :new-count="this.status.periodTotals.partners"
            destination="Partners"
          ></dashboard-card>
        </v-col>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.credentialsSent')"
            icon="$vuetify.icons.credentialManagement"
            :count="this.status.totals.credentialsSent"
            :new-count="this.status.periodTotals.credentialsSent"
            destination="CredentialManagement"
          ></dashboard-card>
        </v-col>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.presentationRequestsReceived')"
            icon="$vuetify.icons.proofRequests"
            :count="this.status.totals.presentationRequestsReceived"
            :new-count="this.status.periodTotals.presentationRequestsReceived"
          ></dashboard-card>
        </v-col>
        <v-col class="col-12 col-sm-6 col-md-4">
          <dashboard-card
            :title="$t('view.dashboard.tasks')"
            icon="$vuetify.icons.notifications"
            :count="this.status.totals.tasks"
            :new-count="this.status.periodTotals.tasks"
            destination="Notifications"
          ></dashboard-card>
        </v-col>
      </v-row>
    </div>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import { CredentialTypes } from "@/constants";
import VBpaButton from "@/components/BpaButton";
import DashboardCard from "@/components/DashboardCard.vue";

export default {
  name: "Dashboard",
  components: { DashboardCard, VBpaButton },
  created() {
    EventBus.$emit("title", this.$t("view.dashboard.title"));
    this.getStatus();
  },
  data: () => {
    return {
      isWelcome: true,
      isLoading: true,
      CredentialTypes,
    };
  },
  computed: {
    partnerNotificationsCount() {
      return this.$store.getters.partnerNotificationsCount;
    },
    credentialNotificationsCount() {
      return this.$store.getters.credentialNotificationsCount;
    },
  },
  methods: {
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
        .catch((error) => {
          this.isLoading = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
