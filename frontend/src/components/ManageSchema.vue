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
        <span>{{ schema.label }}</span>
        <v-layout align-end justify-end>
          <v-btn
            depressed
            color="red"
            icon
            :disabled="schema.isReadOnly"
            @click="deleteSchema"
          >
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            v-model="schema.schemaId"
            readonly
            outlined
            dense
            @blur="reset"
            :label="$t('component.manageSchema.labelSchemaId')"
          >
            <template v-slot:append>
              <v-tooltip top>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    v-bind="attrs"
                    class="mr-0"
                    icon
                    v-on="on"
                    @click="copySchemaId"
                  >
                    <v-icon> $vuetify.icons.copy </v-icon>
                  </v-btn>
                </template>
                <span>{{ copyText }}</span>
              </v-tooltip>
            </template>
          </v-text-field>
        </v-list-item>
        <v-list-item class="mt-4">
          <v-checkbox
            class="mt-1"
            :label="$t('component.manageSchema.labelCheckbox')"
            v-model="schema.isMine"
            :readonly="true"
            outlined
            dense
          >
          </v-checkbox>
        </v-list-item>
      </v-container>
      <v-container>
        <v-tabs v-model="tab">
          <v-tab v-for="item in items" :key="item.key" :href="`#${item.key}`">
            {{ item.title }}
          </v-tab>
        </v-tabs>
        <v-tabs-items v-model="tab">
          <v-tab-item transition="false" value="schema-attributes">
            <v-card flat class="mt-2" v-show="!isEdit">
              <v-list-item
                v-for="attribute in schema.schemaAttributeNames"
                :key="attribute.id"
              >
                <p class="grey--text text--darken-2 font-weight-medium">
                  {{ attribute }}
                  <v-icon
                    :key="checkBoxGroup"
                    x-small
                    v-show="isDefaultAttribute(attribute)"
                    >$vuetify.icons.asterisk</v-icon
                  >
                </p>
              </v-list-item>
            </v-card>
            <v-form v-show="isEdit">
              <v-card flat class="mt-3">
                <v-row>
                  <v-col cols="8" class="py-0" />
                  <v-col cols="2" class="py-0"
                    ><p class="grey--text">
                      {{ $t("component.createSchema.headersColumn.isDefault") }}
                    </p></v-col
                  >
                </v-row>
                <v-row
                  v-for="(attr, index) in schema.schemaAttributeNames"
                  :key="attr.id"
                >
                  <v-col cols="8" class="py-0">
                    <v-list-item>
                      <p class="grey--text text--darken-2 font-weight-medium">
                        {{ attr }}
                        <v-icon x-small v-show="isDefaultAttribute(attr)"
                          >$vuetify.icons.asterisk</v-icon
                        >
                      </p>
                    </v-list-item>
                  </v-col>
                  <v-col cols="2" class="py-0">
                    <v-checkbox
                      class="mt-1 pt-1"
                      v-model="checkBoxGroup"
                      :value="index + 1"
                      outlined
                      dense
                    ></v-checkbox>
                  </v-col>
                </v-row>
              </v-card>
            </v-form>
          </v-tab-item>
          <v-tab-item transition="false" value="credential-definitions">
            <v-card flat class="mt-2">
              <credential-definitions
                :schema="schema"
                :credentialDefinitions="schema.credentialDefinitions"
                :reset="resetChildForms"
                @changed="onChanged"
              />
            </v-card>
          </v-tab-item>
          <v-tab-item transition="false" value="trusted-issuers">
            <v-card flat class="mt-2">
              <trusted-issuers
                :schema="schema"
                :trustedIssuers="schema.trustedIssuer"
                :reset="resetChildForms"
                @changed="onChanged"
              />
            </v-card>
          </v-tab-item>
        </v-tabs-items>
      </v-container>
      <v-card-actions>
        <v-layout align-end justify-end v-show="tabIsSchemaAttribute">
          <v-bpa-button v-show="!isEdit" color="secondary" @click="editSchema()"
            >{{ $t("button.edit") }}
          </v-bpa-button>
          <v-bpa-button
            v-show="isEdit"
            color="secondary"
            @click="updateSchema()"
            >{{ $t("button.save") }}</v-bpa-button
          >
          <v-bpa-button color="primary" @click="closed">{{
            $t("button.close")
          }}</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import store from "@/store";
import { adminService, UpdateSchemaRequest, SchemaAPI } from "@/services";
import TrustedIssuers from "@/components/TrustedIssuers.vue";
import CredentialDefinitions from "@/components/CredentialDefinitions.vue";
import VBpaButton from "@/components/BpaButton";
import { CredentialTypes } from "@/constants";
export default {
  name: "ManageSchema",
  props: {
    dialog: {
      type: Boolean,
      default: () => false,
    },
    trustedIssuers: {
      type: Boolean,
      default: () => true,
    },
    credentialDefinitions: {
      type: Boolean,
      default: () => true,
    },
    value: {},
  },
  components: {
    VBpaButton,
    CredentialDefinitions,
    TrustedIssuers,
  },
  watch: {
    dialog(value: boolean) {
      // if dialog is opening, reset to first tab
      if (value) {
        this.tab = "schema-attributes";
      }
    },
  },
  data: () => {
    return {
      isEdit: false,
      tab: undefined as string,
      resetChildForms: false,
      checkBoxGroup: 0,
      copyText: "",
    };
  },
  created() {
    this.copyText = this.$t("button.clickToCopy");
  },
  computed: {
    typeIsJsonLD() {
      return this.schema.type === CredentialTypes.JSON_LD.type;
    },
    schema: {
      get() {
        return this.value;
      },
      set(value: SchemaAPI) {
        this.$emit("input", value);
      },
    },
    items() {
      const tabs = [
        {
          title: this.$t("component.manageSchema.tabs.schemaAttributes"),
          key: "schema-attributes",
        },
      ];
      if (this.credentialDefinitions && !this.typeIsJsonLD)
        tabs.push({
          title: this.$t("component.manageSchema.tabs.credentialDefinitions"),
          key: "credential-definitions",
        });
      if ((!this.schema.isMine || this.typeIsJsonLD) && this.trustedIssuers)
        tabs.push({
          title: this.$t("component.manageSchema.tabs.trustedIssuers"),
          key: "trusted-issuers",
        });
      return tabs;
    },
    tabIsSchemaAttribute() {
      return this.tab === "schema-attributes";
    },
  },
  methods: {
    async copySchemaId() {
      await navigator.clipboard.writeText(this.schema.schemaId);
      this.copyText = this.$t("button.copied");
    },
    reset() {
      this.copyText = this.$t("button.clickToCopy");
    },
    isDefaultAttribute(attribute: string): boolean {
      return attribute === this.schema.defaultAttributeName;
    },
    editSchema() {
      this.isEdit = true;
    },
    getUpdatedSchema(): UpdateSchemaRequest {
      const newDefaultAttribute =
        this.schema.schemaAttributeNames[this.checkBoxGroup - 1];
      return {
        defaultAttribute: newDefaultAttribute,
      };
    },
    async updateSchema() {
      const newDefaultAttribute: UpdateSchemaRequest = this.getUpdatedSchema();
      this.schema.defaultAttributeName = newDefaultAttribute.defaultAttribute;
      adminService
        .updateSchema(this.schema.id, newDefaultAttribute)
        .then((result) => {
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("component.manageSchema.eventSuccessUpdate")
            );
            store.dispatch("loadSchemas");
            this.$emit("changed");
            this.$emit("updated");
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
      this.isEdit = false;
    },
    deleteSchema() {
      adminService
        .removeSchema(this.schema.id)
        .then((result) => {
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("component.manageSchema.eventSuccessDelete")
            );
            this.$emit("changed");
            this.$emit("deleted");
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    onChanged() {
      this.$emit("changed");
    },
    closed() {
      this.resetChildForms = !this.resetChildForms;
      this.isEdit = false;
      this.$emit("closed");
    },
  },
};
</script>
