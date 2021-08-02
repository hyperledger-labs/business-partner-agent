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
    <v-data-table
      :loading="isLoading"
      :hide-default-footer="proofTemplates.length < 10"
      :headers="headers"
      :items="proofTemplates"
      single-select
      :sort-by="['name', 'createdAt']"
      :sort-desc="[false, true]"
      multi-sort
      @click:row="viewProofTemplate"
    >
    </v-data-table>
  </v-container>
</template>
<script>
import store from "@/store";

export default {
  props: {
    headers: {
      type: Array,
      default: () => [
        {
          text: "Name",
          value: "name",
        },
        {
          text: "Created At",
          value: "createdAt",
        },
      ],
    },
    isLoading: Boolean,
  },
  data: () => {
    return {
      dialog: false,
      proofTemplate: {},
      dirty: false,
    };
  },
  computed: {
    proofTemplates: {
      get() {
        return this.$store.getters.getProofTemplates;
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
  },
  components: {},
};
</script>
