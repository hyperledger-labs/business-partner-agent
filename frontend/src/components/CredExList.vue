<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container>
    <v-data-table
      :loading="isLoading"
      :hide-default-footer="items.length < 10"
      :headers="headers"
      :items="items"
      :sort-by="['updatedAt']"
      :sort-desc="[true]"
      single-select
      @click:row="openItem"
    >
      <template v-slot:[`item.indicator`]="{ item }">
        <new-message-icon :type="'credential'" :id="item.id"></new-message-icon>
      </template>
      <template v-slot:[`item.state`]="{ item }">
        <span>
          {{ (item.state ? item.state.replace("_", " ") : "") | capitalize }}
          <v-icon
            v-if="isItemActive(item) && !item.revoked"
            class="iconHeight"
            color="green"
            >$vuetify.icons.check</v-icon
          >
          <v-icon v-else-if="isItemActive(item) && item.revoked"
            >$vuetify.icons.check</v-icon
          >
          <v-tooltip v-if="item.errorMsg && stateIsProblemOrDeclined(item)" top>
            <template v-slot:activator="{ on, attrs }">
              <v-icon
                color="error"
                class="iconHeight"
                small
                v-bind="attrs"
                v-on="on"
              >
                $vuetify.icons.connectionAlert
              </v-icon>
            </template>
            <span>{{ item.errorMsg }}</span>
          </v-tooltip>
        </span>
      </template>
      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>
      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>
      <template v-slot:[`item.role`]="{ item }">
        {{ item.role | capitalize }}
      </template>
      <template v-slot:[`item.revocable`]="{ item }">
        <span v-if="item.revocable && item.revoked"
          >{{ $t("component.credExList.table.revoked") }}
          <v-icon
            :title="$t('component.credExList.table.iconCredRevoked')"
            class="iconHeight"
            >$vuetify.icons.revoked</v-icon
          >
        </span>
        <v-icon
          v-else-if="
            isItemActive(item) &&
            item.revocable &&
            !item.revoked &&
            item.role === exchangeRoles.HOLDER
          "
          color="green"
          :title="$t('component.credExList.table.holderNotRevoked')"
          class="iconHeight"
          >$vuetify.icons.revoke</v-icon
        >
        <v-icon
          v-else-if="isItemActive(item) && !item.revocable"
          color="green"
          :title="$t('component.credExList.table.holderNotRevocable')"
          class="iconHeight"
          >$vuetify.icons.check</v-icon
        >
        <v-btn
          small
          v-else-if="item.revocable && item.role === exchangeRoles.ISSUER"
          color="green"
          :title="$t('component.credExList.table.iconRevokeCred')"
          @click.stop="revokeCredential(item.id)"
          :disabled="revoked.includes(item.id)"
        >
          <v-icon left>$vuetify.icons.revoke</v-icon>
          {{ $t("button.revoke") }}
        </v-btn>
        <span v-else></span>
      </template>
    </v-data-table>
    <v-dialog v-model="dialog" max-width="600px" persistent>
      <v-card>
        <v-card-title class="bg-light">
          <span class="headline">{{
            $t("component.credExList.dialog.title")
          }}</span>
        </v-card-title>
        <v-card-text>
          <v-select
            :label="$t('component.credExList.dialog.partnerLabel')"
            v-model="partner"
            :items="partnerList"
            outlined
            disabled
            dense
          ></v-select>
          <v-select
            v-if="documentRoleIsIssuer"
            :label="$t('component.credExList.dialog.credDefLabel')"
            return-object
            v-model="credDef"
            :items="credDefList"
            outlined
            :disabled="!documentStateIsProposalReceived"
            dense
          ></v-select>

          <!-- Timeline  -->
          <Timeline
            v-if="document.credentialStateToTimestamp"
            :time-entries="document.credentialStateToTimestamp"
          />

          <br />

          <v-card>
            <v-card-title class="bg-light" style="font-size: small"
              >{{ $t("component.credExList.dialog.attributesTitle") }}
              <v-layout
                align-center
                justify-end
                v-if="documentStateIsProposalReceived"
              >
                <div v-if="isEditModeCredential">
                  <v-btn
                    icon
                    :disabled="
                      !dialogEditCredentialIsModified ||
                      dialogEditCredentialHasEmptyField
                    "
                    @click="saveCredentialEdit"
                    color="primary"
                  >
                    <v-icon dark>$vuetify.icons.save</v-icon>
                  </v-btn>

                  <v-btn icon @click="resetCredentialEdit" color="error">
                    <v-icon dark>$vuetify.icons.cancel</v-icon>
                  </v-btn>
                </div>
                <v-btn
                  v-if="!isEditModeCredential"
                  icon
                  @click="isEditModeCredential = true"
                  color="primary"
                >
                  <v-icon dark>$vuetify.icons.pencil</v-icon>
                </v-btn>
              </v-layout>
            </v-card-title>

            <v-card-text>
              <Cred
                :document="document"
                :isReadOnly="!isEditModeCredential"
                showOnlyContent
              ></Cred>
            </v-card-text>
          </v-card>
          <v-text-field
            v-if="
              documentStateIsOfferReceived || documentStateIsProposalReceived
            "
            v-model="declineReasonText"
            :label="$t('component.credExList.dialog.declineReasonLabel')"
            counter="255"
          ></v-text-field>
        </v-card-text>
        <v-card-actions>
          <v-layout align-end justify-end>
            <v-bpa-button
              :color="
                documentStateIsProposalReceived ||
                documentStateIsOfferReceived ||
                documentStateIsRevokedAndRoleIsIssuer
                  ? 'secondary'
                  : 'primary'
              "
              @click="closeDialog"
              >{{ $t("button.close") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsProposalReceived"
              color="secondary"
              @click="declineCredentialProposal(document.walletCredentialId)"
              >{{ $t("button.decline") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsProposalReceived"
              color="primary"
              :disabled="
                dialogEditCredentialIsInitialData ||
                isEditModeCredential ||
                !dialogEditCredentialIsModified
              "
              :loading="isLoadingSendCounterOffer"
              @click="sendCounterOffer(false)"
              >{{ $t("button.sendCounterOffer") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsProposalReceived"
              color="primary"
              :disabled="
                !dialogEditCredentialIsInitialData || isEditModeCredential
              "
              :loading="isLoadingSendCounterOffer"
              @click="sendCounterOffer(true)"
              >{{ $t("button.accept") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsOfferReceived"
              color="secondary"
              @click="declineCredentialOffer(document.walletCredentialId)"
              >{{ $t("button.decline") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsOfferReceived"
              color="primary"
              @click="acceptCredentialOffer(document.walletCredentialId)"
              >{{ $t("button.accept") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="documentStateIsRevokedAndRoleIsIssuer"
              color="primary"
              @click="reIssueCredential(document.walletCredentialId)"
              >{{ $t("button.reissue") }}</v-bpa-button
            >
          </v-layout>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<style scoped>
.iconHeight {
  display: inherit;
}
</style>
<script lang="ts">
import { issuerService } from "@/services";
import Cred from "@/components/Credential.vue";
import VBpaButton from "@/components/BpaButton";
import NewMessageIcon from "@/components/NewMessageIcon.vue";
import Timeline from "@/components/Timeline.vue";
import { EventBus } from "@/main";
import { CredentialExchangeRoles, CredentialExchangeStates } from "@/constants";

export default {
  props: {
    items: Array,
    isActiveFn: {
      type: Function,
      default: (item) =>
        item.state === CredentialExchangeStates.CREDENTIAL_ISSUED ||
        item.state === CredentialExchangeStates.CREDENTIAL_ACKED ||
        item.state === CredentialExchangeStates.DONE,
    },
    isLoading: Boolean,
    headerRole: {
      type: Boolean,
      default: false,
    },
    openItemById: String,
  },
  created() {
    this.$store.dispatch("loadPartnerSelectList");
    this.$store.dispatch("loadCredDefSelectList");
  },
  mounted() {
    // Open Item directly. Is used for links from notifications/activity
    if (this.openItemById) {
      // FIXME: items observable is typically not resolved yet. Then items is empty
      const item = this.items.find((item) => item.id === this.openItemById);
      if (item) {
        this.openItem(item);
      } else {
        // Load record separately if items have not be resolved
        issuerService.getCredExRecord(this.openItemById).then((resp) => {
          if (resp.data) {
            this.openItem(resp.data);
          }
        });
      }
    }
  },
  computed: {
    headers() {
      return [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: this.$t("component.credExList.headers.displayText"),
          value: "displayText",
        },
        {
          text:
            this.headerRole === true
              ? this.$t("component.credExList.headers.role")
              : this.$t("component.credExList.headers.partnerName"),
          value: this.headerRole === true ? "role" : "partner.name",
        },
        {
          text: this.$t("component.credExList.headers.updatedAt"),
          value: "updatedAt",
        },
        {
          text: this.$t("component.credExList.headers.state"),
          value: "state",
        },
        {
          text: this.$t("component.credExList.headers.revocable"),
          value: "revocable",
        },
      ];
    },
    partnerList: {
      get() {
        return this.$store.getters.getPartnerSelectList;
      },
    },
    credDefList: {
      get() {
        return this.$store.getters.getCredDefSelectList;
      },
    },
    documentStateIsProposalReceived() {
      return (
        this.document.credentialExchangeState ===
        CredentialExchangeStates.PROPOSAL_RECEIVED
      );
    },
    documentStateIsOfferReceived() {
      return (
        this.document.credentialExchangeState ===
        CredentialExchangeStates.OFFER_RECEIVED
      );
    },
    documentRoleIsIssuer() {
      return (
        this.document.credentialExchangeRole === CredentialExchangeRoles.ISSUER
      );
    },
    documentStateIsRevokedAndRoleIsIssuer() {
      return (
        this.document.credentialExchangeState ===
          CredentialExchangeStates.REVOKED &&
        this.document.credentialExchangeRole === CredentialExchangeRoles.ISSUER
      );
    },
    dialogEditCredentialIsInitialData: function () {
      return (
        JSON.stringify(this.document.credentialData) ===
        JSON.stringify(this.document.credentialInitialData)
      );
    },
    dialogEditCredentialIsModified: function () {
      return !!(
        JSON.stringify(this.document.credentialData) !==
          JSON.stringify(this.document.credentialUnchangedData) ||
        this.document.credentialWasEdited
      );
    },
    dialogEditCredentialHasEmptyField: function () {
      for (const attribute in this.document.credentialData) {
        if (this.document.credentialData[attribute] === "") {
          return true;
        }
      }
      return false;
    },
  },
  data: () => {
    return {
      dialog: false,
      isEditModeCredential: false,
      isLoadingSendCounterOffer: false,
      credentialContentChanged: false,
      declineReasonText: "",
      exchangeRoles: CredentialExchangeRoles,
      document: {},
      partner: {},
      credDef: {},
      revoked: [],
    };
  },
  watch: {
    items(value) {
      console.log("Credential Exchange Item refresh");
      console.log(value);
    },
  },
  methods: {
    openItem(item) {
      this.dialog = true;
      this.partner = this.partnerList.find((p) => p.value === item.partner.id);
      this.credDef = this.credDefList.find((p) => p.value === item.credDef.id);

      const credentialStateToTimestamp = Object.entries(item.stateToTimestamp);
      for (const stateElement of credentialStateToTimestamp) {
        if (
          (item.errorMsg &&
            stateElement[0] === CredentialExchangeStates.DECLINED) ||
          stateElement[0] === CredentialExchangeStates.PROBLEM
        ) {
          stateElement.push(item.errorMsg);
        } else {
          stateElement.push(undefined);
        }
      }

      this.document = {
        credentialData: { ...item.credential.attrs },
        credentialInitialData: { ...item.credential.attrs },
        credentialUnchangedData: { ...item.credential.attrs },
        schemaId: item.credential.schemaId,
        credentialDefinitionId: item.credential.credentialDefinitionId,
        credentialExchangeId: item.id,
        credentialExchangeState: item.state,
        credentialExchangeRole: item.role,
        credentialWasEdited: false,
        credentialStateToTimestamp,
        walletCredentialId: item.id,
      };

      this.$store.commit("credentialNotificationSeen", { id: item.id });
      this.$store.commit("credentialNotificationSeen", { id: item.id });
      this.$emit("openItem", item);
    },
    stateIsProblemOrDeclined(item) {
      return (
        item.state === CredentialExchangeStates.DECLINED ||
        item.state === CredentialExchangeStates.PROBLEM
      );
    },
    resetCredentialEdit() {
      Object.assign(
        this.document.credentialData,
        this.document.credentialUnchangedData
      );
      this.isEditModeCredential = false;
    },
    saveCredentialEdit() {
      this.document.credentialWasEdited = true;
      Object.assign(
        this.document.credentialUnchangedData,
        this.document.credentialData
      );
      this.isEditModeCredential = false;
    },
    closeDialog() {
      this.resetCredentialEdit();
      this.declineReasonText = "";
      this.$emit("changed");
      this.dialog = false;
    },
    isItemActive(item) {
      return this.isActiveFn(item);
    },
    revokeCredential(id) {
      this.revoked.push(id);
      issuerService.revokeCredential(id);
    },
    async acceptCredentialOffer(id) {
      await issuerService.acceptCredentialOffer(id);
      this.closeDialog();
    },
    async declineCredentialOffer(id) {
      await issuerService.declineCredentialOffer(id, this.declineReasonText);
      this.closeDialog();
    },
    async declineCredentialProposal(id) {
      await issuerService.declineCredentialProposal(id, this.declineReasonText);
      this.closeDialog();
    },
    async reIssueCredential(id) {
      await issuerService.reIssueCredential(id);
      this.closeDialog();
    },
    async sendCounterOffer(acceptAll) {
      this.isLoadingSendCounterOffer = true;

      let acceptProposal = false;

      if (acceptAll) {
        acceptProposal = acceptAll;
      }

      const counterOffer = {
        acceptProposal,
        credDefId: this.credDef.credentialDefinitionId,
        attributes: this.document.credentialData,
      };

      issuerService
        .sendCredentialOffer(this.document.credentialExchangeId, counterOffer)
        .then((response) => {
          EventBus.$emit("success", this.$axiosErrorMessage(response));
          this.closeDialog();
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        })
        .finally(() => {
          this.isLoadingSendCounterOffer = false;
        });
    },
  },
  components: {
    VBpaButton,
    Cred,
    NewMessageIcon,
    Timeline,
  },
};
</script>
