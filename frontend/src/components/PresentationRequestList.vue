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
    @click:row="openPresentation"
  >
    <template v-slot:[`item.indicator`]="{item}">
      <new-message-icon
          :type="'presentation'"
          :id="item.id"
      ></new-message-icon>
    </template>
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
      <div v-if="item.state === 'request_received'">Details</div>
    </template>
  </v-data-table>
</template>

<script>
import { EventBus } from "../main";
import { CredentialTypes } from "../constants";
import { presentationListHeaders } from "@/components/tableHeaders/PresentationListHeaders";
import NewMessageIcon from "@/components/NewMessageIcon";

export default {
  components: {NewMessageIcon},
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
    isItemActive(item) {
      return this.isActiveFn(item);
    },
    openPresentation(item) {
      console.log("open details");
      console.log(item);
      if (item.id) {
        this.$router.push({
          path: `/app/presentation-request/${item.id}/details`,
          append: true,
        });
      } else {
        EventBus.$emit(
          "error",
          "No details view available for presentations in public profile."
        );
      }
    },
  },
};
</script>
