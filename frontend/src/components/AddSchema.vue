<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        {{ $t("component.addSchema.title") }}
      </v-card-title>
      <v-card-text>
        <v-list-item>
          <v-list-item-title
            class="grey--text text--darken-2 font-weight-medium"
          >
            {{ $t("component.addSchema.schemaName") }}:
          </v-list-item-title>
          <v-list-item-subtitle>
            <v-text-field
              class="mt-6"
              :placeholder="$t('component.addSchema.placeholderName')"
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
            {{ $t("component.addSchema.schemaId") }}:
          </v-list-item-title>
          <v-list-item-subtitle>
            <v-text-field
              class="mt-6"
              :placeholder="$t('component.addSchema.placeholderId')"
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
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
            :disabled="fieldsEmpty"
            >{{ $t("button.submit") }}</v-bpa-button
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
    };
  },
  computed: {
    rules() {
      return {
        required: (value) => !!value || this.$t("app.rules.required"),
      };
    },
    fieldsEmpty() {
      return (
        this.schema.label.length === 0 || this.schema.schemaId.length === 0
      );
    },
  },
  methods: {
    async submit() {
      this.isBusy = true;
      adminService
        .addSchema(this.schema)
        .then((result) => {
          this.isBusy = false;
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("component.addSchema.eventSuccess")
            );
            this.$emit("success");
          }
        })
        .catch((e) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(e));
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
