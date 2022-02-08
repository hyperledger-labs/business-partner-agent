<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<style scoped>
.sub-table.theme--light.v-data-table {
  background: transparent;
}

.sub-table .v-data-table-header {
  display: none;
}
</style>

<template>
  <v-container>
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ proofTemplate.name }}</span>
        <v-layout align-end justify-end>
          <v-btn depressed color="red" icon @click="deleteProofTemplate">
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>

      <!-- Basic Data -->
      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateName"
            v-model="proofTemplate.name"
            readonly
            dense
            :label="$t('view.proofTemplate.view.name')"
          ></v-text-field>
        </v-list-item>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateCreatedAt"
            v-bind:value="proofTemplate.createdAt | formatDateLong"
            readonly
            dense
            :label="$t('view.proofTemplate.view.createdAt')"
          >
          </v-text-field>
        </v-list-item>
      </v-container>
      <v-divider></v-divider>

      <!-- Attribute Groups -->
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title>{{
            $t("view.proofTemplate.view.attributeGroupsTitle")
          }}</v-list-item-title>
          <v-list-item-subtitle>{{
            $t("view.proofTemplate.view.attributeGroupsSubtitle")
          }}</v-list-item-subtitle>
          <attribute-group
            v-bind:request-data="proofTemplate.attributeGroups"
          ></attribute-group>
        </v-list-item-content>
      </v-list-item>

      <!-- Proof Templates Actions -->
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="$router.go(-1)">
            {{ $t("button.close") }}
          </v-bpa-button>
          <v-bpa-button color="primary" disabled>
            {{ $t("view.proofTemplate.view.createProofRequest") }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import AttributeGroup from "@/components/proof-templates/AttributeGroup.vue";
import proofTemplateService from "@/services/proofTemplateService";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "ProofTemplates",
  props: {
    id: {
      type: String,
      required: false,
    },
  },
  components: {
    AttributeGroup,
    VBpaButton,
  },
  created() {
    EventBus.$emit("title", this.$t("view.proofTemplates.title"));
  },
  data: () => {
    return {
      proofTemplate: {},
    };
  },
  mounted() {
    proofTemplateService.getProofTemplate(this.id).then((result) => {
      this.proofTemplate = result.data;
    });
  },
  computed: {
    schemas() {
      return this.$store.getters.getSchemas.filter(
        (schema) => schema.type === "INDY"
      );
    },
  },
  methods: {
    deleteProofTemplate() {
      proofTemplateService
        .deleteProofTemplate(this.proofTemplate.id)
        .then((result) => {
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.proofTemplate.view.eventSuccessDelete")
            );
            this.$router.push({
              name: "ProofTemplates",
              params: {},
            });
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
  },
};
</script>
