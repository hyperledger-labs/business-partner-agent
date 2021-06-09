<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light"> Create Schema </v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-row>
            <v-col cols="4" class="pb-0">
              <p class="grey--text text--darken-2 font-weight-medium">
                Schema Information
              </p>
            </v-col>
            <v-col cols="8" class="pb-0">
              <v-text-field
                label="Schema Label"
                placeholder="Label in your application for this schema"
                v-model="schemaLabel"
                :rules="[rules.required]"
                outlined
                dense
              ></v-text-field>
              <v-text-field
                label="Schema Name"
                placeholder="Published schema name (ex. my-schema)"
                v-model="schemaName"
                :rules="[rules.required, rules.schemaText]"
                required
                outlined
                dense
              ></v-text-field>
              <v-text-field
                label="Schema Version"
                placeholder="Published schema version (ex. 1.2 or 1.2.3)"
                v-model="schemaVersion"
                :rules="[rules.required, rules.version]"
                required
                outlined
                dense
              ></v-text-field>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="4" class="pb-0">
              <p class="grey--text text--darken-2 font-weight-medium">
                Schema Attributes
              </p>
            </v-col>
          </v-row>
          <v-row>
            <v-col class="pb-0">
              <v-row>
                <v-col cols="8" class="py-0"
                  ><p class="grey--text">Name</p></v-col
                >
                <v-col cols="2" class="py-0"
                  ><p class="grey--text">Is Default</p></v-col
                >
                <v-col class="py-0"><p class="grey--text">Action</p> </v-col>
              </v-row>
              <v-row
                v-for="(attr, index) in schemaAttributes"
                v-bind:key="attr.type"
              >
                <v-col cols="8" class="py-0">
                  <v-text-field
                    placeholder="Ex. companyName or company-name"
                    v-model="attr.text"
                    :rules="[rules.required, rules.schemaText]"
                    outlined
                    dense
                  ></v-text-field>
                </v-col>
                <v-col cols="2" class="py-0">
                  <v-checkbox
                    v-model="attr.defaultAttribute"
                    outlined
                    dense
                    style="margin-top: 4px; padding-top: 4px"
                    @change="makeDefaultAttribute(index, attr.defaultAttribute)"
                  ></v-checkbox>
                </v-col>
                <v-col class="py-0">
                  <v-layout>
                    <v-btn
                      v-if="index === schemaAttributes.length - 1"
                      icon
                      text
                      @click="addAttribute"
                    >
                      <v-icon color="primary">$vuetify.icons.add</v-icon></v-btn
                    >
                    <v-btn
                      icon
                      v-if="index !== schemaAttributes.length - 1"
                      @click="deleteAttribute(index)"
                    >
                      <v-icon color="error">$vuetify.icons.delete</v-icon>
                    </v-btn>
                  </v-layout>
                </v-col>
              </v-row>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
          <v-btn
            :loading="this.isBusy"
            color="primary"
            text
            @click="submit()"
            :disabled="fieldsEmpty"
            >Submit</v-btn
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import { issuerService } from "@/services";

export default {
  name: "CreateSchema",
  components: {},
  props: {},
  data: () => {
    return {
      valid: true,
      schemaLabel: "",
      schemaName: "",
      schemaVersion: "",
      schemaAttributeText: "",
      schemaAttributes: [{ defaultAttribute: true, text: "" }],
      isBusy: false,
      rules: {
        required: (value) => !!value || "Can't be empty",
        version: (value) =>
          (value && /^(\d+)\.(\d+)(?:\.\d+)?$/.test(value)) ||
          "Must be follow common version numbering (ex. 1.2 or 1.2.3)",
        schemaText: (value) =>
          (value && /^[a-zA-Z\d-_]+$/.test(value)) ||
          "Must be alphanumeric with optional '_' or '-'",
      },
    };
  },
  computed: {
    fieldsEmpty() {
      return (
        this.schemaLabel.length === 0 ||
        this.schemaName.length === 0 ||
        this.schemaVersion.length === 0 ||
        this.schemaAttributes.length === 0
      );
    },
  },
  methods: {
    makeDefaultAttribute(idx, val) {
      // if setting true, set all others to false...
      if (val) {
        this.schemaAttributes.forEach(
          (v, i) => (v.defaultAttribute = idx === i)
        );
      }
    },
    addAttribute() {
      this.schemaAttributes.push({
        defaultAttribute: false,
        text: "",
      });
    },
    deleteAttribute(i) {
      this.schemaAttributes.splice(i, 1);
    },
    fixSchemaParams(s) {
      return s.trim().replace(/ /g, "_");
    },
    getSchemaFormData() {
      const attrs = this.schemaAttributes
        .filter((x) => x.text.trim().length)
        .map((x) => this.fixSchemaParams(x.text));
      const defaultAttr = this.schemaAttributes.find((x) => x.defaultAttribute);
      return {
        schemaLabel: this.schemaLabel,
        schemaName: this.fixSchemaParams(this.schemaName),
        schemaVersion: this.fixSchemaParams(this.schemaVersion),
        attributes: attrs,
        defaultAttributeName: defaultAttr ? defaultAttr.text : undefined,
      };
    },
    resetForm() {
      // reset validation
      this.$refs.form.reset();
      // reset form data
      const initialData = this.$options.data.call(this);
      Object.assign(this.$data, initialData);
    },
    async saveSchema() {
      try {
        const schemaForm = this.getSchemaFormData();
        const resp = await issuerService.createSchema(schemaForm);
        if (resp.status === 200) {
          return resp.data;
        }
      } catch (error) {
        if (error.response && error.response.status === 400) {
          EventBus.$emit("error", "Schema already exists");
        } else {
          EventBus.$emit("error", error);
        }
      }
    },
    async submit() {
      this.isBusy = true;
      try {
        const _schema = await this.saveSchema();
        if (_schema) {
          EventBus.$emit("success", "Schema created successfully");
          this.resetForm();
          this.$emit("success");
        }
      } catch (error) {
        this.isBusy = false;
        EventBus.$emit("error", error);
      }
    },
    cancel() {
      this.resetForm();
      this.$emit("cancelled");
    },
  },
};
</script>
