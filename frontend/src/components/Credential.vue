<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <h3 v-if="!showOnlyContent && (document.issuer || document.issuedAt)">
      Issuer
    </h3>
    <v-row v-if="!showOnlyContent">
      <v-col>
        <v-text-field
          v-if="document.issuer"
          label="Issuer"
          v-model="document.issuer"
          disabled
          outlined
          dense
        ></v-text-field>
        <v-text-field
          v-if="document.issuedAt"
          label="Issued at"
          :placeholder="
            $options.filters.moment(document.issuedAt, 'YYYY-MM-DD HH:mm')
          "
          disabled
          outlined
          dense
        ></v-text-field>
      </v-col>
    </v-row>

    <h3 v-if="document.credentialData && !showOnlyContent">
      Credential Content
    </h3>
    <v-row>
      <v-col>
        <v-text-field
          v-for="field in schema.fields"
          :key="field.type"
          :label="field.label"
          placeholder
          :disabled="isReadOnly"
          :rules="[(v) => !!v || 'Item is required']"
          :required="field.required"
          outlined
          dense
          :value="documentData[field.type]"
          @change="fieldChanged(field.type, $event)"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
</template>

<script>
import { getSchema } from "../constants";
export default {
  props: {
    isReadOnly: Boolean,
    document: Object,
    showOnlyContent: Boolean,
  },
  created() {
    console.log(this.document);
    // New created document
    if (
      !{}.hasOwnProperty.call(this.document, "documentData") &&
      !{}.hasOwnProperty.call(this.document, "credentialData") &&
      !{}.hasOwnProperty.call(this.document, "proofData")
    ) {
      this.documentData = Object.fromEntries(
        this.schema.fields.map((field) => {
          return [field.type, ""];
        })
      );
      // Existing document or credential
    } else {
      // Check if document or credential data is here. This needs to be improved
      let documentData;
      if (this.document.documentData) {
        documentData = this.document.documentData;
      } else if (this.document.credentialData) {
        documentData = this.document.credentialData;
      } else if (this.document.proofData) {
        documentData = this.document.proofData;
      }
      // Only support one nested node for now
      let nestedData = Object.values(documentData).find((value) => {
        return typeof value === "object" && value !== null;
      });
      documentData = nestedData ? nestedData : documentData;

      // Filter empty elements only for credentials
      if (!this.document.documentData) {
        this.documentData = Object.fromEntries(
          Object.entries(documentData).filter(([, value]) => {
            return value !== "";
          })
        );
      } else {
        this.documentData = documentData;
      }

      this.intDoc = { ...this.documentData };
    }
  },
  data: () => {
    return {
      intDoc: Object,
    };
  },
  computed: {
    schema: function () {
      let s = getSchema(this.document.type);

      if (s && {}.hasOwnProperty.call(s, "fields")) {
        return s;
      } else if (this.documentData) {
        // No known schema. Generate one from data
        // Todo: Support arrays and objects as fields
        s = {
          type: this.document.type,
          fields: Object.keys(this.documentData).map((key) => {
            return {
              type: key,
              label: key,
            };
          }),
        };
        console.log(s);
        return s;
      } else {
        console.log("I'm here");
        return this.$store.getters.getPreparedSchema(this.document.schemaId);
      }
    },
  },
  methods: {
    fieldChanged(fieldType, event) {
      if (this.intDoc[fieldType] != event) {
        this.documentData[fieldType] = event;
      } else {
        this.documentData[fieldType] = event;
      }

      const isDirty = Object.keys(this.intDoc).find((key) => {
        return this.documentData[key] != this.intDoc[key] ? true : false;
      })
        ? true
        : false;
      this.$emit("doc-changed", isDirty);
    },
  },
};
</script>
