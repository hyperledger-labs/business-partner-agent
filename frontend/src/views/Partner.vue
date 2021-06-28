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
        <span>{{ partner.name }}</span>
        <PartnerStateIndicator
          v-if="partner.state"
          v-bind:state="partner.state"
        ></PartnerStateIndicator>
        <v-chip class="ml-2" v-for="tag in partner.tag" :key="tag.id">{{
          tag.name
        }}</v-chip>
        <v-layout align-center justify-end>
          <v-btn icon @click="isUpdatingDid = !isUpdatingDid">
            <v-icon small dark>$vuetify.icons.identity</v-icon>
          </v-btn>
          <span
            v-if="!isUpdatingDid"
            class="grey--text text--darken-2 font-weight-medium text-caption pl-1 pr-4"
            >{{ partner.did }}</span
          >
          <v-text-field
            class="mt-4 col-lg-6 col-md-6 col-sm-8"
            v-else
            label="DID"
            v-model="did"
            outlined
            :rules="[rules.required]"
            dense
          >
            <template v-slot:append>
              <v-bpa-button
                color="secondary"
                class="pb-1"
                @click="isUpdatingDid = false"
                >{{ $t("button.cancel") }}</v-bpa-button
              >
              <v-bpa-button
                class="pb-1"
                color="primary"
                :loading="isBusy"
                @click="submitDidUpdate()"
                >{{ $t("button.save") }}</v-bpa-button
              >
            </template>
          </v-text-field>
          <v-dialog v-model="updatePartnerDialog" max-width="600px">
            <template v-slot:activator="{ on, attrs }">
              <v-btn icon v-bind="attrs" v-on="on" color="primary">
                <v-icon dark>$vuetify.icons.pencil</v-icon>
              </v-btn>
            </template>
            <UpdatePartner
              v-bind:partner="partner"
              @success="onUpdatePartner"
              @cancelled="updatePartnerDialog = false"
            />
          </v-dialog>

          <v-tooltip top>
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button
                color="primary"
                v-bind="attrs"
                v-on="on"
                icon
                @click="refreshPartner()"
              >
                <v-icon dark>$vuetify.icons.refresh</v-icon>
              </v-bpa-button>
            </template>
            <span>{{ $t("view.partner.refreshProfile") }}</span>
          </v-tooltip>

          <v-btn depressed color="red" icon @click="deletePartner()">
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-progress-linear v-if="isLoading" indeterminate></v-progress-linear>

      <v-card-text>
        <template
          v-if="partner.bpa_state === PartnerStates.CONNECTION_REQUEST_RECEIVED"
        >
          <v-banner two-line>
            <v-avatar slot="icon" color="white" size="40">
              <v-icon icon="$vuetify.icons.connectionAlert" color="primary">
                $vuetify.icons.connectionAlert
              </v-icon>
            </v-avatar>

            <v-row>
              <span class="font-weight-medium">
                {{ $t("view.partner.requestReceived") }}
              </span>
            </v-row>
            <v-row>{{
              $t("view.partner.requestReceivedSubtitle", { alias: this.alias })
            }}</v-row>
            <template v-slot:actions>
              <v-bpa-button color="secondary" @click="deletePartner">
                {{ $t("view.partner.removePartner") }}
              </v-bpa-button>
              <v-bpa-button color="primary" @click="acceptPartnerRequest">
                {{ $t("view.partner.acceptPartner") }}
              </v-bpa-button>
            </template>
          </v-banner>
        </template>
        <template
          v-if="partner.bpa_state === PartnerStates.CONNECTION_REQUEST_SENT"
        >
          <v-banner two-line>
            <v-avatar slot="icon" color="white" size="40">
              <v-icon icon="$vuetify.icons.connectionWaiting" color="primary">
                $vuetify.icons.connectionWaiting
              </v-icon>
            </v-avatar>

            <v-row>
              <span class="font-weight-medium">{{
                $t("view.partner.requestSent")
              }}</span>
            </v-row>
            <v-row>{{ $t("view.partner.requestSentSubtitle") }}</v-row>
          </v-banner>
        </template>
        <Profile v-if="isReady" v-bind:partner="partner"></Profile>
        <v-row v-if="partner.ariesSupport" class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">
                {{ $t("view.partner.receivedPresentations.title") }}
              </p>
            </v-row>
            <v-row>{{ $t("view.partner.receivedPresentations.text") }}</v-row>
            <v-row class="mt-4">
              <v-btn small @click="requestPresentation" :disabled="!isActive">{{
                $t("view.partner.receivedPresentations.button")
              }}</v-btn>
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
                {{ $t("view.partner.sentPresentations.title") }}
              </p>
            </v-row>
            <v-row>{{ $t("view.partner.sentPresentations.text") }}</v-row>
            <v-row class="mt-4">
              <v-btn small @click="sendPresentation" :disabled="!isActive">
                {{ $t("view.partner.sentPresentations.button") }}</v-btn
              >
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
        <v-row class="mx-4">
          <v-divider></v-divider>
        </v-row>
        <v-row v-if="partner.ariesSupport" class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">
                {{ $t("view.partner.issuedCredentials.title") }}
              </p>
            </v-row>
            <v-row>{{ $t("view.partner.issuedCredentials.text") }}</v-row>
            <v-row class="mt-4">
              <v-dialog
                v-model="issueCredentialDialog"
                persistent
                max-width="600px"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-btn small v-bind="attrs" v-on="on">{{
                    $t("view.partner.issuedCredentials.button")
                  }}</v-btn>
                </template>
                <IssueCredential
                  :partnerId="id"
                  @success="onCredentialIssued"
                  @cancelled="issueCredentialDialog = false"
                >
                </IssueCredential>
              </v-dialog>
            </v-row>
          </v-col>
          <v-col cols="8">
            <CredExList
              v-if="isReady"
              v-bind:items="issuedCredentials"
              v-bind:headers="headersIssued"
            ></CredExList>
          </v-col>
        </v-row>
        <v-row v-if="partner.ariesSupport" class="mx-4">
          <v-col cols="4">
            <v-row>
              <p class="grey--text text--darken-2 font-weight-medium">
                {{ $t("view.partner.presentationRequests.title") }}
              </p>
            </v-row>
            <v-row>{{ $t("view.partner.presentationRequests.text") }}</v-row>
          </v-col>
          <v-col cols="8">
            <PresentationRequestList
              v-if="isReady"
              v-bind:presentationRequests="presentationRequests"
              v-bind:headers="headersPresentationRequest"
              v-on:removedItem="removePresentationRequest"
              v-on:responseSuccess="presentationRequestSuccess"
            ></PresentationRequestList>
          </v-col>
        </v-row>
      </v-card-text>

      <v-card-actions>
        <v-expansion-panels v-if="expertMode" accordion flat>
          <v-expansion-panel>
            <v-expansion-panel-header
              class="grey--text text--darken-2 font-weight-medium bg-light"
              >{{ $t("showRawData") }}</v-expansion-panel-header
            >
            <v-expansion-panel-content class="bg-light">
              <vue-json-pretty :data="rawData"></vue-json-pretty>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="attentionPartnerStateDialog" max-width="600px">
      <v-card>
        <v-card-title class="headline"
          >{{ $t("view.partner.stateDialog.title", { state: partner.state }) }}
        </v-card-title>

        <v-card-text>
          {{ $t("view.partner.stateDialog.text", { state: partner.state }) }}
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-bpa-button
            color="secondary"
            @click="attentionPartnerStateDialog = false"
            >{{ $t("view.partner.stateDialog.no") }}</v-bpa-button
          >

          <v-bpa-button color="primary" @click="proceed">{{
            $t("view.partner.stateDialog.yes")
          }}</v-bpa-button>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import Profile from "@/components/Profile";
import PresentationList from "@/components/PresentationList";
import PartnerStateIndicator from "@/components/PartnerStateIndicator";
import { CredentialTypes, PartnerStates } from "../constants";
import {
  getPartnerProfile,
  getPartnerName,
  getPartnerState,
} from "@/utils/partnerUtils";
import { EventBus } from "../main";
import {
  sentHeaders,
  receivedHeaders,
} from "@/components/tableHeaders/PartnerHeaders";
import { issuerService } from "@/services";
import CredExList from "@/components/CredExList";
import IssueCredential from "@/components/IssueCredential";
import PresentationRequestList from "@/components/PresentationRequestList";
import UpdatePartner from "@/components/UpdatePartner";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "Partner",
  props: ["id"],
  components: {
    VBpaButton,
    Profile,
    PresentationList,
    PartnerStateIndicator,
    CredExList,
    IssueCredential,
    PresentationRequestList,
    UpdatePartner,
  },
  created() {
    EventBus.$emit("title", this.$t("view.partner.title"));
    this.getPartner();
    this.getPresentationRecords();
    this.getIssuedCredentials(this.id);
    this.$store.commit("partnerSeen", { id: this.id });
  },
  data: () => {
    return {
      isReady: false,
      isBusy: false,
      isUpdatingDid: false,
      isLoading: true,
      attentionPartnerStateDialog: false,
      updatePartnerDialog: false,
      goTo: {},
      alias: "",
      did: "",
      partner: {},
      rawData: {},
      credentials: [],
      presentationsSent: [],
      presentationsReceived: [],
      issuedCredentials: [],
      presentationRequests: [],
      rules: {
        required: (value) => !!value || "Can't be empty",
      },
      headersSent: sentHeaders,
      headersReceived: receivedHeaders,
      PartnerStates: PartnerStates,
      headersIssued: [
        {
          text: "Type",
          value: "displayText",
        },
        {
          text: "Updated at",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
      ],
      issueCredentialDialog: false,
      headersPresentationRequest: [
        {
          text: "Schema",
          value:
            "proofRequest.requestedAttributes.attribute_group_0.restrictions[0].schema_id",
        },
        {
          text: "Received at",
          value: "sentAt", //miss labelled.
        },
        {
          text: "State",
          value: "state",
        },
        {
          text: "",
          value: "actions",
        },
      ],
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    isActive() {
      return this.partner.bpa_state === PartnerStates.ACTIVE_OR_RESPONSE;
    },
  },
  methods: {
    proceed() {
      this.attentionPartnerStateDialog = false;
      this.$router.push(this.goTo);
    },
    // Presentations
    requestPresentation() {
      if (this.isActive) {
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
      if (this.isActive) {
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
        .get(`${this.$apiBaseUrl}/partners/${this.id}/proof-exchanges`)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            let data = result.data;
            console.log(data);
            this.presentationsSent = data.filter((item) => {
              console.log("PresentationSent");
              return (
                item.role === "prover" &&
                [
                  "presentation_sent",
                  "presentation_acked",
                  "proposal_sent",
                ].includes(item.state)
              );
            });
            this.presentationRequests = data.filter((item) => {
              console.log("PresentationRequest");
              return (
                item.role === "prover" && item.state === "request_received"
              );
            });
            this.presentationsReceived = data.filter((item) => {
              console.log("PresentationReceived");
              return item.role === "verifier";
            });
            console.log(this.presentationRequests);
          }
        })
        .catch((e) => {
          console.error(e);
          // EventBus.$emit("error", e);
        });
      console.log(this.presentationsRequests);
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
    removePresentationRequest(id) {
      let objIndex = this.presentationRequests.findIndex((item) => {
        return item.id === id;
      });
      this.presentationRequests[objIndex].state = "presentation_rejected"; //not an aries state
    },

    presentationRequestSuccess(id) {
      let objIndex = this.presentationRequests.findIndex((item) => {
        return item.id === id;
      });
      this.presentationRequests[objIndex].state = "presentation_sent";
      this.presentationsSent.push(this.presentationRequests[objIndex]);

      this.presentationRequests = this.presentationRequests.filter((item) => {
        return item.id !== id;
      });
    },

    // Issue Credentials
    getIssuedCredentials(id) {
      console.log("Getting issued credential records...");
      issuerService
        .listCredentialExchangesAsIssuer(id)
        .then((result) => {
          if ({}.hasOwnProperty.call(result, "data")) {
            let data = result.data;
            this.issuedCredentials = data;
          }
        })
        .catch((e) => {
          console.error(e);
          // EventBus.$emit("error", e);
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

            this.partner.bpa_state = getPartnerState(this.partner);

            // Hacky way to define a partner name
            // Todo: Make this consistent. Probably in backend
            this.partner.name = getPartnerName(this.partner);
            this.alias = this.partner.name;
            this.did = this.partner.did;
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
    acceptPartnerRequest() {
      this.$axios
        .put(`${this.$apiBaseUrl}/partners/${this.id}/accept`, {})
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Connection request accepted");
            this.getPartner();
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
                  return cred.type !== CredentialTypes.PROFILE.type;
                });
              }

              // Hacky way to define a partner name
              // Todo: Make this consistent. Probably in backend
              this.partner.name = getPartnerName(this.partner);
              this.partner.bpa_state = getPartnerState(this.partner);
              this.alias = this.partner.name;
              this.did = this.partner.did;
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
    onUpdatePartner() {
      this.getPartner();
      this.updatePartnerDialog = false;
    },
    submitDidUpdate() {
      this.isBusy = true;
      if (this.did && this.did !== "") {
        this.$axios
          .put(`${this.$apiBaseUrl}/partners/${this.id}/did`, {
            did: this.did,
          })
          .then((result) => {
            if (result.status === 200) {
              this.isBusy = false;
              this.partner.did = this.did;
              this.isUpdatingDid = false;
            }
          })
          .catch((e) => {
            this.isBusy = false;
            this.isUpdatingDid = false;
            console.error(e);
            EventBus.$emit("error", e);
          });
      } else {
        this.isBusy = false;
      }
    },
    onCredentialIssued() {
      this.issueCredentialDialog = false;
      this.getIssuedCredentials(`${this.id}`);
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
