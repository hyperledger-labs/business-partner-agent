<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-data-table
    :hide-default-footer="presentationRequests.length < 10"
    v-model="selected"
    :show-select="selectable"
    :headers="headers"
    :items="presentationRequests"
    :expanded.sync="expanded"
    item-key="index"
    :sort-by="['createdAt']"
    :sort-desc="[false]"
    single-select
  >
    <template v-slot:[`item.createdAt`]="{ item }">
      {{ item.createdAt | formatDateLong }}
    </template>
    <template v-slot:[`item.sentAt`]="{ item }">
      {{ item.sentAt | formatDateLong }}
    </template>
    <template v-slot:[`item.receivedAt`]="{ item }">
      {{ item.receivedAt | formatDateLong }}
    </template>
    <template v-slot:[`item.state`]="{ item }">
      <v-icon v-if="isItemActive(item)" color="green">mdi-check</v-icon>
      <span v-else>
        {{ item.state.replace("_", " ") }}
      </span>
    </template>
    <template v-slot:[`item.actions`]="{ item }">
      <div v-if="item.state === 'request_received'">
        <v-icon small @click.stop="rejectPresentationRequest(item)">
          $vuetify.icons.close
        </v-icon>
        <v-icon small @click.stop="respondToPresentationRequest(item)">
          $vuetify.icons.check
        </v-icon>
        <v-tooltip v-if="item.problemReport" top>
          <template v-slot:activator="{ on, attrs }">
            <v-icon color="error" small v-bind="attrs" v-on="on">
              $vuetify.icons.connectionAlert
            </v-icon>
          </template>
          <span>{{ item.problemReport }}</span>
        </v-tooltip>
      </div>
    </template>
  </v-data-table>
</template>

<script>
import { EventBus } from "../main";
import { CredentialTypes } from "../constants";
import { presentationListHeaders } from "@/components/tableHeaders/PresentationListHeaders";

export default {
  props: {
    presentationRequests: Array,
    selectable: {
      type: Boolean,
      default: false,
    },
    headers: {
      type: Array,
      default: () => presentationListHeaders,
    },
    isActiveFn: {
      type: Function,
      default: (item) =>
        item.state === "verified" || item.state == "presentation_acked",
    },
  },
  data: () => {
    return {
      selected: [],
      CredentialTypes: CredentialTypes,
      expanded: [],
    };
  },
  methods: {
    rejectPresentationRequest(presentationRequest) {
      let partnerId = this.$route.params.id;
      this.$axios
        .post(
          `${this.$apiBaseUrl}/partners/${partnerId}/proof-exchanges/${presentationRequest.id}/reject`
        )
        .then((result) => {
          if (result.status === 200) {
            this.$emit("removedItem", presentationRequest.id);
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
            this.$emit("responseSuccess", presentationRequest.id);
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    isItemActive(item) {
      return this.isActiveFn(item);
    },
  },
};
</script>
