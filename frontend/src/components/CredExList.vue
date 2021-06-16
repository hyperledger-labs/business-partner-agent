<!--
 Copyright (c) 2021 - for information on the respective copyright owner
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
      <template v-slot:[`item.state`]="{ item }">
        <v-icon v-if="isItemActive(item)" color="green"
          >$vuetify.icons.check</v-icon
        >
        <span v-else>
          {{ item.state.replace("_", " ") }}
        </span>
      </template>
      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>
      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>
    </v-data-table>
    <v-dialog v-model="dialog" max-width="600px">
      <v-card>
        <v-card-title class="bg-light">
          <span class="headline">Credential Data</span>
        </v-card-title>
        <v-card-text>
          <Cred :document="document" isReadOnly showOnlyContent></Cred>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" @click="dialog = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<script>
import Cred from "@/components/Credential.vue";

export default {
  props: {
    items: Array,
    headers: {
      type: Array,
      default: () => [
        {
          text: "Type",
          value: "displayText",
        },
        {
          text: "Issued To",
          value: "partner.alias",
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
    },
    isActiveFn: {
      type: Function,
      default: (item) =>
        item.state === "credential_issued" || item.state == "credential_acked",
    },
    isLoading: Boolean,
  },
  data: () => {
    return {
      dialog: false,
      document: {},
    };
  },
  watch: {
    items(val) {
      console.log("Credential Exchange Item refresh");
      console.log(val);
    },
  },
  computed: {},
  methods: {
    openItem(item) {
      this.dialog = true;
      this.document = {
        credentialData: { ...item.credential.attrs },
        schemaId: item.credential.schemaId,
        credentialDefinitionId: item.credential.credentialDefinitionId,
      };
      this.$emit("openItem", item);
    },
    isItemActive(item) {
      return this.isActiveFn(item);
    },
  },
  components: {
    Cred,
  },
};
</script>
