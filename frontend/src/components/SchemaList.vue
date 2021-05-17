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
      :hide-default-footer="schemas.length < 10"
      :headers="headers"
      :items="schemas"
      single-select
      :sort-by="['canIssue', 'isMine', 'label']"
      :sort-desc="[true, true, false]"
      multi-sort
      @click:row="openItem"
    >
      <template v-slot:[`item.trustedIssuer`]="{ item }">
        <v-icon
          v-if="Array.isArray(item.trustedIssuer) && item.trustedIssuer.length"
          >$vuetify.icons.check</v-icon
        >
      </template>
      <template v-slot:[`item.canIssue`]="{ item }">
        <v-icon v-if="item.canIssue">$vuetify.icons.check</v-icon>
      </template>
      <template v-slot:[`item.isMine`]="{ item }">
        <v-icon v-if="item.isMine">$vuetify.icons.check</v-icon>
      </template>
    </v-data-table>
    <v-dialog v-model="dialog" persistent max-width="800px">
      <ManageSchema
        :dialog="dialog"
        :schema="schema"
        :credential-definitions="manageCredentialDefinitions"
        :trusted-issuers="manageTrustedIssuers"
        @closed="onClosed"
        @changed="onChanged"
        @deleted="onDeleted"
      />
    </v-dialog>
  </v-container>
</template>
<script>
import ManageSchema from "@/components/ManageSchema";
import store from "@/store";
export default {
  props: {
    headers: {
      type: Array,
      default: () => [
        {
          text: "Name",
          value: "label",
        },
        {
          text: "Can Issue",
          value: "canIssue",
        },
        {
          text: "Mine",
          value: "isMine",
        },
        {
          text: "Trusted Issuers",
          value: "trustedIssuer",
        },
      ],
    },
    isLoading: Boolean,
    manageTrustedIssuers: {
      type: Boolean,
      default: () => true,
    },
    manageCredentialDefinitions: {
      type: Boolean,
      default: () => true,
    },
  },
  data: () => {
    return {
      dialog: false,
      schema: {},
      dirty: false,
    };
  },
  computed: {
    schemas: {
      get() {
        return this.$store.getters.getSchemaBasedSchemas;
      },
    },
  },
  methods: {
    openItem(item) {
      this.dialog = true;
      this.dirty = false;
      this.schema = item;
    },
    onClosed() {
      this.dialog = false;
      if (this.dirty) {
        store.dispatch("loadSchemas");
        this.$emit("changed");
      }
      this.dirty = false;
    },
    onDeleted() {
      this.dialog = false;
    },
    onChanged() {
      // something changed related to this schema, will need to reload the schema list
      this.dirty = true;
    },
  },
  components: { ManageSchema },
};
</script>
