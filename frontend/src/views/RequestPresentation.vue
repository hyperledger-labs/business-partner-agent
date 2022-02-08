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
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ $t("view.requestPresentation.title") }}
      </v-card-title>
      <v-card-text>
        <proof-templates-list show-checkboxes v-model="selectedProofTemplate">
        </proof-templates-list>
        <template>
          <v-card-actions>
            <v-layout align-center align-end justify-end>
              <v-switch
                v-if="expertMode"
                v-model="useV2Exchange"
                :label="$t('button.useV2')"
              ></v-switch>
              <v-bpa-button color="secondary" @click="cancel()">{{
                $t("button.cancel")
              }}</v-bpa-button>
              <v-bpa-button color="primary" @click="openCreateProofTemplate()">
                {{ $t("view.proofTemplate.create.title") }}
              </v-bpa-button>
              <v-bpa-button
                :loading="this.isBusy"
                :disabled="selectedProofTemplate.length === 0"
                color="primary"
                @click="submitRequest()"
              >
                {{ $t("view.requestPresentation.sendRequest") }}
              </v-bpa-button>
            </v-layout>
          </v-card-actions>
        </template>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import ProofTemplatesList from "@/components/proof-templates/ProofTemplatesList.vue";
import proofTemplateService from "@/services/proofTemplateService";
import { ExchangeVersion } from "@/constants";

export default {
  name: "RequestPresentation",
  components: { ProofTemplatesList, VBpaButton },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.requestPresentation.title"));
  },
  data: () => {
    return {
      isBusy: false,
      selectedProofTemplate: [],
      selectedSchema: [],
      selectedIssuer: [],
      useV2Exchange: false,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    openCreateProofTemplate() {
      this.$router.push({
        name: "RequestPresentationCreateProofTemplate",
        params: {
          id: this.id,
        },
      });
    },
    async submitRequest() {
      this.isBusy = true;
      const data = {
        exchangeVersion: this.useV2Exchange
          ? ExchangeVersion.V2
          : ExchangeVersion.V1,
      };

      proofTemplateService
        .sendProofTemplate(this.selectedProofTemplate[0].id, this.id, data)
        .then(() => {
          EventBus.$emit(
            "success",
            this.$t("view.requestPresentation.eventSuccessSend")
          );
          this.isBusy = false;
          this.$router.go(-1);
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
          this.isBusy = false;
        });
    },
    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
