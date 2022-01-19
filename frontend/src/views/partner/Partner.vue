<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-card class="mx-auto mb-4">
      <!-- Title Bar -->
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
          <v-btn icon disabled>
            <v-icon small dark>$vuetify.icons.identity</v-icon>
          </v-btn>
          <span
            class="grey--text text--darken-2 font-weight-medium text-caption pl-1 pr-4"
            >{{ partner.did }}</span
          >
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
      <!-- Title Bar -->
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
      </v-card-text>

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
    </v-card>

    <!-- Presentation Exchanges -->
    <v-card v-if="partner.ariesSupport" class="mb-4">
      <v-card-title class="bg-light">
        {{ $t("view.partner.presentationExchanges.title") }}
        <v-layout justify-end>
          <v-bpa-button
            color="primary"
            icon
            @click="refreshPresentationRecords"
          >
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout>
      </v-card-title>
      <v-progress-linear
        v-if="isLoadingPresExRecords"
        indeterminate
      ></v-progress-linear>
      <PresentationExList
        v-if="isReady"
        v-model="presentationExRecords"
        v-bind:openItemById="presExId"
        @changed="refreshPresentationRecords"
      />
      <v-card-actions>
        <v-bpa-button small color="secondary" @click="sendPresentation">{{
          $t("view.partner.presentationExchanges.button.send")
        }}</v-bpa-button>
        <v-bpa-button small color="primary" @click="requestPresentation">{{
          $t("view.partner.presentationExchanges.button.request")
        }}</v-bpa-button>
      </v-card-actions>
      <v-expansion-panels v-if="expertMode" accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >{{ $t("showRawData") }}</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="presentationExRecords"></vue-json-pretty>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card>

    <!-- Credential Exchanges -->
    <v-card v-if="partner.ariesSupport" class="mb-4">
      <v-card-title class="bg-light"
        >{{ $t("view.partner.credentialExchanges.title") }}
        <v-layout justify-end>
          <v-bpa-button
            color="primary"
            icon
            @click="refreshIssuedCredentialRecords"
          >
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout>
      </v-card-title>
      <v-progress-linear
        v-if="isLoadingCredExRecords"
        indeterminate
      ></v-progress-linear>
      <CredExList
        v-if="isReady"
        v-bind:items="issuedCredentials"
        header-role
        v-bind:openItemById="credExId"
        @changed="refreshIssuedCredentialRecords"
      ></CredExList>
      <v-card-actions>
        <v-dialog v-model="issueCredentialDialog" persistent max-width="600px">
          <template v-slot:activator="{ on, attrs }">
            <v-bpa-button color="secondary" small v-bind="attrs" v-on="on">{{
              $t("view.partner.credentialExchanges.button.issueCredential")
            }}</v-bpa-button>
          </template>
          <IssueCredential
            :partnerId="id"
            @success="onCredentialIssued"
            @cancelled="issueCredentialDialog = false"
          >
          </IssueCredential>
        </v-dialog>
        <v-bpa-button
          style="margin-left: 8px"
          small
          color="primary"
          @click="requestCredential"
          >{{
            $t("view.partner.credentialExchanges.button.requestCredential")
          }}</v-bpa-button
        >
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

<script lang="ts">
import Profile from "@/components/Profile.vue";
import PartnerStateIndicator from "@/components/PartnerStateIndicator.vue";
import { CredentialTypes, PartnerStates } from "@/constants";
import { getPartnerProfile, getPartnerState } from "@/utils/partnerUtils";
import { EventBus } from "@/main";
import { issuerService, partnerService } from "@/services";
import CredExList from "@/components/CredExList.vue";
import PresentationExList from "@/components/PresentationExList.vue";
import IssueCredential from "@/components/IssueCredential.vue";
import UpdatePartner from "@/components/UpdatePartner.vue";
import VBpaButton from "@/components/BpaButton";
import store from "@/store";

export default {
  name: "Partner",
  props: {
    id: String,
    presExId: String,
    credExId: String,
  },
  components: {
    VBpaButton,
    Profile,
    PresentationExList,
    PartnerStateIndicator,
    CredExList,
    IssueCredential,
    UpdatePartner,
  },
  created() {
    EventBus.$emit("title", this.$t("view.partner.title"));
    this.getPartner();
    this.getPresentationRecords();
    this.getIssuedCredentials(this.id);
    this.$store.commit("partnerNotificationSeen", { id: this.id });
  },
  data: () => {
    return {
      isReady: false,
      isBusy: false,
      isLoading: true,
      isLoadingCredExRecords: true,
      isLoadingPresExRecords: true,
      attentionPartnerStateDialog: false,
      updatePartnerDialog: false,
      goTo: {},
      alias: "",
      did: "",
      partner: {},
      rawData: {},
      credentials: [],
      presentationExRecords: [],
      issuedCredentials: [],
      PartnerStates: PartnerStates,
      issueCredentialDialog: false,
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
    refreshPresentationRecords() {
      this.getPresentationRecords();
    },
    getPresentationRecords() {
      console.log("Getting presentation records...");
      this.isLoadingPresExRecords = true;
      partnerService
        .getPresentationExRecords(this.id)
        .then((result) => {
          this.isLoadingPresExRecords = false;
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            let data = result.data;
            console.log(data);
            this.presentationExRecords = data;
          }
        })
        .catch((error) => {
          this.isLoadingPresExRecords = false;
          console.error(error);
        });
    },

    // Issue Credentials
    getIssuedCredentials(id) {
      console.log("Getting issued credential records...");
      this.isLoadingCredExRecords = true;
      issuerService
        .listCredentialExchanges(id)
        .then((result) => {
          this.isLoadingCredExRecords = false;
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.issuedCredentials = result.data;
          }
        })
        .catch((error) => {
          this.isLoadingCredExRecords = false;
          console.error(error);
        });
    },
    refreshIssuedCredentialRecords() {
      this.getIssuedCredentials(this.id);
    },
    requestCredential() {
      if (this.isActive) {
        this.$router.push({
          name: "RequestCredential",
          params: {
            id: this.id,
          },
        });
      } else {
        this.attentionPartnerStateDialog = true;
        this.goTo = {
          name: "RequestCredential",
          params: {
            id: this.id,
          },
        };
      }
    },
    getPartner() {
      console.log("Getting partner...");
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then((result) => {
          console.log(result);
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.rawData = result.data;
            this.partner = {
              ...result.data,

              profile: getPartnerProfile(result.data),
            };

            this.partner.bpa_state = getPartnerState(this.partner);
            this.alias = this.partner.name;
            this.did = this.partner.did;
            this.isReady = true;
            this.isLoading = false;

            console.log(this.partner);
          }
        })
        .catch((error) => {
          this.isLoading = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    deletePartner() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/partners/${this.id}`)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            store.dispatch("loadPartners");
            EventBus.$emit(
              "success",
              this.$t("view.partner.eventSuccessPartnerDelete")
            );
            this.$router.push({
              name: "Partners",
            });
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    acceptPartnerRequest() {
      this.$axios
        .put(`${this.$apiBaseUrl}/partners/${this.id}/accept`, {})
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.partner.eventSuccessConnectionAccepted")
            );
            // allow a little time for the partner state to change, so the remove/accept panel will not be displayed
            setTimeout(() => this.getPartner(), 1000);
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    refreshPartner() {
      this.isLoading = true;
      this.$axios
        .get(`${this.$apiBaseUrl}/partners/${this.id}/refresh`)
        .then(async (result) => {
          if (
            result.status === 200 &&
            Object.prototype.hasOwnProperty.call(result, "data")
          ) {
            console.log(result.data);
            this.rawData = result.data;
            this.partner = {
              ...result.data,

              profile: getPartnerProfile(result.data),
            };
            if (
              Object.prototype.hasOwnProperty.call(this.partner, "credential")
            ) {
              // Show only creds other than OrgProfile in credential list
              this.credentials = this.partner.credential.filter((cred) => {
                return cred.type !== CredentialTypes.PROFILE.type;
              });
            }

            this.partner.bpa_state = getPartnerState(this.partner);
            this.alias = this.partner.name;
            this.did = this.partner.did;
            console.log(this.partner);
            this.isReady = true;
            this.isLoading = false;
          }
        })
        .catch((error) => {
          this.isLoading = false;
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    onUpdatePartner() {
      this.getPartner();
      this.updatePartnerDialog = false;
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
