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
            id="schemaId"
            v-model="schema.schemaId"
            readonly
            outlined
            dense
            :label="$t('component.manageSchema.labelSchemaId')"
            :append-icon="'$vuetify.icons.copy'"
            @click:append="copySchemaId"
          ></v-text-field>
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
        <v-layout align-end justify-end>
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
import { adminService } from "@/services";
import TrustedIssuers from "@/components/TrustedIssuers.vue";
import CredentialDefinitions from "@/components/CredentialDefinitions.vue";
import VBpaButton from "@/components/BpaButton";
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
    dialog(value) {
      // if dialog is opening, reset to first tab
      if (value) {
        this.tab = "schema-attributes";
      }
    },
  },
  data: () => {
    return {
      tab: undefined,
      resetChildForms: false,
    };
  },
  computed: {
    schema: {
      get() {
        return this.value;
      },
      set(value) {
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
      if (this.credentialDefinitions)
        tabs.push({
          title: this.$t("component.manageSchema.tabs.credentialDefinitions"),
          key: "credential-definitions",
        });
      if (!this.schema.isMine && this.trustedIssuers)
        tabs.push({
          title: this.$t("component.manageSchema.tabs.trustedIssuers"),
          key: "trusted-issuers",
        });
      return tabs;
    },
  },
  methods: {
    copySchemaId() {
      let idElement = document.querySelector(
        "#schemaId"
      ) as HTMLTextAreaElement;
      idElement.select();
      let successful;
      try {
        successful = document.execCommand("copy");
      } catch {
        successful = false;
      }
      successful
        ? EventBus.$emit(
            "success",
            this.$t("component.manageSchema.eventSuccessCopy")
          )
        : EventBus.$emit(
            "error",
            this.$t("component.manageSchema.eventErrorCopy")
          );
      idElement.blur();
      window.getSelection().removeAllRanges();
    },
    deleteSchema() {
      adminService
        .deleteSchema(this.schema.id)
        .then((result) => {
          console.log(result);
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
      this.$emit("closed");
    },
  },
};
</script>
