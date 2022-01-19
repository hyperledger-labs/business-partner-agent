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
        {{ $t("component.createSchema.title") }}
      </v-card-title>
      <v-card-text>
        <v-form ref="form" v-model="valid">
          <v-row>
            <v-col cols="4" class="pb-0">
              <p class="grey--text text--darken-2 font-weight-medium">
                {{ $t("component.createSchema.schemaInformation") }}
              </p>
            </v-col>
            <v-col cols="8" class="pb-0">
              <v-text-field
                :label="$t('component.createSchema.labelSchema')"
                :placeholder="$t('component.createSchema.placeholderSchema')"
                v-model="schemaLabel"
                :rules="[rules.required]"
                outlined
                dense
              ></v-text-field>
              <v-text-field
                :label="$t('component.createSchema.labelName')"
                :placeholder="$t('component.createSchema.placeholderName')"
                v-model="schemaName"
                :rules="[rules.required, rules.schemaName]"
                required
                outlined
                dense
              ></v-text-field>
              <v-text-field
                :label="$t('component.createSchema.labelVersion')"
                :placeholder="$t('component.createSchema.placeholderVersion')"
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
                {{ $t("component.createSchema.schemaAttributes") }}
              </p>
            </v-col>
          </v-row>
          <v-row>
            <v-col class="pb-0">
              <v-row>
                <v-col cols="8" class="py-0"
                  ><p class="grey--text">
                    {{ $t("component.createSchema.headersColumn.name") }}
                  </p></v-col
                >
                <v-col cols="2" class="py-0"
                  ><p class="grey--text">
                    {{ $t("component.createSchema.headersColumn.isDefault") }}
                  </p></v-col
                >
                <v-col class="py-0"
                  ><p class="grey--text">
                    {{ $t("component.createSchema.headersColumn.action") }}
                  </p>
                </v-col>
              </v-row>
              <v-row
                v-for="(attr, index) in schemaAttributes"
                v-bind:key="attr.type"
              >
                <v-col cols="8" class="py-0">
                  <v-text-field
                    :placeholder="
                      $t('component.createSchema.placeholderAttribute')
                    "
                    v-model="attr.text"
                    :rules="[rules.required, rules.schemaAttributeName]"
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
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
            :disabled="fieldsEmpty"
            >{{ $t("button.submit") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import { issuerService } from "@/services";
import VBpaButton from "@/components/BpaButton";
import * as textUtils from "@/utils/textUtils";

export default {
  name: "CreateSchema",
  components: { VBpaButton },
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
    };
  },
  computed: {
    fieldsEmpty() {
      return (
        this.schemaLabel.length === 0 ||
        this.schemaName.length === 0 ||
        !textUtils.isValidSchemaName(this.schemaName) ||
        this.schemaVersion.length === 0 ||
        !textUtils.isValidSchemaVersion(this.schemaVersion) ||
        this.schemaAttributes.filter((x) => x.text.trim().length).length ===
          0 ||
        this.schemaAttributes
          .filter((x) => x.text.trim().length)
          .some((x) => !textUtils.isValidSchemaAttributeName(x.text))
      );
    },
    rules() {
      return {
        required: (value) => !!value || this.$t("app.rules.required"),
        version: (value) =>
          textUtils.isValidSchemaVersion(value) ||
          this.$t("app.rules.validVersion"),
        schemaName: (value) =>
          textUtils.isValidSchemaName(value) ||
          this.$t("app.rules.validSchema"),
        schemaAttributeName: (value) =>
          textUtils.isValidSchemaAttributeName(value) ||
          this.$t("app.rules.validAttributeName"),
      };
    },
  },
  methods: {
    makeDefaultAttribute(index, value) {
      // if setting true, set all others to false...
      if (value) {
        for (const [index_, v] of this.schemaAttributes.entries())
          v.defaultAttribute = index === index_;
      }
    },
    addAttribute() {
      this.schemaAttributes.push({
        defaultAttribute: false,
        text: "",
      });
    },
    deleteAttribute(index) {
      this.schemaAttributes.splice(index, 1);
    },
    fixSchemaParams(s) {
      return s.trim().replace(/ /g, "_");
    },
    getSchemaFormData() {
      const attributes = this.schemaAttributes
        .filter((x) => x.text.trim().length)
        .map((x) => this.fixSchemaParams(x.text));
      const defaultAttribute = this.schemaAttributes.find(
        (x) => x.defaultAttribute
      );
      return {
        schemaLabel: this.schemaLabel,
        schemaName: this.fixSchemaParams(this.schemaName),
        schemaVersion: this.fixSchemaParams(this.schemaVersion),
        attributes: attributes,
        defaultAttributeName: defaultAttribute
          ? defaultAttribute.text
          : undefined,
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
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    async submit() {
      this.isBusy = true;
      try {
        const _schema = await this.saveSchema();
        if (_schema) {
          EventBus.$emit(
            "success",
            this.$t("component.createSchema.eventSuccess")
          );
          this.resetForm();
          this.$emit("success");
        }
      } catch (error) {
        this.isBusy = false;
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    cancel() {
      this.resetForm();
      this.$emit("cancelled");
    },
  },
};
</script>
