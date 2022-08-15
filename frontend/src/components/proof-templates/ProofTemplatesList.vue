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
      v-on:input="loadProofTemplates"
      single-line
      hide-details
      clearable
    ></v-text-field>
    <v-data-table
      :hide-default-footer="hideFooter"
      :server-items-length="totalNumberOfElements"
      :options.sync="options"
      v-model="inputValue"
      :loading="isBusy"
      :show-select="showCheckboxes"
      :headers="headers"
      :items="proofTemplates"
      sort-by="createdAt"
      sort-desc
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
import { EventBus } from "@/main";
import { PageOptions, ProofTemplate, proofTemplateService } from "@/services";

export default {
  props: {
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
      isBusy: true,
      proofTemplates: new Array<ProofTemplate>(),
      hideFooter: true,
      totalNumberOfElements: 0,
      options: {},
    };
  },
  watch: {
    options: {
      handler() {
        this.loadProofTemplates();
      },
    },
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
    inputValue: {
      get() {
        return this.value;
      },
      set(value: ProofTemplate) {
        this.$emit("input", value);
      },
    },
  },
  methods: {
    async loadProofTemplates() {
      this.isBusy = true;
      this.proofTemplates = [];
      if (!this.search) {
        this.search = "";
      }
      const params = PageOptions.toUrlSearchParams(this.options);
      try {
        const response = await proofTemplateService.getProofTemplates(
          this.search,
          params
        );
        if (response.status === 200) {
          const { itemsPerPage } = this.options;
          this.proofTemplates = response.data.content;
          this.totalNumberOfElements = response.data.totalSize;
          this.hideFooter = this.totalNumberOfElements <= itemsPerPage;
        }
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
      this.isBusy = false;
    },
    openItem(proofTemplate: ProofTemplate) {
      this.dialog = true;
      this.dirty = false;
      this.proofTemplate = proofTemplate;
    },
    onClosed() {
      this.dialog = false;
      if (this.dirty) {
        this.loadProofTemplates();
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
    viewProofTemplate(proofTemplate: ProofTemplate) {
      this.$router.push({
        name: "ProofTemplateView",
        params: {
          id: proofTemplate.id,
        },
      });
    },
    deleteProofTemplate(proofTemplate: ProofTemplate) {
      console.log(JSON.stringify(proofTemplate));

      proofTemplateService
        .deleteProofTemplate(proofTemplate.id)
        .then((result) => {
          if (result.status === 200) {
            this.$emit("removedItem", proofTemplate.id);
          }
          // reload proof templates
          this.loadProofTemplates();
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
