<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

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
      @click:row="openItem"
    >
      <template v-slot:[`item.trustedIssuer`]="{ item }">
        <v-icon
          v-if="
            Array.isArray(item.trustedIssuer) && item.trustedIssuer.length > 0
          "
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
        v-model="schema"
        :credential-definitions="manageCredentialDefinitions"
        :trusted-issuers="manageTrustedIssuers"
        @closed="onClosed"
        @changed="onChanged"
        @deleted="onDeleted"
      />
    </v-dialog>
  </v-container>
</template>
<script lang="ts">
import ManageSchema from "@/components/ManageSchema.vue";
import store from "@/store";
export default {
  props: {
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
    headers() {
      return [
        {
          text: this.$t("component.schemaList.label"),
          value: "label",
        },
        {
          text: this.$t("component.schemaList.canIssue"),
          value: "canIssue",
        },
        {
          text: this.$t("component.schemaList.isMine"),
          value: "isMine",
        },
        {
          text: this.$t("component.schemaList.trustedIssuer"),
          value: "trustedIssuer",
        },
      ];
    },
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
      if (!this.schema.credentialDefinitions) {
        this.schema.credentialDefinitions = [];
      }
      if (!this.schema.trustedIssuer) {
        this.schema.trustedIssuer = [];
      }
    },
    onClosed() {
      this.dialog = false;
      if (this.dirty) {
        store.dispatch("loadSchemas");
        store.dispatch("loadCredDefSelectList");
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
