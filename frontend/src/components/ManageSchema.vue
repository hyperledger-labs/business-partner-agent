<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

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
            id="schemaId"
            v-model="schema.schemaId"
            readonly
            outlined
            dense
            label="Schema ID"
            :append-icon="'$vuetify.icons.copy'"
            @click:append="copySchemaId"
          ></v-text-field>
        </v-list-item>
        <v-list-item class="mt-4">
          <v-checkbox
            class="mt-1"
            label="My Schema"
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
            <v-card flat class="mt-2">
              <v-list-item
                v-for="attribute in schema.schemaAttributeNames"
                :key="attribute.id"
              >
                <p class="grey--text text--darken-2 font-weight-medium">
                  {{ attribute }}
                </p>
              </v-list-item>
            </v-card>
          </v-tab-item>
          <v-tab-item transition="false" value="credential-definitions">
            <v-card flat class="mt-2">
              <credential-definitions
                :schema="schema"
                :credentialDefinitions="schema.credentialDefinitions"
                @changed="onChanged"
              />
            </v-card>
          </v-tab-item>
          <v-tab-item transition="false" value="trusted-issuers">
            <v-card flat class="mt-2">
              <trusted-issuers
                :schema="schema"
                :trustedIssuers="schema.trustedIssuer"
                @changed="onChanged"
              />
            </v-card>
          </v-tab-item>
        </v-tabs-items>
      </v-container>

      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="primary" @click="closed">Close</v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import { adminService } from "@/services";
import TrustedIssuers from "@/components/TrustedIssuers";
import CredentialDefinitions from "@/components/CredentialDefinitions";
import VBpaButton from "@/components/BpaButton";
export default {
  name: "ManageSchema",
  props: {
    dialog: {
      type: Boolean,
      default: () => false,
    },
    schema: Object,
    trustedIssuers: {
      type: Boolean,
      default: () => true,
    },
    credentialDefinitions: {
      type: Boolean,
      default: () => true,
    },
  },
  components: {
    VBpaButton,
    CredentialDefinitions,
    TrustedIssuers,
  },
  watch: {
    dialog(val) {
      // if dialog is opening, reset to first tab
      if (val) {
        this.tab = "schema-attributes";
      }
    },
  },
  created() {},
  data: () => {
    return {
      tab: null,
    };
  },
  computed: {
    items() {
      const tabs = [{ title: "Schema Attributes", key: "schema-attributes" }];
      if (this.credentialDefinitions)
        tabs.push({
          title: "Credential Definitions",
          key: "credential-definitions",
        });
      if (!this.schema.isMine && this.trustedIssuers)
        tabs.push({ title: "Trusted Issuers", key: "trusted-issuers" });
      return tabs;
    },
  },
  methods: {
    copySchemaId() {
      let idEl = document.querySelector("#schemaId");
      idEl.select();
      let successfull;
      try {
        successfull = document.execCommand("copy");
      } catch (err) {
        successfull = false;
      }
      successfull
        ? EventBus.$emit("success", "Schema ID copied")
        : EventBus.$emit("error", "Can't copy Schema ID");
      idEl.blur();
      window.getSelection().removeAllRanges();
    },
    deleteSchema() {
      adminService
        .deleteSchema(this.schema.id)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Schema deleted");
            this.$emit("changed");
            this.$emit("deleted");
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    onChanged() {
      this.$emit("changed");
    },
    closed() {
      this.$emit("closed");
    },
  },
};
</script>
