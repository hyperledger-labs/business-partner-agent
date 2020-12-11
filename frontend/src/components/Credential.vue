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
export default {
  props: {
    isReadOnly: {
      type: Boolean,
      default: false,
    },
    document: Object,
    showOnlyContent: Boolean,
    isNew: Boolean,
  },
  created() {
    console.log("Credential: ", this.document);
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
    schema: function () {
      let schemaTemplate = this.$store.getters.createTemplateFromSchemaId(
        this.document.schemaId
      );
      if (!schemaTemplate) {
        schemaTemplate = this.createTemplateFromSchema(this.document);
      } else {
        console.log("No Schema Template found", this.document);
      }
      return schemaTemplate;
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

    createTemplateFromSchemaId(schemaId) {
      const schemas = this.$store.getters.getSchemas;
      let schema = schemas.find((schema) => {
        return schema.schemaId === schemaId;
      });
      if (schema) {
        //TODO check if fields already available
        let objectTemplate = Object.assign(schema, {
          fields: schema.schemaAttributeNames.map((key) => {
            return {
              type: key,
              label: key
                ? key.substring(0, 1).toUpperCase() +
                  key.substring(1).replace(/([a-z])([A-Z])/g, "$1 $2")
                : "",
            };
          }),
        });
        return objectTemplate;
      }
      return null;
    },

    createTemplateFromSchema(objData) {
      const documentDataTypes = ["documentData", "credentialData", "proofData"];
      const dataType = documentDataTypes.find((val) => {
        if (objData && {}.hasOwnProperty.call(objData, val)) {
          return val;
        }
      });
      const s = {
        type: objData.type,
        label: "",
        fields: Object.keys(dataType ? objData[dataType] : objData).map(
          (key) => {
            return {
              type: key,
              label: key
                ? key.substring(0, 1).toUpperCase() +
                  key.substring(1).replace(/([a-z])([A-Z])/g, "$1 $2")
                : "",
            };
          }
        ),
      };
      return s;
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
      if (!this.document.id && !this.document.credentialData) {
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
