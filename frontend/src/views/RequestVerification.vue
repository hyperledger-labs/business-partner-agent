<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        Select Business Partner
      </v-card-title>
      <v-card-text>
        <h4 class="pt-4">
          Select the business partner you would like to request a verification
          from
        </h4>
        <PartnerList
          v-bind:selectable="true"
          ref="partnerList"
          v-bind:onlyIssuersForSchema="schemaId"
        ></PartnerList>
      </v-card-text>

      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()"
            >Cancel</v-bpa-button
          >
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="checkRequest"
            >Submit</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
    <v-dialog v-model="attentionPartnerStateDialog" max-width="500">
      <v-card>
        <v-card-title class="headline"
          >Connection State {{ partner.state }}
        </v-card-title>

        <v-card-text>
          The connection with your Business Partner is marked as
          {{ partner.state }}. This could mean that your request will fail. Do
          you want to try anyways?
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-bpa-button
            color="secondary"
            @click="attentionPartnerStateDialog = false"
            >No</v-bpa-button
          >

          <v-bpa-button color="primary" @click="submitRequest"
            >Yes</v-bpa-button
          >
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import PartnerList from "@/components/PartnerList";
import VBpaButton from "@/components/BpaButton";
import { getPartnerState } from "@/utils/partnerUtils";
import { PartnerStates } from "@/constants";

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
    EventBus.$emit("title", "Request Verification");
    if (!this.schemaId) {
      EventBus.$emit("success", "Can't start verification");
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
    };
  },
  computed: {},
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
        EventBus.$emit("error", "No partner for verification request selected");
      }
    },
    submitRequest() {
      this.attentionPartnerStateDialog = false;
      this.isBusy = true;
      this.$axios
        .post(
          `${this.$apiBaseUrl}/partners/${this.partner.id}/credential-request`,
          {
            documentId: this.documentId,
          }
        )
        .then((res) => {
          console.log(res);
          this.isBusy = false;
          EventBus.$emit("success", "Verification Request sent");
          this.$router.push({
            name: "Wallet",
          });
        })
        .catch((e) => {
          this.isBusy = false;
          EventBus.$emit("error", this.$axiosErrorMessage(e));
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
