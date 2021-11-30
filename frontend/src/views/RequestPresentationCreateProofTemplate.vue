<!--
 Copyright (c) 2020-2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <proof-template-create
    disable-route-back
    enable-v2-switch
    :create-button-label="$t('button.createAndSend')"
    v-on:received-proof-template-id="submitRequest($event)"
  ></proof-template-create>
</template>

<script>
import { EventBus } from "@/main";
import ProofTemplateCreate from "@/views/ProofTemplateCreate";
import proofTemplateService from "@/services/proofTemplateService";
import { ExchangeVersion } from "@/constants";

export default {
  name: "RequestPresentation",
  components: { ProofTemplateCreate },
  props: {
    id: String, // partner ID
  },
  mounted() {
    EventBus.$emit("title", this.$t("view.requestPresentation.title"));
  },
  methods: {
    async submitRequest(proofTemplateIdAndExchangeVersion) {
      const data = {
        exchangeVersion: proofTemplateIdAndExchangeVersion.useV2Exchange
          ? ExchangeVersion.V2
          : ExchangeVersion.V1,
      };

      proofTemplateService
        .sendProofTemplate(
          proofTemplateIdAndExchangeVersion.documentId,
          this.id,
          data
        )
        .then(() => {
          EventBus.$emit(
            "success",
            this.$t("view.requestPresentation.eventSuccessSend")
          );
          this.$router.go(-2);
        })
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
        });
    },
  },
};
</script>

<style>
.bg-light {
  background-color: #fafafa;
}
</style>
