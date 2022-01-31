<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        {{ $t("component.addSchema.title") }}
      </v-card-title>

      <credential-type-tabs :is-busy="isBusy">
        <template v-slot:indy>
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
            <v-list-item-title
              class="grey--text text--darken-2 font-weight-medium"
            >
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
              <v-bpa-button color="secondary" @click="cancel()">{{
                $t("button.cancel")
              }}</v-bpa-button>
              <v-bpa-button
                :loading="isBusy"
                color="primary"
                @click="submitSchemaIndy()"
                :disabled="fieldsEmptyIndy"
                >{{ $t("button.submit") }}</v-bpa-button
              >
            </v-layout>
          </v-card-actions>
        </template>
        <template v-slot:json-ld>
          <br />
          <v-textarea
            rows="5"
            outlined
            dense
            clearable
            :loading="false"
            :rules="[rules.validJson]"
            :label="$t('component.addSchema.placeholderJsonLd')"
          ></v-textarea>
          <v-list-item>
            <v-list-item-title
              class="grey--text text--darken-2 font-weight-medium"
            >
              {{ $t("component.addSchema.schemaId") }}:
            </v-list-item-title>
            <v-list-item-subtitle>
              <v-text-field
                class="mt-6"
                :placeholder="$t('component.addSchema.placeholderJsonLdId')"
                v-model="schemaJsonLd.schemaId"
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
              {{ $t("component.addSchema.schemaName") }}:
            </v-list-item-title>
            <v-list-item-subtitle>
              <v-text-field
                class="mt-6"
                :placeholder="$t('component.addSchema.placeholderName')"
                v-model="schemaJsonLd.label"
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
              {{ $t("component.addSchema.schemaJsonLdType") }}:
            </v-list-item-title>
            <v-list-item-subtitle>
              <v-text-field
                class="mt-6"
                :placeholder="$t('component.addSchema.placeholderJsonLdType')"
                v-model="schemaJsonLd.ldType"
                :rules="[rules.required]"
                outlined
                dense
                required
              >
              </v-text-field>
            </v-list-item-subtitle>
          </v-list-item>
          <v-list-item>
            <v-data-table
              :items="tempJsonLdAttributes"
              :headers="headersJsonLdTable"
              v-model="schemaJsonLd.attributes"
              show-select
            >
            </v-data-table>
          </v-list-item>
          <v-card-actions>
            <v-layout align-end justify-end>
              <v-bpa-button color="secondary" @click="cancel()">{{
                $t("button.cancel")
              }}</v-bpa-button>
              <v-bpa-button
                :loading="isBusy"
                color="primary"
                @click="submitSchemaJsonLd"
                >{{ $t("button.submit") }}</v-bpa-button
              >
            </v-layout>
          </v-card-actions>
        </template>
      </credential-type-tabs>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import adminService from "@/services/adminService";
import VBpaButton from "@/components/BpaButton";
import CredentialTypeTabs from "@/components/schema-add/CredentialTypeTabs.vue";
import { validateJson } from "@/utils/validateUtils";
import { jsonLdService } from "@/services";

export default {
  name: "SchemaAdd",
  components: { CredentialTypeTabs, VBpaButton },
  props: {},
  data: () => {
    return {
      schemaIndy: {
        label: "",
        schemaId: "",
      },
      schemaJsonLd: {
        label: "",
        schemaId: "",
        attributes: ["email"],
        defaultAttributeName: "email",
        ldType: "",
      },
      tempJsonLdAttributes: [],
      isBusy: false,
    };
  },
  computed: {
    headersJsonLdTable() {
      return [
        {
          text: "Name",
          value: "name",
        },
      ];
    },
    rules() {
      return {
        required: (value) => !!value || this.$t("app.rules.required"),
        validJson: (value) =>
          validateJson(value) || this.$t("app.rules.validJson"),
      };
    },
    fieldsEmptyIndy() {
      return (
        this.schemaIndy.label.length === 0 ||
        this.schemaIndy.schemaId.length === 0
      );
    },
    fieldsEmptyJsonLd() {
      return (
        this.schemaJsonLd.label.length === 0 ||
        this.schemaJsonLd.schemaId.length === 0 ||
        this.schemaJsonLd.ldType.length === 0 ||
        this.schemaJsonLd.attributes.size === 0
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
    async submitSchemaJsonLd() {
      this.isBusy = true;

      adminService
        .addSchema(this.schemaJsonLd)
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
    async getJsonLdAttributes() {
      this.isBusy = true;
      jsonLdService
        .contextParser()
        .parse(this.schemaJsonLd.schemaId)
        .then((response) => {
          for (const attribute of Object.keys(response.getContextRaw())) {
            this.tempJsonLdAttributes.push({
              name: attribute,
            });
          }
        })
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", error.message);
        })
        .finally(() => {
          this.isBusy = false;
        });
    },
    cancel() {
      this.$emit("cancelled");
    },
  },
};
</script>
