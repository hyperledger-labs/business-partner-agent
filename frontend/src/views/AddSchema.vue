<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card max-width="600" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ $t("view.addSchema.title") }}</span>
      </v-card-title>

      <v-list-item>
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ $t("view.addSchema.schemaName") }}:
        </v-list-item-title>
        <v-list-item-subtitle>
          <v-text-field
            class="mt-6"
            :placeholder="$t('view.addSchema.placeholderName')"
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
        <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
          {{ $t("view.addSchema.schemaId") }}:
        </v-list-item-title>
        <v-list-item-subtitle>
          <v-text-field
            class="mt-6"
            :placeholder="$t('view.addSchema.placeholderSchemaId')"
            v-model="schema.schemaId"
            :rules="[rules.required]"
            outlined
            dense
            required
          >
          </v-text-field>
        </v-list-item-subtitle>
      </v-list-item>

      <v-card-text>
        <trusted-issuer ref="trustedIssuers" />
      </v-card-text>

      <v-card-actions>
        <v-layout justify-end>
          <v-bpa-button
            :loading="this.isBusyAddSchema"
            :disabled="fieldsEmpty"
            color="primary"
            @click="addSchema"
          >
            {{ $t("button.submit") }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import TrustedIssuer from "../components/TrustedIssuers.vue";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "AddSchema",
  components: {
    VBpaButton,
    TrustedIssuer,
  },
  data: () => {
    return {
      schema: {
        label: "",
        schemaId: "",
      },
      isBusyAddSchema: false,
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
    addSchema() {
      this.isBusyAddSchema = true;
      let trustedIssuers = this.$refs.trustedIssuers.$props.trustedIssuers;
      console.log(trustedIssuers);
      if (trustedIssuers.length > 0) {
        for (const entry of trustedIssuers) {
          delete entry.isEdit;
        }
        this.schema.trustedIssuer = trustedIssuers;
      }
      console.log(this.schema);

      this.$axios
        .post(`${this.$apiBaseUrl}/admin/schema`, this.schema)
        .then((result) => {
          console.log(result);
          this.isBusyAddSchema = false;

          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.addSchema.eventSuccessSchemaAdd")
            );
            this.$router.push({ name: "SchemaSettings" });
          }
        })
        .catch((error) => {
          this.isBusyAddSchema = false;
          if (error.response.status === 400) {
            EventBus.$emit(
              "error",
              this.$t("view.addSchema.eventErrorSchemaExists")
            );
          } else {
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          }
        });
    },
  },
};
</script>
