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
        :hide-default-footer="itemsWithIndex.length < 10"
        :headers="headers"
        :items="itemsWithIndex"
        item-key="index"
        single-select
        :sort-by="['canIssue', 'isCreator', 'label']"
        :sort-desc="[true, true, false]"
        multi-sort
        @click:row="openItem"
    >
      <template v-slot:[`item.trustedIssuer`]="{ item }">
        <v-icon v-if="item.trustedIssuer && item.trustedIssuer.length">$vuetify.icons.check</v-icon>
      </template>
      <template v-slot:[`item.canIssue`]="{ item }">
        <v-icon v-if="item.canIssue">$vuetify.icons.check</v-icon>
      </template>
      <template v-slot:[`item.isCreator`]="{ item }">
        <v-icon v-if="item.isCreator">$vuetify.icons.check</v-icon>
      </template>

    </v-data-table>
    <v-dialog v-model="dialog" persistent max-width="600px">
        <ManageSchema
          :schema="schema"
          @closed="onClosed"
          @credDefAdded="onCredDefAdded"
          @schemaDeleted="onSchemaDeleted" />
    </v-dialog>
  </v-container>
</template>
<script>

  import ManageSchema from "@/components/ManageSchema";
  export default {
    props: {
      items: Array,
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
            value: "isCreator",
          },
          {
            text: "Trusted Issuers",
            value: "trustedIssuer",
          },
        ],
      },
      isLoading: Boolean
    },
    data: () => {
      return {
        dialog: false,
        schema: {},
      };
    },
    computed: {
      // Add an unique index, because elements do not have unique id
      itemsWithIndex: function () {
        return this.items.map((item, index) => ({
          ...item,
          canIssue: item.credentialDefinitions && item.credentialDefinitions.length,
          index: index + 1,
        }));
      },
    },
    methods: {
      openItem(item) {
        this.dialog = true;
        this.schema = item;
        this.$emit("openItem", item);
      },
      onClosed() {
        console.log('onClosed');
        this.dialog = false;
      },
      onCredDefAdded() {
        console.log('onCredDefAdded');
      },
      onSchemaDeleted() {
        console.log('onSchemaDeleted');
      }
    },
    components: {ManageSchema},
  };
</script>
