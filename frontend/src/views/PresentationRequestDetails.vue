<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-card v-if="!isLoading" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ $t("view.presentationRequestDetails.header") }}
      </v-card-title>
      <v-card-text>
        <ProofRequest
          v-bind:proofRequest="presentationRequest.proofRequest"
          v-bind:credentials="this.$store.getters.getCredentials"
          isReadOnly
        ></ProofRequest>
        <v-divider></v-divider>
      </v-card-text>
      <v-layout align-end justify-end>
        <v-bpa-button
          color="secondary"
          @click="declinePresentationRequest(presentationRequest)"
          >Decline</v-bpa-button
        >
        <v-bpa-button
          :loading="this.isBusy"
          color="primary"
          @click="respondToPresentationRequest(presentationRequest)"
          >Accept</v-bpa-button
        >
        <v-tooltip v-if="presentationRequest.problemReport" top>
          <template v-slot:activator="{ on, attrs }">
            <v-icon
              color="error"
              small
              v-bind="attrs"
              v-on="on"
              style="margin-bottom: 11px; margin-right: 15px"
            >
              $vuetify.icons.connectionAlert
            </v-icon>
          </template>
          <span>{{ presentationRequest.problemReport }}</span>
        </v-tooltip>
      </v-layout>
      <v-card-actions>
        <v-expansion-panels v-if="expertMode" accordion flat>
          <v-expansion-panel>
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
              >Show raw data</v-expansion-panel-header
            >
            <v-expansion-panel-content class="bg-light">
              <vue-json-pretty :data="rawData"></vue-json-pretty>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import ProofRequest from "@/components/ProofRequest";
import VBpaButton from "@/components/BpaButton";

export default {
  props: {
    id: String,
  },
  mounted() {
    this.$store.dispatch("loadCredentials");
    this.fetch();
    console.log(this.$store.getters.getCredentials);
    this.$store.commit("presentationNotificationSeen", {id: this.id});
  },
  data: () => {
    return {
      rawData: {},
      presentationRequest: {},
      isLoading: true,
    };
  },
  methods: {
    fetch() {
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/proof-exchanges/${this.id}`)
        .then((result) => {
          console.log("result");
          console.log(result);
          if ({}.hasOwnProperty.call(result, "data")) {
            this.rawData = result.data;
            this.presentationRequest = result.data;
            this.isLoading = false;
          }
        })
        .catch((e) => {
          this.isLoading = false;
          if (e.response.status === 404) {
            this.data = [];
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    declinePresentationRequest(presentationRequest) {
      let partnerId = this.$route.params.id;
      this.$axios
        .post(
          `${this.$apiBaseUrl}/partners/${partnerId}/proof-exchanges/${presentationRequest.id}/decline`
        )
        .then((result) => {
          if (result.status === 200) {
            console.log("SUCCESSFULLY DECLINED");
            this.$router.go(-1);
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    respondToPresentationRequest(presentationRequest) {
      let partnerId = this.$route.params.id;
      this.$axios
        .post(
          `${this.$apiBaseUrl}/partners/${partnerId}/proof-exchanges/${presentationRequest.id}/prove`
        )
        .then((result) => {
          if (result.status === 200) {
            console.log("SUCCESSFULLY RESPONDED");
            this.$router.go(-1);
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
  },
  components: {
    ProofRequest,
    VBpaButton,
  },
};
</script>
