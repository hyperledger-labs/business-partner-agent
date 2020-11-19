<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <div>
    <v-row>
      <v-col cols="12" class="pb-0">
        <v-text-field
          v-if="
            intDoc.label instanceof String || typeof intDoc.label === 'string'
          "
          label="Label (Optional)"
          placeholder
          outlined
          dense
          :value="intDoc.label"
          :disabled="showOnlyContent"
          @change="docFieldChanged('label', $event)"
        ></v-text-field>
      </v-col>
    </v-row>
    <h3 v-if="!showOnlyContent && (intDoc.issuer || intDoc.issuedAt)">
      Issuer
    </h3>
    <v-row v-if="!showOnlyContent">
      <v-col>
        <v-text-field
          v-if="intDoc.issuer"
          label="Issuer"
          v-model="intDoc.issuer"
          disabled
          outlined
          dense
        ></v-text-field>
        <v-text-field
          v-if="intDoc.issuedAt"
          label="Issued at"
          :placeholder="
            $options.filters.moment(intDoc.issuedAt, 'YYYY-MM-DD HH:mm')
          "
          disabled
          outlined
          dense
        ></v-text-field>
      </v-col>
    </v-row>

    <h3 v-if="intDoc.credentialData && !showOnlyContent">Credential Content</h3>
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
          :value="intDoc[documentDataType][field.type]"
          @change="docDataFieldChanged(field.type, $event)"
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
    this.prepareDocument();
  },
  data: () => {
    return {
      intDoc: Object,
      origIntDoc: Object,
      documentDataTypes: ["documentData", "credentialData", "proofData"],
      labelValue: "",
      documentDataType: "",
    };
  },
  computed: {
    schema: function () {
      let s = getSchema(this.document.type);
      if (s && {}.hasOwnProperty.call(s, "fields")) {
        return s;
        // No known schema. Generate one from data
        // Todo: Support arrays and objects as fields
      } else {
        s = {
          type: this.document.type,
          fields: Object.keys(this.intDoc).map((key) => {
            return {
              type: key,
              label: key,
            };
          }),
        };
        console.log(s);
        return s;
      }
    },
  },
  methods: {
    docDataFieldChanged(propertyName, event) {
      console.log("CREDENTIAL DATA FIELD CHANGED", propertyName, event);
      if (this.origIntDoc[this.documentDataType][propertyName] != event) {
        this.intDoc[this.documentDataType][propertyName] = event;
      } else {
        this.intDoc[this.documentDataType][propertyName] = event;
      }

      const isDirty = Object.keys(this.origIntDoc[this.documentDataType]).find(
        (key) => {
          return this.intDoc[this.documentDataType][key] !=
            this.origIntDoc[this.documentDataType][key]
            ? true
            : false;
        }
      )
        ? true
        : false;
      this.$emit("doc-data-field-changed", isDirty);
    },

    docFieldChanged(propertyName, event) {
      if (this.origIntDoc[propertyName] != event) {
        this.intDoc[propertyName] = event;
      } else {
        this.intDoc[propertyName] = event;
      }
      this.$emit("doc-field-changed", { key: propertyName, value: event });
    },

    prepareDocument() {
      //New Document
      if (!this.document.id) {
        this.documentDataType = this.documentDataTypes[0];
        this.document.label = "";
        this.intDoc[this.documentDataType] = Object.fromEntries(
          this.schema.fields.map((field) => {
            return [field.type, ""];
          })
        );
        //this.intDoc = Object.assign(this.intDoc, {[this.documentDataType]:arrFields});
        this.intCopy();
      }
      //Existing Document, extract Data
      else {
        this.documentDataTypes.forEach((field) => {
          if ({}.hasOwnProperty.call(this.document, field)) {
            this.documentDataType = field;
            this.intDoc = this.document;
            this.intCopy();
            return;
          }
        });
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
  },
};
</script>
