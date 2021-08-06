<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-row v-for="(entry, index) in items" v-bind:key="index">
      <v-col cols="4" class="py-0">
        <v-text-field
          label="Tag"
          :disabled="!entry.isEdit"
          v-model="entry.tag"
          outlined
          dense
        ></v-text-field>
      </v-col>
      <v-col class="py-0">
        <v-checkbox
          class="mt-1"
          label="Revocable"
          v-model="entry.isSupportRevocation"
          :disabled="!isTailsConfigured || !entry.isEdit"
          outlined
          dense
        >
        </v-checkbox>
      </v-col>
      <v-col cols="3" class="py-0">
        <v-btn
          v-if="!entry.id && entry.isEdit"
          :loading="isBusy"
          icon
          @click="saveItem(entry)"
        >
          <v-icon color="primary">$vuetify.icons.save</v-icon>
        </v-btn>
        <v-btn icon v-if="!entry.isEdit" @click="deleteItem(index)">
          <v-icon color="error">$vuetify.icons.delete</v-icon>
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-bpa-button :disabled="isEdit" color="secondary" @click="addItem"
        >Add Credential Definition</v-bpa-button
      >
    </v-row>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import issuerService from "@/services/issuerService";
import VBpaButton from "@/components/BpaButton";

export default {
  components: { VBpaButton },
  props: {
    schema: {
      type: Object,
      default: () => {},
    },
    credentialDefinitions: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  watch: {
    schema(val) {
      console.log("Credential definitions schema refresh");
      console.log(val);
      this.isEdit = false;
      this.editingItem = false;
    },
    credentialDefinitions(val) {
      console.log("Credential definitions list refresh");
      console.log(val);
      this.items = Array.from(val);
      this.isEdit = false;
      this.editingItem = false;
    },
  },
  created() {},
  mounted() {
    this.items = Array.from(this.credentialDefinitions);
  },
  data: () => {
    return {
      items: [],
      isEdit: false,
      editingItem: null,
      isBusy: false,
    };
  },
  computed: {
    isTailsConfigured() {
      return this.$config.tailsServerConfigured;
    },
  },
  methods: {
    addItem() {
      this.isEdit = true;
      this.items.push({
        schemaId: this.id,
        tag: "",
        supportRevocation: false,
        isEdit: true,
      });
    },

    deleteItem(index) {
      let item = this.items[index];
      if (item.id) {
        issuerService
          .deleteCredDef(item.id)
          .then((result) => {
            console.log(result);
            this.items.splice(index, 1);
            this.$emit("changed");
          })
          .catch((e) => {
            console.error(e);
            EventBus.$emit("error", e);
          });
      } else {
        this.items.splice(index, 1);
      }
    },

    saveItem(item) {
      this.createNewItem(item);
    },

    createNewItem(item) {
      this.isBusy = true;
      const data = {
        schemaId: this.schema.schemaId,
        tag: item.tag,
        supportRevocation: item.supportRevocation,
      };
      issuerService
        .createCredDef(data)
        .then((result) => {
          console.log(result);
          this.isBusy = false;

          if (result.status === 200) {
            this.isEdit = false;
            item.isEdit = false;
            EventBus.$emit("success", "New credential definition added");
            this.$emit("changed");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
  },
};
</script>
