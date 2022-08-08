<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-row v-bind:key="index" v-for="(entry, index) in items">
      <v-col cols="4" class="py-0">
        <v-text-field
          :label="$t('component.credentialDefinitions.label')"
          :disabled="!entry.isEdit"
          v-model="entry.tag"
          :rules="[(v) => !!v || $t('app.rules.required')]"
          outlined
          dense
        ></v-text-field>
      </v-col>
      <v-col class="py-0">
        <v-checkbox
          class="mt-1"
          :label="$t('component.credentialDefinitions.labelCheckbox')"
          v-model="entry.supportRevocation"
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
          :disabled="entry.tag && entry.tag.trim().length === 0"
          icon
          @click="saveItem(entry)"
        >
          <v-icon color="primary">$vuetify.icons.save</v-icon>
        </v-btn>
        <v-btn icon v-if="!entry.isEdit" @click="deleteItem(index)">
          <v-icon color="error">$vuetify.icons.delete</v-icon>
        </v-btn>
        <v-btn
          icon
          v-if="!entry.id && entry.isEdit"
          @click="cancelSaveItem(index)"
        >
          <v-icon color="error">$vuetify.icons.cancel</v-icon>
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-bpa-button :disabled="isEdit" color="secondary" @click="addItem">{{
        $t("component.credentialDefinitions.buttonAddCredDef")
      }}</v-bpa-button>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import issuerService from "@/services/issuer-service";
import VBpaButton from "@/components/BpaButton";
import { CreateCredDefRequest, CredDef } from "@/services";

export default {
  components: { VBpaButton },
  props: {
    schema: {
      type: Object,
    },
    credentialDefinitions: {
      type: Array,
      default: function (): CredDef[] {
        return [];
      },
    },
    reset: {
      type: Boolean,
      default: () => false,
    },
  },
  watch: {
    schema() {
      this.isEdit = false;
      this.editingItem = false;
    },
    credentialDefinitions(value: CredDef[]) {
      this.items = [...value];
      this.isEdit = false;
      this.editingItem = false;
    },
    reset(newValue: boolean, oldValue: boolean) {
      // use this to reset the form, remove any outstanding items that are not saved.
      if (newValue !== oldValue) {
        this.items = [...this.credentialDefinitions];
        this.isEdit = false;
        this.editingItem = false;
      }
    },
  },
  mounted() {
    this.items = [...this.credentialDefinitions];
  },
  data: () => {
    return {
      items: new Array<CredDef & { isEdit: boolean }>(),
      isEdit: false,
      editingItem: undefined as boolean,
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
    deleteItem(index: number) {
      let item = this.items[index];
      if (item.id) {
        issuerService
          .deleteCredDef(item.id)
          .then((result) => {
            console.log(result);
            this.items.splice(index, 1);
            this.$emit("changed");
          })
          .catch((error) => {
            this.emitter.emit("error", this.$axiosErrorMessage(error));
          });
      } else {
        this.items.splice(index, 1);
      }
    },
    saveItem(item: CredDef & { isEdit: boolean }) {
      this.createNewItem(item);
    },
    createNewItem(item: CredDef & { isEdit: boolean }) {
      this.isBusy = true;
      const data: CreateCredDefRequest = {
        schemaId: this.schema.schemaId,
        tag: item.tag,
        supportRevocation: item.supportRevocation,
      };
      issuerService
        .createCredDef(data)
        .then((result) => {
          this.isBusy = false;

          if (result.status === 200) {
            this.isEdit = false;
            item.isEdit = false;
            this.emitter.emit(
              "success",
              this.$t("component.credentialDefinitions.eventSuccess")
            );
            this.$emit("changed");
            this.$store.dispatch("loadCredDefSelectList");
          }
        })
        .catch((error) => {
          this.isBusy = false;
          this.emitter.emit("error", this.$axiosErrorMessage(error));
        });
    },

    cancelSaveItem(index: number) {
      this.isEdit = false;
      this.editingItem = false;
      this.items.splice(index, 1);
    },
  },
};
</script>
