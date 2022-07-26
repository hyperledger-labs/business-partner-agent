<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <v-row
      v-if="
        (intDoc.label instanceof String || typeof intDoc.label === 'string') &&
        !showOnlyContent
      "
    >
      <v-col cols="12" class="pb-0 mt-4">
        <v-text-field
          :label="$t('component.credential.label')"
          outlined
          dense
          :value="intDoc.label"
          :disabled="showOnlyContent"
          @change="docFieldChanged('label', $event)"
        ></v-text-field>
      </v-col>
    </v-row>

    <h3
      v-if="!showOnlyContent && (intDoc.issuer || intDoc.issuedAt)"
      class="mb-4"
    >
      {{ $t("component.credential.issuerTitle") }}
    </h3>

    <v-row v-if="!showOnlyContent">
      <v-col>
        <v-text-field
          v-if="intDoc.issuer"
          :label="$t('component.credential.issuerLabel')"
          v-model="intDoc.issuer"
          disabled
          outlined
          dense
        ></v-text-field>
        <v-text-field
          v-if="intDoc.issuedAt"
          :label="$t('component.credential.issuedAt')"
          :value="dateTime(intDoc.issuedAt, 'YYYY-MM-DD HH:mm')"
          disabled
          outlined
          dense
        ></v-text-field>
      </v-col>
    </v-row>

    <h3 v-if="intDoc.credentialData && !showOnlyContent" class="mb-4">
      {{ $t("component.issueCredential.attributesTitle") }}
    </h3>
    <v-row>
      <v-col>
        <v-text-field
          v-for="field in schema.fields"
          :key="field.type"
          :label="field.label"
          :disabled="isReadOnly"
          :rules="[(v) => !!v || $t('app.rules.required')]"
          :required="field.required"
          outlined
          dense
          :value="intDoc[documentDataType][field.type]"
          @change="docDataFieldChanged(field.type, $event)"
        ></v-text-field>
      </v-col>
    </v-row>
  </div>
</template>

<script lang="ts">
import * as textUtils from "@/utils/textUtils";
import { MyDocumentAPI, SchemaAPI } from "@/services";
import moment from "moment";

export default {
  props: {
    isReadOnly: {
      type: Boolean,
      default: false,
    },
    document: {} as MyDocumentAPI,
    showOnlyContent: Boolean,
    isNew: Boolean,
  },
  watch: {
    document(value: MyDocumentAPI) {
      // document has been updated...
      if (value) {
        this.prepareDocument();
      }
    },
  },
  created() {
    console.log("Credential:", this.document);
    this.prepareDocument();
  },
  data: () => {
    return {
      intDoc: {
        type: Object,
        default: {},
      },
      origIntDoc: Object,
      documentDataTypes: ["documentData", "credentialData", "proofData"],
      documentDataType: "",
    };
  },
  computed: {
    schema: function (): {
      type: string;
      label: string;
      fields: { type: string; label: string };
    } {
      let schemaTemplate = this.createTemplateFromSchemaId(
        this.document.schemaId
      );
      if (!schemaTemplate) {
        schemaTemplate = this.createTemplateFromSchema(this.document);
      }
      return schemaTemplate;
    },
  },
  methods: {
    docDataFieldChanged(propertyName: string, eventField: string) {
      if (this.origIntDoc[this.documentDataType][propertyName] !== eventField) {
        this.intDoc[this.documentDataType][propertyName] = eventField;
      } else {
        this.intDoc[this.documentDataType][propertyName] = eventField;
      }

      const isDirty = !!Object.keys(
        this.origIntDoc[this.documentDataType]
      ).some((key) => {
        return (
          this.intDoc[this.documentDataType][key] !==
          this.origIntDoc[this.documentDataType][key]
        );
      });
      this.$emit("doc-data-field-changed", isDirty);
    },

    createTemplateFromSchemaId(schemaId: string) {
      // as a holder, i may not know the schema - schemaID is set only if we have stored the schema
      // return null if we do not have a schema id.
      if (!schemaId) return;
      const schemas = this.$store.getters.getSchemas;
      let schema = schemas.find((s: SchemaAPI) => {
        return s.schemaId === schemaId;
      });
      if (schema) {
        //TODO check if fields already available
        return Object.assign(schema, {
          fields: schema.schemaAttributeNames.map((key: string) => {
            return {
              type: key,
              label: textUtils.schemaAttributeLabel(key),
            };
          }),
        });
      }
    },

    createTemplateFromSchema(objectData: MyDocumentAPI) {
      const documentDataTypes = ["documentData", "credentialData", "proofData"];
      const dataType: string = documentDataTypes.find((value) => {
        if (
          objectData &&
          Object.prototype.hasOwnProperty.call(objectData, value)
        ) {
          return value;
        }
      });
      return {
        type: objectData.type,
        label: "",
        fields: Object.keys(
          dataType ? objectData[dataType as keyof MyDocumentAPI] : objectData
        ).map((key) => {
          return {
            type: key,
            label: textUtils.schemaAttributeLabel(key),
          };
        }),
      };
    },

    docFieldChanged(propertyName: string, eventLabel: string) {
      this.intDoc[propertyName] = eventLabel;
      this.$emit("doc-field-changed", { key: propertyName, value: eventLabel });
    },

    prepareDocument() {
      //New Document
      if (!this.document.id && !this.document.credentialData) {
        this.documentDataType = this.documentDataTypes[0];
        this.intDoc.label = "";
        this.intDoc[this.documentDataType] = Object.fromEntries(
          this.schema.fields.map((field: { type: string; label: string }) => {
            return [field.type, ""];
          })
        );
        this.intCopy();
      }
      //Existing Document, extract Data
      else {
        for (const field of this.documentDataTypes) {
          if (Object.prototype.hasOwnProperty.call(this.document, field)) {
            this.documentDataType = field;
            this.intDoc = this.document;
            this.intCopy();
          }
        }
      }
    },

    intCopy() {
      this.origIntDoc = { ...this.intDoc };
      //create deep copy of objects
      Object.entries(this.intDoc).find(([key, value]) => {
        if (typeof value === "object" && value !== null) {
          this.origIntDoc[key] = { ...this.intDoc[key] };
        }
      });
    },
    dateTime(date: number) {
      return moment(date).format("YYYY-MM-DD");
    },
  },
};
</script>
