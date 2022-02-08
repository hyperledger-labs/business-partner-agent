<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("component.addSchema.schemaName") }}:
      </v-list-item-title>
      <v-list-item-subtitle>
        <v-text-field
          class="mt-6"
          :placeholder="$t('component.addSchema.placeholderName')"
          v-model="schemaIndy.label"
          :rules="[rules.required]"
          outlined
          dense
          required
        >
        </v-text-field>
      </v-list-item-subtitle>
    </v-list-item>
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("component.addSchema.schemaId") }}:
      </v-list-item-title>
      <v-list-item-subtitle>
        <v-text-field
          class="mt-6"
          :placeholder="$t('component.addSchema.placeholderId')"
          v-model="schemaIndy.schemaId"
          :rules="[rules.required]"
          outlined
          dense
          required
        >
        </v-text-field>
      </v-list-item-subtitle>
    </v-list-item>
    <v-card-actions>
      <v-layout align-end justify-end>
        <v-bpa-button color="secondary" @click="cancel"
          >{{ $t("button.cancel") }}
        </v-bpa-button>
        <v-bpa-button
          :loading="isBusy"
          color="primary"
          @click="submitSchemaIndy"
          :disabled="fieldsEmptyIndy"
          >{{ $t("button.submit") }}
        </v-bpa-button>
      </v-layout>
    </v-card-actions>
  </v-container>
</template>
<script lang="ts">
import VBpaButton from "@/components/BpaButton";
import adminService from "@/services/adminService";
import { EventBus } from "@/main";

export default {
  name: "import-credential-indy",
  components: { VBpaButton },
  data: () => {
    return {
      isBusy: false,
      schemaIndy: {
        label: "",
        schemaId: "",
      },
    };
  },
  computed: {
    rules() {
      return {
        required: (value: string) => !!value || this.$t("app.rules.required"),
      };
    },
    fieldsEmptyIndy() {
      return (
        this.schemaIndy.label.length === 0 ||
        this.schemaIndy.schemaId.length === 0
      );
    },
  },
  methods: {
    async submitSchemaIndy() {
      this.isBusy = true;
      adminService
        .addSchema(this.schemaIndy)
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
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
