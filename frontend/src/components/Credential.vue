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
            (intDoc.label instanceof String ||
              typeof intDoc.label === 'string') &&
            !showOnlyContent
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
          v-for="field in filteredSchemaField"
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
    isNew: Boolean,
  },
  created() {
    console.log("Credential: ", this.document);
    this.prepareDocument();
    // // New created document
    // if (
    //   !{}.hasOwnProperty.call(this.document, "documentData") &&
    //   !{}.hasOwnProperty.call(this.document, "credentialData") &&
    //   !{}.hasOwnProperty.call(this.document, "proofData")
    // ) {
    //   this.documentData = Object.fromEntries(
    //     this.schema.fields.map((field) => {
    //       return [field.type, ""];
    //     })
    //   );
    //   // Existing document or credential
    // } else {
    //   // Check if document or credential data is here. This needs to be improved
    //   let documentData;
    //   if (this.document.documentData) {
    //     documentData = this.document.documentData;
    //   } else if (this.document.credentialData) {
    //     documentData = this.document.credentialData;
    //   } else if (this.document.proofData) {
    //     documentData = this.document.proofData;
    //   }
    //   // Only support one nested node for now
    //   let nestedData = Object.values(documentData).find((value) => {
    //     return typeof value === "object" && value !== null;
    //   });
    //   documentData = nestedData ? nestedData : documentData;

    //   // Filter empty elements only for credentials
    //   if (!this.document.documentData) {
    //     this.documentData = Object.fromEntries(
    //       Object.entries(documentData).filter(([, value]) => {
    //         return value !== "";
    //       })
    //     );
    //   } else {
    //     this.documentData = documentData;
    //   }

    //   this.intDoc = { ...this.documentData };
    // }
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
    schema: function () {
      let s = getSchema(this.document.type);
      if (s && {}.hasOwnProperty.call(s, "fields")) {
        return s;
      } else {
        const dataType = this.documentDataTypes.find((val) => {
          if (this.document && {}.hasOwnProperty.call(this.document, val)) {
            return val;
          }
        });
        console.log(dataType);
        if (dataType) {
          s = {
            type: this.document.type,
            label: "",
            fields: Object.keys(
              dataType ? this.intDoc[dataType] : this.intDoc
            ).map((key) => {
              return {
                type: key,
                label: key
                  ? key.substring(0, 1).toUpperCase() +
                    key.substring(1).replace(/([a-z])([A-Z])/g, "$1 $2")
                  : "",
              };
            }),
          };
          return s;
        } else {
          return this.$store.getters.getPreparedSchema(this.document.schemaId);
        }
      }
    },
    filteredSchemaField() {
      let fields = this.schema.fields;
      if (!this.isReadOnly) {
        return fields;
      } else {
        return fields.filter((field) => {
          if (this.intDoc[this.documentDataType][field.type] !== "") {
            return field;
          }
        });
      }
    },
  },
  methods: {
    docDataFieldChanged(propertyName, event) {
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
      if (
        !this.document.id &&
        !{}.hasOwnProperty.call(this.document, "schemaId")
      ) {
        this.documentDataType = this.documentDataTypes[0];
        this.intDoc.label = "";
        this.intDoc[this.documentDataType] = Object.fromEntries(
          this.schema.fields.map((field) => {
            return [field.type, ""];
          })
        );
        this.intCopy();
      }
      //Existing Document, extract Data
      else {
        this.documentDataTypes.forEach((field) => {
          if ({}.hasOwnProperty.call(this.document, field)) {
            this.documentDataType = field;
            this.intDoc = this.document;
            console.log(this.intDoc);
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
