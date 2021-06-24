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
        Create a Presentation Request
      </v-card-title>
      <v-card-text>
        <template>
          <v-stepper class="elevation-0" v-model="step" flat>
            <v-stepper-header>
              <v-stepper-step :complete="step > 1" step="1">
                Select Credential Type (Schema)
              </v-stepper-step>

              <v-stepper-step :complete="step > 2" step="2">
                Select allowed Issuers
              </v-stepper-step>
            </v-stepper-header>

            <v-stepper-items>
              <v-stepper-content step="1">
                <v-data-table
                  hide-default-footer
                  v-model="selectedSchema"
                  :show-select="true"
                  single-select
                  :headers="headers"
                  :items="schemas"
                  item-key="id"
                  hide-default-header
                >
                </v-data-table>

                <v-card-actions>
                  <v-layout align-end justify-end>
                    <v-bpa-button color="secondary" @click="cancel()"
                      >Cancel</v-bpa-button
                    >
                    <v-bpa-button
                      :disabled="selectedSchema.length === 0"
                      color="primary"
                      @click="step = 2"
                      >Continue</v-bpa-button
                    >
                  </v-layout>
                </v-card-actions>
              </v-stepper-content>

              <v-stepper-content step="2">
                <v-data-table
                  hide-default-footer
                  v-model="selectedIssuer"
                  :show-select="true"
                  :items="
                    selectedSchema.length > 0
                      ? selectedSchema[0].trustedIssuer
                      : []
                  "
                  item-key="id"
                  :headers="headersIssuer"
                  hide-default-header
                >
                </v-data-table>

                <v-card-actions>
                  <v-layout align-end justify-end>
                    <v-bpa-button color="secondary" @click="step = 1">Back</v-bpa-button>

                    <v-bpa-button
                      :loading="this.isBusy"
                      color="primary"
                      @click="submitRequest()"
                      >Send Request</v-bpa-button
                    >
                  </v-layout>
                </v-card-actions>
              </v-stepper-content>
            </v-stepper-items>
          </v-stepper>
        </template>
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "../main";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "RequestPresentation",
  components: {VBpaButton},
  props: {
    id: String, //partner ID
  },
  created() {
    EventBus.$emit("title", "Request Presentation");
    this.fetchSchemas();
  },
  data: () => {
    return {
      isBusy: false,
      step: 1,
      schemas: [],
      selectedSchema: [],
      selectedIssuer: [],
      headers: [
        {
          text: "",
          value: "label",
        },
      ],
      headersIssuer: [
        {
          text: "Name",
          value: "label",
        },
      ],
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    fetchSchemas() {
      this.$axios
        .get(`${this.$apiBaseUrl}/admin/schema`)
        .then((result) => {
          console.log(result);
          if ({}.hasOwnProperty.call(result, "data")) {
            this.schemas = result.data;
          }
        })
        .catch((e) => {
          if (e.response.status === 404) {
            this.schemas = [];
          } else {
            console.error(e);
            EventBus.$emit("error", e);
          }
        });
    },
    submitRequest() {
      this.isBusy = true;

      let request = {
        requestBySchema: {
          schemaId: this.selectedSchema[0].schemaId,
          issuerDid: this.selectedIssuer.map((entry) => entry.issuerDid),
        },
      };

      this.$axios
        .post(`${this.$apiBaseUrl}/partners/${this.id}/proof-request`, request)
        .then(() => {
          this.isBusy = false;
          EventBus.$emit("success", "Presentation request sent");
          this.$router.go(-1);
        })
        .catch((e) => {
          this.isBusy = false;
          console.error(e);
          EventBus.$emit("error", e);
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

.bg-light-2 {
  background-color: #ececec;
}

@media only screen and (max-width: 959px) {
  .v-stepper:not(.v-stepper--vertical) .v-stepper__label {
    display: flex !important;
  }
}
</style>
