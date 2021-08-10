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
      <template v-slot:[`item.indicator`]="{item}">
        <new-message-icon
            :type="'credential'"
            :id="item.id"
        ></new-message-icon>
      </template>
      <template v-slot:[`item.state`]="{ item }">
        <v-icon v-if="isItemActive(item) && !item.revoked" color="green" title="credential issued"
          >$vuetify.icons.check</v-icon
        >
        <v-icon v-else-if="isItemActive(item) && item.revoked" title="credential revoked"
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
      <template v-slot:[`item.revocable`]="{ item }">
        <v-icon v-if="item.revocable && item.revoked" title="credential revoked"
        >$vuetify.icons.revoked</v-icon
        >
        <v-icon v-else-if="item.revocable" color="green" title="revoke credential"
                @click.stop="revokeCredential(item.id)" :disabled="revoked.includes(item.id)"
        >$vuetify.icons.revoke</v-icon
        >
        <span v-else>
        </span>
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
          <v-bpa-button color="primary" @click="dialog = false"
            >Close</v-bpa-button
          >
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<script>

import { issuerService } from "@/services";
import Cred from "@/components/Credential.vue";
import VBpaButton from "@/components/BpaButton";
import NewMessageIcon from "@/components/NewMessageIcon";

export default {
  props: {
    items: Array,
    headers: {
      type: Array,
      default: () => [
        {
          text: '',
          value: 'indicator',
          sortable: false,
          filterable: false
        },
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
        {
          text: "Revocation",
          value: "revocable",
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
      revoked: []
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
    revokeCredential(id) {
      this.revoked.push(id);
      issuerService.revokeCredential(id);
    }
  },
  components: {
    VBpaButton,
    Cred,
    NewMessageIcon
  },
};
</script>
