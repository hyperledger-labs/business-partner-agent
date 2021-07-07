<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light"> Import Schema </v-card-title>
      <v-card-text>
        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            Schema Name:
          </v-list-item-title>
          <v-list-item-subtitle>
            <v-text-field
              class="mt-6"
              placeholder="Name"
              v-model="schema.label"
              :rules="[rules.required]"
              outlined
              dense
              required
            >
            </v-text-field>
          </v-list-item-subtitle>
        </v-list-item>
        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            Schema ID:
          </v-list-item-title>
          <v-list-item-subtitle>
            <v-text-field
              class="mt-6"
              placeholder="Schema ID"
              v-model="schema.schemaId"
              :rules="[rules.required]"
              outlined
              dense
              required
            >
            </v-text-field>
          </v-list-item-subtitle>
        </v-list-item>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()"
            >Cancel</v-bpa-button
          >
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
            :disabled="fieldsEmpty"
            >Submit</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import adminService from "@/services/adminService";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "AddSchema",
  components: { VBpaButton },
  props: {},
  data: () => {
    return {
      schema: {
        label: "",
        schemaId: "",
      },
      isBusy: false,
      rules: {
        required: (value) => !!value || "Can't be empty",
      },
    };
  },
  computed: {
    fieldsEmpty() {
      return (
        this.schema.label.length === 0 || this.schema.schemaId.length === 0
      );
    },
  },
  watch: {},
  methods: {
    async submit() {
      this.isBusy = true;
      adminService
        .addSchema(this.schema)
        .then((result) => {
          this.isBusy = false;
          if (result.status === 200) {
            EventBus.$emit("success", "Schema added successfully");
            this.$emit("success");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          if (e.response.status === 400) {
            EventBus.$emit("error", "Schema already exists");
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
