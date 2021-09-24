<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="my-4">
      <v-card-title class="bg-light">{{
        $t("view.issueCredentials.cards.action.title")
      }}</v-card-title>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-autocomplete
            :label="$t('view.issueCredentials.cards.action.partnerLabel')"
            v-model="partner"
            :items="partnerList"
            return-object
            class="mx-4"
            flat
            single-line
            hide-no-data
            hide-details
            dense
            outlined
            clearable
            clear-icon="$vuetify.icons.delete"
          >
            <template v-slot:item="data">
              <v-icon
                x-small
                class="ml-2 mr-2"
                :color="partnerStateColor(data.item)"
                >$vuetify.icons.partnerState</v-icon
              >
              {{ data.item.text }}
            </template>
            <template v-slot:selection="data">
              <v-icon
                x-small
                class="ml-2 mr-2"
                :color="partnerStateColor(data.item)"
                >$vuetify.icons.partnerState</v-icon
              >
              {{ data.item.text }}
            </template>
          </v-autocomplete>
          <v-autocomplete
            :label="$t('view.issueCredentials.cards.action.credDefLabel')"
            v-model="credDef"
            :items="credDefList"
            return-object
            class="mx-4"
            flat
            single-line
            hide-no-data
            hide-details
            dense
            outlined
            clearable
            clear-icon="$vuetify.icons.delete"
          ></v-autocomplete>
          <v-dialog
            v-model="issueCredentialDialog"
            persistent
            max-width="600px"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-bpa-button
                v-bind="attrs"
                v-on="on"
                color="primary"
                :disabled="issueCredentialDisabled"
                >Issue Credential</v-bpa-button
              >
            </template>
            <IssueCredential
              :credDefId="credDefId"
              :partnerId="partnerId"
              :open="issueCredentialDialog"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false"
            >
            </IssueCredential>
          </v-dialog>
        </v-layout>
      </v-card-actions>
    </v-card>
    <v-card class="my-4">
      <v-card-title class="bg-light"
        >{{ $t("view.issueCredentials.cards.table.title")
        }}<v-layout justify-end>
          <v-bpa-button color="primary" icon @click="loadCredentials">
            <v-icon dark>$vuetify.icons.refresh</v-icon>
          </v-bpa-button>
        </v-layout>
      </v-card-title>
      <v-card-text>
        <CredExList
          :items="issuedCredentials"
          :is-loading="isLoadingCredentials"
        ></CredExList>
      </v-card-text>
      <v-card-actions> </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import { issuerService } from "@/services";
import CredExList from "@/components/CredExList";
import IssueCredential from "@/components/IssueCredential";
import * as partnerUtils from "@/utils/partnerUtils";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "CredentialManagement",
  components: {
    VBpaButton,
    IssueCredential,
    CredExList,
  },
  created() {
    EventBus.$emit("title", this.$t("view.issueCredentials.title"));
    this.loadCredentials();
  },
  data: () => {
    return {
      isLoadingCredentials: false,
      issuedCredentials: [],
      partner: {},
      partnerId: "",
      credDef: {},
      credDefId: "",
      issueCredentialDisabled: true,
      issueCredentialDialog: false,
      addSchemaDialog: false,
      createSchemaDialog: false,
    };
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
  },
  watch: {
    partner(val) {
      this.issueCredentialDisabled =
        !val || !val.id || !this.credDef || !this.credDef.id;
      this.partnerId = val ? val.id : "";
    },
    credDef(val) {
      this.issueCredentialDisabled =
        !val || !val.id || !this.partner || !this.partner.id;
      this.credDefId = val ? val.id : "";
    },
  },
  methods: {
    partnerStateColor(p) {
      return partnerUtils.getPartnerStateColor(p.state);
    },
    async loadCredentials() {
      this.isLoadingCredentials = true;
      this.issuedCredentials = [];
      this.partner = {};
      this.credDef = {};

      const iresp = await issuerService.listCredentialExchangesAsIssuer();
      if (iresp.status === 200) {
        this.issuedCredentials = iresp.data;
      }
      this.isLoadingCredentials = false;
    },
    credentialIssued() {
      this.issueCredentialDialog = false;
      this.$store.dispatch("loadPartnerSelectList");
      this.$store.dispatch("loadCredDefSelectList");
      this.loadCredentials();
    },
  },
};
</script>
