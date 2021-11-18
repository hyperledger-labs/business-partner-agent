<!--
  - Copyright (c) 2020-2021 - for information on the respective copyright owner
  - see the NOTICE file and/or the repository at
  - https://github.com/hyperledger-labs/business-partner-agent
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
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
<script>
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
      set(val) {
        this.$emit("input", val);
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
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
        });
    },
  },
};
</script>
