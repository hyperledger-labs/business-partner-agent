<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
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
        <v-bpa-button color="secondary" @click="cancel"
          >{{ $t("button.cancel") }}
        </v-bpa-button>
        <v-bpa-button
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
import { validateJson } from "@/utils/validateUtils";
import adminService from "@/services/adminService";
import { EventBus } from "@/main";
import { jsonLdService } from "@/services";

export default {
  name: "import-credential-json-ld",
  components: { VBpaButton },
  data: () => {
    return {
      isBusy: false,
      schemaJsonLd: {
        label: "",
        schemaId: "",
        attributes: ["email"],
        defaultAttributeName: "email",
        ldType: "",
      },
      tempJsonLdAttributes: [],
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
