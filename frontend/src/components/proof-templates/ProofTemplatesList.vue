<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<!--
This component lists all proof templates in a table allowing the user to manage existing proof templates and
create new ones
-->
<template>
  <v-container>
    <v-text-field
      v-model="search"
      append-icon="$vuetify.icons.search"
      :label="$t('app.search')"
      single-line
      hide-details
    ></v-text-field>
    <v-data-table
      v-model="inputValue"
      :loading="isLoading"
      :show-select="showCheckboxes"
      :hide-default-footer="proofTemplates.length < 10"
      :headers="headers"
      :items="proofTemplates"
      :search="search"
      single-select
      @click:row="viewProofTemplate"
    >
      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>
    </v-data-table>
  </v-container>
</template>
<script lang="ts">
import store from "@/store";
import { EventBus } from "@/main";

export default {
  props: {
    isLoading: Boolean,
    showCheckboxes: {
      type: Boolean,
      default: false,
    },
    value: Array,
  },
  data: () => {
    return {
      search: "",
      dialog: false,
      proofTemplate: {},
      dirty: false,
    };
  },
  created() {
    this.$store.dispatch("loadProofTemplates");
  },
  computed: {
    headers() {
      return [
        {
          text: this.$t("view.proofTemplate.list.name"),
          value: "name",
        },
        {
          text: this.$t("view.proofTemplate.list.createdAt"),
          value: "createdAt",
        },
      ];
    },
    proofTemplates: {
      get() {
        return this.$store.getters.getProofTemplates;
      },
    },
    inputValue: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
  methods: {
    openItem(proofTemplate) {
      this.dialog = true;
      this.dirty = false;
      this.proofTemplate = proofTemplate;
    },
    onClosed() {
      this.dialog = false;
      if (this.dirty) {
        store.dispatch("loadProofTemplates");
        this.$emit("changed");
      }
      this.dirty = false;
    },
    onDeleted() {
      this.dialog = false;
    },
    onChanged() {
      this.dirty = true;
    },
    viewProofTemplate(proofTemplate) {
      this.$router.push({
        name: "ProofTemplateView",
        params: {
          id: proofTemplate.id,
        },
      });
    },
    deleteProofTemplate(proofTemplate) {
      console.log(JSON.stringify(proofTemplate));

      this.$axios
        .delete(`${this.$apiBaseUrl}/proof-templates/${proofTemplate.id}`)
        .then((result) => {
          if (result.status === 200) {
            this.$emit("removedItem", proofTemplate.id);
          }

          // reload proof templates
          this.$store.dispatch("loadProofTemplates");
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
