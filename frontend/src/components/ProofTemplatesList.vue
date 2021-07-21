<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
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
      @click:row="openItem"
    >
    </v-data-table>
    <v-dialog v-model="dialog" persistent max-width="800px">
      <ProofTemplateView
        :dialog="dialog"
        :proofTemplate="proofTemplate"
        @closed="onClosed"
        @changed="onChanged"
        @deleted="onDeleted"
      />
    </v-dialog>
  </v-container>
</template>
<script>
import store from "@/store";
import ProofTemplateView from "@/components/ProofTemplateView";
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
  },
  components: {
    ProofTemplateView,
  },
};
</script>
