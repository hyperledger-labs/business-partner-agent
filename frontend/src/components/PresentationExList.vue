<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :loading="isLoading"
      :hide-default-footer="items.length < 10"
      :headers="headers"
      :items="items"
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
      single-select
      @click:row="openItem"
    >
      <template v-slot:[`item.indicator`]="{ item }">
        <new-message-icon
          :type="'presentation'"
          :id="item.id"
        ></new-message-icon>
      </template>
      <template v-slot:[`item.label`]="{ item }">
        {{
          item.proofRequest && item.proofRequest.name
            ? item.proofRequest.name
            : item.typeLabel
        }}
      </template>
      <template v-slot:[`item.state`]="{ item }">
        <span>
          {{ item.state ? item.state.replace("_", " ") : "" }}
        </span>
        <v-icon v-if="isComplete(item)" color="green">mdi-check</v-icon>
        <v-tooltip v-if="item.problemReport" top>
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
          <span>{{ item.problemReport }}</span>
        </v-tooltip>
      </template>
      <template v-slot:[`item.updatedAt`]="{ item }">
        {{
          (item.updatedAt || item.sentAt || item.receivedAt) | formatDateLong
        }}
      </template>
    </v-data-table>
    <v-dialog v-model="dialog" max-width="800px">
      <v-card>
        <v-card-title class="bg-light">
          <span class="headline">Presentation Exchange</span>
          <v-layout justify-end>
            <v-btn depressed color="red" icon @click="deleteItem">
              <v-icon dark>$vuetify.icons.delete</v-icon>
            </v-btn>
          </v-layout>
        </v-card-title>
        <v-card-text>
          <PresentationRecord :record="record"></PresentationRecord>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-bpa-button color="secondary" @click="closeItem(record)"
            >Close</v-bpa-button
          >
          <span v-if="record.state === 'request_received'">
            <v-bpa-button color="secondary" @click="decline"
              >Decline</v-bpa-button
            >
            <v-bpa-button
              :loading="this.isBusy"
              color="primary"
              @click="approve"
              >Accept</v-bpa-button
            >
          </span>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import { proofExService } from "@/services";
// import { EventBus } from "../main";
import NewMessageIcon from "@/components/NewMessageIcon";
import PresentationRecord from "@/components/PresentationRecord";
import VBpaButton from "@/components/BpaButton";
export default {
  props: {
    items: Array,
  },
  data: () => {
    return {
      selected: [],
      record: {},
      dialog: false,
      isBusy: false,
      isLoading: false,
      headers: [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: "Name",
          value: "label",
        },
        {
          text: "Role",
          value: "role",
        },
        {
          text: "Updated At",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
      ],
    };
  },
  computed: {},
  methods: {
    approve() {
      proofExService.approveProofRequest(this.record.id);
      this.dialog = false;
    },
    decline() {
      proofExService.declineProofRequest(this.record.id);
      this.dialog = false;
    },
    openItem(item) {
      this.record = item;
      this.dialog = true;
      this.$emit("openItem", item);
    },
    closeItem() {
      this.dialog = false;
    },
    async deleteItem() {
      const resp = await proofExService.deleteProofExRecord(this.record.id);
      if (resp.status === 200) {
        const idx = this.items.findIndex((item) => item.id === this.record.id);
        this.items.splice(idx, 1);
      }
      this.dialog = false;
    },
    isComplete(item) {
      // TOD: implement
      item;
      return false;
    },
  },
  components: {
    NewMessageIcon,
    PresentationRecord,
    VBpaButton,
  },
};
</script>
