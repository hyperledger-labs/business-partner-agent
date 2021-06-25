<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container v-if="!isLoading">
    <vue-json-pretty :data="rawData"></vue-json-pretty>
    <v-card-actions>
      <v-layout align-end justify-end>
        <v-btn
          color="secondary"
          text
          @click="rejectPresentationRequest(presentationRequest)"
          >Reject</v-btn
        >
        <v-btn
          :loading="this.isBusy"
          color="primary"
          text
          @click="respondToPresentationRequest(presentationRequest)"
          :disabled="submitDisabled"
          >Accept</v-btn
        >
      </v-layout>
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
  </v-container>
</template>

<script>
import { EventBus } from "../main";

export default {
  props: {
    id: String,
  },
  mounted() {
    console.log("MOUNTED");
    this.fetch();
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
        .get(`${this.$apiBaseUrl}/proof-exchanges/${this.id}`)
        .then((result) => {
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
    rejectPresentationRequest(presentationRequest) {
      let partnerId = this.$route.params.id;
      this.$axios
        .post(
          `${this.$apiBaseUrl}/partners/${partnerId}/proof-exchanges/${presentationRequest.id}/reject`
        )
        .then((result) => {
          if (result.status === 200) {
            console.log("SUCCESSFULLY REJECTED");
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
};
</script>
