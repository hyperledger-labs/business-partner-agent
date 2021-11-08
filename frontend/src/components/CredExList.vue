<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

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
        </span>
        <v-icon v-if="isItemActive(item) && !item.revoked" color="green"
          >$vuetify.icons.check</v-icon
        >
        <v-icon v-else-if="isItemActive(item) && item.revoked"
          >$vuetify.icons.check</v-icon
        >
        <v-tooltip
          v-if="item.errorMsg && item.state === exchangeStates.PROBLEM"
          top
        >
          <template v-slot:activator="{ on, attrs }">
            <v-icon color="error" small v-bind="attrs" v-on="on">
              $vuetify.icons.connectionAlert
            </v-icon>
          </template>
          <span>{{ item.errorMsg }}</span>
        </v-tooltip>
      </template>
      <template v-slot:[`item.updatedAt`]="{ item }">
        {{ item.updatedAt | formatDateLong }}
      </template>
      <template v-slot:[`item.createdAt`]="{ item }">
        {{ item.createdAt | formatDateLong }}
      </template>
      <template v-slot:[`item.role`]="{ item }">
        {{ item.role }}
      </template>
      <template v-slot:[`item.revocable`]="{ item }">
        <v-icon
          v-if="item.revocable && item.revoked"
          :title="$t('component.credExList.table.iconCredRevoked')"
          >$vuetify.icons.revoked</v-icon
        >
        <v-icon
          v-else-if="
            isItemActive(item) &&
            item.revocable &&
            !item.revoked &&
            item.role === exchangeRoles.HOLDER
          "
          color="green"
          :title="$t('component.credExList.table.holderNotRevoked')"
          >$vuetify.icons.revoke</v-icon
        >
        <v-icon
          v-else-if="isItemActive(item) && !item.revocable"
          color="green"
          :title="$t('component.credExList.table.holderNotRevocable')"
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
    <v-dialog v-model="dialog" max-width="600px">
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
            v-if="document.credentialExchangeRole === exchangeRoles.ISSUER"
            :label="$t('component.credExList.dialog.credDefLabel')"
            return-object
            v-model="credDef"
            :items="credDefList"
            outlined
            disabled
            dense
          ></v-select>

          <!-- Timeline  -->
          <Timeline
            v-if="document.credentialStateToTimestamp"
            v-bind:timeEntries="
              Object.entries(document.credentialStateToTimestamp)
            "
          />

          <br />

          <v-card>
            <v-card-title class="bg-light" style="font-size: small"
              >{{ $t("component.credExList.dialog.attributesTitle") }}
              <v-layout
                align-center
                justify-end
                v-if="
                  document.credentialExchangeState ===
                  exchangeStates.PROPOSAL_RECEIVED
                "
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
        </v-card-text>
        <v-card-actions>
          <v-layout align-end justify-end>
            <v-bpa-button
              :color="
                document.credentialExchangeState ===
                  exchangeStates.PROPOSAL_RECEIVED ||
                document.credentialExchangeState ===
                  exchangeStates.OFFER_RECEIVED
                  ? 'secondary'
                  : 'primary'
              "
              @click="closeDialog"
              >{{ $t("button.close") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="
                document.credentialExchangeState ===
                exchangeStates.PROPOSAL_RECEIVED
              "
              color="secondary"
              @click="declineCredentialProposal(document.walletCredentialId)"
              >{{ $t("button.decline") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="
                document.credentialExchangeState ===
                exchangeStates.PROPOSAL_RECEIVED
              "
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
              v-if="
                document.credentialExchangeState ===
                exchangeStates.PROPOSAL_RECEIVED
              "
              color="primary"
              :disabled="
                !dialogEditCredentialIsInitialData || isEditModeCredential
              "
              :loading="isLoadingSendCounterOffer"
              @click="sendCounterOffer(true)"
              >{{ $t("button.accept") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="
                document.credentialExchangeState ===
                exchangeStates.OFFER_RECEIVED
              "
              color="secondary"
              @click="declineCredentialOffer(document.walletCredentialId)"
              >{{ $t("button.decline") }}</v-bpa-button
            >
            <v-bpa-button
              v-if="
                document.credentialExchangeState ===
                exchangeStates.OFFER_RECEIVED
              "
              color="primary"
              @click="acceptCredentialOffer(document.walletCredentialId)"
              >{{ $t("button.accept") }}</v-bpa-button
            >
          </v-layout>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>
<script>
import { issuerService } from "@/services";
import Cred from "@/components/Credential.vue";
import VBpaButton from "@/components/BpaButton";
import NewMessageIcon from "@/components/NewMessageIcon";
import Timeline from "@/components/Timeline";
import { EventBus } from "@/main";
import { CredentialExchangeRoles, CredentialExchangeStates } from "@/constants";

export default {
  props: {
    items: Array,
    headers: {
      type: Array,
      default: () => [
        {
          text: "",
          value: "indicator",
          sortable: false,
          filterable: false,
        },
        {
          text: "Type",
          value: "displayText",
        },
        {
          text: "Issued to",
          value: "partner.name",
        },
        {
          text: "Updated at",
          value: "updatedAt",
        },
        {
          text: "State",
          value: "state",
        },
        {
          text: "Revocation",
          value: "revocable",
        },
      ],
    },
    isActiveFn: {
      type: Function,
      default: (item) =>
        item.state === CredentialExchangeStates.CREDENTIAL_ISSUED ||
        item.state === CredentialExchangeStates.CREDENTIAL_ACKED ||
        item.state === CredentialExchangeStates.DONE,
    },
    isLoading: Boolean,
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
      exchangeStates: CredentialExchangeStates,
      exchangeRoles: CredentialExchangeRoles,
      document: {},
      partner: {},
      credDef: {},
      revoked: [],
    };
  },
  watch: {
    items(val) {
      console.log("Credential Exchange Item refresh");
      console.log(val);
    },
  },
  methods: {
    openItem(item) {
      this.dialog = true;
      this.partner = this.partnerList.find((p) => p.value === item.partner.id);
      this.credDef = this.credDefList.find((p) => p.value === item.credDef.id);

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
        credentialStateToTimestamp: item.stateToTimestamp,
        walletCredentialId: item.id,
      };

      this.$store.commit("credentialNotificationSeen", { id: item.id });
      this.$store.commit("credentialNotificationSeen", { id: item.id });
      this.$emit("openItem", item);
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
      this.dialog = false;
    },
    isItemActive(item) {
      return this.isActiveFn(item);
    },
    revokeCredential(id) {
      this.revoked.push(id);
      issuerService.revokeCredential(id);
    },
    acceptCredentialOffer(id) {
      issuerService.acceptCredentialOffer(id);
      this.closeDialog();
    },
    declineCredentialOffer(id) {
      issuerService.declineCredentialOffer(id);
      this.closeDialog();
    },
    declineCredentialProposal(id) {
      issuerService.declineCredentialProposal(id);
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
        attributes: this.document.credentialData,
      };

      issuerService
        .sendCredentialOffer(this.document.credentialExchangeId, counterOffer)
        .then((res) => {
          EventBus.$emit("success", this.$axiosErrorMessage(res));
          this.closeDialog();
        })
        .catch((err) => {
          EventBus.$emit("error", this.$axiosErrorMessage(err));
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
