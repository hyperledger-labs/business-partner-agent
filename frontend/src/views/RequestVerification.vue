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
        {{ $t("view.requestVerification.titleSelect") }}
      </v-card-title>
      <v-card-text>
        <h4 class="pt-4">
          {{ $t("view.requestVerification.subtitleSelect") }}
        </h4>
        <PartnerList
          v-bind:selectable="true"
          ref="partnerList"
          v-bind:onlyIssuersForSchema="schemaId"
        ></PartnerList>
      </v-card-text>

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
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="checkRequest"
            >{{ $t("button.submit") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
    <v-dialog v-model="attentionPartnerStateDialog" max-width="500">
      <v-card>
        <v-card-title class="headline"
          >{{
            $t("view.requestVerification.connectionState", {
              state: partner.state,
            })
          }}
        </v-card-title>

        <v-card-text>
          {{
            $t("view.requestVerification.connectionWarning", {
              state: partner.state,
            })
          }}
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-bpa-button
            color="secondary"
            @click="attentionPartnerStateDialog = false"
            >{{ $t("button.no") }}</v-bpa-button
          >

          <v-bpa-button color="primary" @click="submitRequest">{{
            $t("button.yes")
          }}</v-bpa-button>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import PartnerList from "@/components/PartnerList.vue";
import VBpaButton from "@/components/BpaButton";
import { getPartnerState } from "@/utils/partnerUtils";
import { PartnerStates, ExchangeVersion } from "@/constants";
import credentialService from "@/services/credentialService";

export default {
  name: "RequestVerification",
  components: {
    VBpaButton,
    PartnerList,
  },
  props: {
    documentId: String,
    schemaId: String,
  },
  created() {
    EventBus.$emit("title", this.$t("view.requestVerification.title"));
    if (!this.schemaId) {
      EventBus.$emit(
        "success",
        this.$t("view.requestVerification.eventSuccessNoVerification")
      );
      this.$router.push({ name: "Wallet" });
    }
  },
  data: () => {
    return {
      document: {},
      isBusy: false,
      isReady: false,
      attentionPartnerStateDialog: false,
      partner: {},
      getPartnerState: getPartnerState,
      useV2Exchange: false,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    checkRequest() {
      if (this.$refs.partnerList.selected.length === 1) {
        if (this.$refs.partnerList.selected[0].id) {
          this.partner = this.$refs.partnerList.selected[0];
          if (
            this.getPartnerState(this.partner) ===
            PartnerStates.ACTIVE_OR_RESPONSE
          ) {
            this.submitRequest();
          } else {
            this.attentionPartnerStateDialog = true;
          }
        }
      } else {
        EventBus.$emit(
          "error",
          this.$t("view.requestVerification.eventErrorSelect")
        );
      }
    },
    submitRequest() {
      this.attentionPartnerStateDialog = false;
      this.isBusy = true;
      const data = {
        documentId: this.documentId,
        exchangeVersion: this.useV2Exchange
          ? ExchangeVersion.V2
          : ExchangeVersion.V1,
      };

      credentialService
        .sendCredentialRequest(this.partner.id, data)
        .then((response) => {
          console.log(response);
          this.isBusy = false;
          EventBus.$emit(
            "success",
            this.$t("view.requestVerification.eventSuccessSend")
          );
          this.$router.go(-2);
        })
        .catch((error) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    cancel() {
      this.$router.go(-1);
    },
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}
</style>
