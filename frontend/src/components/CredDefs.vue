<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <h4 class="my-4 grey--text text--darken-3">Credential Definitions</h4>

    <v-row v-for="(entry, index) in items" v-bind:key="index">
      <v-col cols="4" class="py-0">
        <v-text-field
          label="Tag"
          :disabled="entry.id"
          v-model="entry.tag"
          outlined
          dense
        ></v-text-field>
      </v-col>
      <v-col class="py-0">
        <v-checkbox
            label="Support Revocation"
            :disabled="entry.id"
            v-model="entry.supportRevocation"
            outlined
            dense
        >
        </v-checkbox>
      </v-col>
      <v-col class="py-0">
        <v-text-field
            label="Revocation Registry Size"
            placeholder="Revocation Registry Size (integer)"
            :disabled="entry.id"
            v-model="entry.revocationRegistrySize"
            outlined
            dense
        >
        </v-text-field>
      </v-col>
      <v-col cols="3" class="py-0">
        <v-btn
          v-if="!entry.id"
          :loading="isBusy"
          color="primary"
          text
          @click="saveItem(entry)"
          >Save</v-btn
        >

        <v-btn
          v-if="!entry.id && this.isAdd"
          color="secondary"
          text
          @click="cancelAdd(index)"
          >Cancel</v-btn
        >

      </v-col>
    </v-row>
    <v-btn :disabled="isAdd" color="primary" text @click="addItem"
      >Add Item</v-btn
    >
  </div>
</template>

<script>
import { EventBus } from "@/main";
import { issuerService } from '@/services';
export default {
  props: {
    id: String, //schema ID
    items: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  created() {},
  mounted() {},

  data: () => {
    return {
      isAdd: false,
      editingTrustedIssuer: null,
      isBusy: false
    };
  },
  computed: {},
  methods: {
    addItem() {
      this.isAdd = true;
      this.items.push({
        id: undefined,
        tag: "default",
        supportRevocation: false,
        revocationRegistrySize: 4
      });
    },
    cancelAdd(index) {
      this.isAdd = false;
      this.items.splice(index, 1);
    },
    async saveItem(item) {
      this.isBusy = true;
      try {
        const data = {
          schemaId: this.id,
          tag: item.tag,
          supportRevocation: item.supportRevocation,
          revocationRegistrySize: item.revocationRegistrySize
        };

        const resp = await issuerService.createCredDef(data);
        if (resp.status === 200) {
          EventBus.$emit("success", "New credential definition added");
          item.id = resp.data.id;
        }
        this.isBusy = false;
      } catch(error) {
        EventBus.$emit("error", error);
      }
    },
  },
};
</script>
