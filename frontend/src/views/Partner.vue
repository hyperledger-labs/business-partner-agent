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
          <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        <span v-if="!isUpdatingName">{{ partner.name }}</span>
        <v-text-field
          class="mt-8"
          v-else
          label="Name"
          append-icon="mdi-done"
          v-model="alias"
          outlined
          :rules="[rules.required]"
          dense
        >
          <template v-slot:append>
            <v-btn class="pb-1" text @click="isUpdatingName = false"
              >Cancel</v-btn
            >
            <v-btn
              class="pb-1"
              text
              color="primary"
              :loading="isBusy"
              @click="submitNameUpdate()"
              >Save</v-btn
            >
          </template>
        </v-text-field>
        <PartnerStateIndicator
          v-if="partner.state"
          v-bind:state="partner.state"
        ></PartnerStateIndicator>
        <v-layout align-center justify-end>
          <v-icon small>mdi-fingerprint</v-icon
          ><span
            class="grey--text text--darken-2 font-weight-medium text-caption pl-1 pr-4"
            >{{ partner.did }}</span
          >
          <v-btn icon @click="isUpdatingName = !isUpdatingName">
            <v-icon dark>mdi-pencil</v-icon>
          </v-btn>
          <v-tooltip top>
            <template v-slot:activator="{ on, attrs }">
              <v-btn
                color="primary"
                v-bind="attrs"
                v-on="on"
                icon
                @click="refreshPartner()"
              >
                <v-icon dark>mdi-refresh</v-icon>
              </v-btn>
            </template>
            <span>Refresh profile from source</span>
          </v-tooltip>

          <v-btn depressed color="red" icon @click="deletePartner()">
            <v-icon dark>mdi-delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-progress-linear v-if="isLoading" indeterminate></v-progress-linear>

      <v-card-text>
        <Profile v-if="isReady" v-bind:partner="partner"></Profile>
        <v-row v-if="partner.ariesSupport" class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">
                Received Presentations
              </p>
            </v-row>
            <v-row>The presentations you received from your partner</v-row>
            <v-row class="mt-4">
              <v-btn small @click="requestPresentation"
                >Request Presentation</v-btn
              >
            </v-row>
          </v-col>
          <v-col cols="8">
            <v-card flat>
              <PresentationList
                v-if="isReady"
                v-bind:credentials="presentationsReceived"
                v-bind:headers="headersReceived"
                v-on:removedItem="removePresentationReceived"
                :expandable="false"
              ></PresentationList>
            </v-card>
          </v-col>
        </v-row>
        <v-row class="mx-4">
          <v-divider></v-divider>
        </v-row>
        <v-row v-if="partner.ariesSupport" class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">
                Sent Presentations
              </p>
            </v-row>
            <v-row>The presentations you sent to your partner</v-row>
            <v-row class="mt-4">
              <v-btn small @click="sendPresentation"> Send Presentation</v-btn>
            </v-row>
          </v-col>
          <v-col cols="8">
            <PresentationList
              v-if="isReady"
              v-bind:credentials="presentationsSent"
              v-bind:headers="headersSent"
              v-on:removedItem="removePresentationSent"
              :expandable="false"
            ></PresentationList>
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions>
        <v-expansion-panels v-if="expertMode" accordion flat>
          <v-expansion-panel>
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
              >Show raw data</v-expansion-panel-header
            >
            <v-expansion-panel-content class="bg-light">
              <vue-json-pretty :data="rawData"></vue-json-pretty>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
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

          <v-btn
            color="secondary"
            text
            @click="attentionPartnerStateDialog = false"
            >No</v-btn
          >

          <v-btn color="primary" text @click="proceed">Yes</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import Profile from "@/components/Profile";
import PresentationList from "@/components/PresentationList";
import PartnerStateIndicator from "@/components/PartnerStateIndicator";
import { CredentialTypes } from "../constants";
import { getPartnerProfile, getPartnerName } from "../utils/partnerUtils";
import { EventBus } from "../main";
import {
  sentHeaders,
  receivedHeaders,
} from "@/components/tableHeaders/PartnerHeaders";

export default {
  name: "Partner",
  props: ["id"],
  components: {
    Profile,
    PresentationList,
    PartnerStateIndicator,
  },
  created() {
    EventBus.$emit("title", "Partner");
    this.getPartner();
    this.getPresentationRecords();

    this.$store.commit("partnerSeen", { id: this.id });
  },
  data: () => {
    return {
      isReady: false,
      isBusy: false,
      isLoading: true,
      isUpdatingName: false,
      attentionPartnerStateDialog: false,
      goTo: {},
      alias: "",
      partner: {},
      rawData: {},
      credentials: [],
      presentationsSent: [],
      presentationsReceived: [],
      rules: {
        required: (value) => !!value || "Can't be empty",
      },
      headersSent: sentHeaders,
      headersReceived: receivedHeaders,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
  },
  methods: {
    proceed() {
      this.attentionPartnerStateDialog = false;
      this.$router.push(this.goTo);
    },
    requestPresentation() {
      if (
        this.partner.state === "response" ||
        this.partner.state === "active"
      ) {
        this.$router.push({
          name: "RequestPresentation",
          params: {
            id: this.id,
          },
        });
      } else {
        this.attentionPartnerStateDialog = true;
        this.goTo = {
          name: "RequestPresentation",
          params: {
            id: this.id,
          },
        };
      }
    },
    sendPresentation() {
      if (
        this.partner.state === "response" ||
        this.partner.state === "active"
      ) {
        this.$router.push({
          name: "SendPresentation",
          params: {
            id: this.id,
          },
        });
      } else {
        this.attentionPartnerStateDialog = true;
        this.goTo = {
          name: "SendPresentation",
          params: {
            id: this.id,
          },
        };
      }
    },
    getPresentationRecords() {
      console.log("Getting presentation records...");
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}/proof`)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            let data = result.data;
            console.log(data);
            this.presentationsSent = data.filter((item) => {
              console.log(item);
              return item.role === "prover";
            });
            this.presentationsReceived = data.filter((item) => {
              return item.role === "verifier";
            });
            console.log(this.presentationsSent);
          }
        })
        .catch((e) => {
          console.error(e);
          // EventBus.$emit("error", e);
        });
    },
    removePresentationReceived(id) {
      this.presentationsReceived = this.presentationsReceived.filter((item) => {
        return item.id !== id;
      });
    },
    removePresentationSent(id) {
      this.presentationsSent = this.presentationsSent.filter((item) => {
        return item.id !== id;
      });
    },
    getPartner() {
      console.log("Getting partner...");
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then((result) => {
          console.log(result);
          if ({}.hasOwnProperty.call(result, "data")) {
            this.rawData = result.data;
            this.partner = {
              ...result.data,
              ...{
                profile: getPartnerProfile(result.data),
              },
            };

            // Hacky way to define a partner name
            // Todo: Make this consistent. Probably in backend
            this.partner.name = getPartnerName(this.partner);
            this.alias = this.partner.name;
            this.isReady = true;
            this.isLoading = false;
            console.log(this.partner);
          }
        })
        .catch((e) => {
          console.error(e);
          this.isLoading = false;
          EventBus.$emit("error", e);
        });
    },
    deletePartner() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Partner deleted");
            this.$router.push({
              name: "Partners",
            });
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    refreshPartner() {
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}/refresh`)
        .then(async (result) => {
          if (result.status === 200) {
            if ({}.hasOwnProperty.call(result, "data")) {
              console.log(result.data);
              this.rawData = result.data;
              this.partner = {
                ...result.data,
                ...{
                  profile: getPartnerProfile(result.data),
                },
              };
              if ({}.hasOwnProperty.call(this.partner, "credential")) {
                // Show only creds other than OrgProfile in credential list
                this.credentials = this.partner.credential.filter((cred) => {
                  return cred.type !== CredentialTypes.PROFILE.name;
                });
              }

              // Hacky way to define a partner name
              // Todo: Make this consistent. Probably in backend
              this.partner.name = getPartnerName(this.partner);
              this.alias = this.partner.name;
              console.log(this.partner);
              this.isReady = true;
              this.isLoading = false;
            }
          }
        })
        .catch((e) => {
          console.error(e);
          this.isLoading = false;
          EventBus.$emit("error", e);
        });
    },
    submitNameUpdate() {
      this.isBusy = true;
      if (this.alias && this.alias !== "") {
        this.$axios
          .put(`${this.$apiBaseUrl}/partners/${this.id}`, {
            alias: this.alias,
          })
          .then((result) => {
            if (result.status === 200) {
              this.isBusy = false;
              this.partner.name = this.alias;
              this.isUpdatingName = false;
            }
          })
          .catch((e) => {
            this.isBusy = false;
            this.isUpdatingName = false;
            console.error(e);
            EventBus.$emit("error", e);
          });
      } else {
        this.isBusy = false;
        console.log("blub");
      }
    },
  },
};
</script>

<style scoped>
.bg-light {
  background-color: #fafafa;
}

.bg-light-2 {
  background-color: #ececec;
}
</style>
