<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <br />
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
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
          :append-outer-icon="
            schemaJsonLd.schemaId.length > 0
              ? '$vuetify.icons.refresh'
              : undefined
          "
          @click:append-outer="getJsonLdAttributes"
        >
        </v-text-field>
      </v-list-item-subtitle>
    </v-list-item>
    <v-list-item>
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
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
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
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
      <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
        {{ $t("component.addSchema.schemaJsonLdAttributes") }}:
      </v-list-item-title>
    </v-list-item>
    <v-text-field
      v-model="searchField"
      append-icon="$vuetify.icons.search"
      :label="$t('app.search')"
      single-line
      hide-details
      clearable
    ></v-text-field>
    <v-data-table
      :items="tempJsonLdAttributes"
      :headers="headersJsonLdTable"
      :search="searchField"
      v-model="selectedJsonLdAttributes"
      item-key="name"
      show-select
    >
    </v-data-table>
    <v-card-actions>
      <v-layout align-end justify-end>
        <v-bpa-button color="secondary" @click="cancel"
          >{{ $t("button.cancel") }}
        </v-bpa-button>
        <v-bpa-button
          :disabled="fieldsEmptyJsonLd"
          :loading="isBusy"
          color="primary"
          @click="submitSchemaJsonLd"
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
import { jsonLdService } from "@/services";

export default {
  name: "import-credential-json-ld",
  components: { VBpaButton },
  data: () => {
    return {
      isBusy: false,
      searchField: "",
      schemaJsonLd: {
        label: "",
        schemaId: "",
        attributes: new Array<string>(),
        ldType: "",
        credentialType: "json-ld",
      },
      selectedJsonLdAttributes: new Array<{ name: string }>(),
      tempJsonLdAttributes: new Array<string>(),
    };
  },
  computed: {
    headersJsonLdTable() {
      return [
        {
          text: this.$t("component.addSchema.headerNameJsonLdAttributes"),
          value: "name",
        },
      ];
    },
    rules() {
      return {
        required: (value: string) => !!value || this.$t("app.rules.required"),
      };
    },
    fieldsEmptyJsonLd() {
      return (
        this.schemaJsonLd.label.length === 0 ||
        this.schemaJsonLd.schemaId.length === 0 ||
        this.schemaJsonLd.ldType.length === 0 ||
        this.selectedJsonLdAttributes.length === 0
      );
    },
  },
  methods: {
    async submitSchemaJsonLd() {
      this.isBusy = true;

      this.selectedJsonLdAttributes.map((selectedAttribute) => {
        this.schemaJsonLd.attributes.push(selectedAttribute.name);
      });

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
      this.schemaJsonLd.attributes = [];
      this.tempJsonLdAttributes = [];
      this.selectedJsonLdAttributes = [];

      jsonLdService
        .contextParser()
        .parse(this.schemaJsonLd.schemaId)
        .then((response) => {
          const regex = new RegExp(/^[a-z]/);
          const attributes = Object.keys(response.getContextRaw()).filter(
            (attribute) => {
              return regex.test(attribute);
            }
          );

          for (const attribute of attributes) {
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
