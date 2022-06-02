<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

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
          <v-switch
            style="display: block; height: 35px"
            dense
            inset
            class="mx-2"
            v-model="useJsonLd"
            label="JSON-LD"
          ></v-switch>
          <v-autocomplete
            :label="$t('view.issueCredentials.cards.action.credDefLabel')"
            v-model="credDef"
            item-value="id"
            :items="credDefOrSchemasList"
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
            <template v-slot:item="data" v-if="useJsonLd"
              >{{ data.item.label }} - {{ data.item.ldType }}</template
            >
            <template v-slot:item="data" v-else>{{
              data.item.displayText
            }}</template>
            <template v-slot:selection="data" v-if="useJsonLd"
              >{{ data.item.label }} - {{ data.item.ldType }}</template
            >
            <template v-slot:selection="data" v-else>{{
              data.item.displayText
            }}</template>
          </v-autocomplete>
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
                >{{
                  $t("view.issueCredentials.cards.action.button")
                }}</v-bpa-button
              >
            </template>
            <IssueCredentialJsonLd
              v-if="!issueOutOfBoundCredential && useJsonLd"
              :schema-id="schemaJsonLdId"
              :partner-id="partnerId"
              :open="issueCredentialDialog"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false"
            >
            </IssueCredentialJsonLd>
            <IssueCredentialIndyOob
              v-else-if="issueOutOfBoundCredential && !useJsonLd"
              :credDefId="credDefId"
              :open="issueCredentialDialog"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false"
            ></IssueCredentialIndyOob>
            <IssueCredentialIndy
              v-else
              :credDefId="credDefId"
              :partnerId="partnerId"
              :open="issueCredentialDialog"
              @success="credentialIssued"
              @cancelled="issueCredentialDialog = false"
            >
            </IssueCredentialIndy>
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
        <CredExList ref="credExList" asIssuer />
      </v-card-text>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import CredExList from "@/components/CredExList.vue";
import * as partnerUtils from "@/utils/partnerUtils";
import VBpaButton from "@/components/BpaButton";
import IssueCredentialIndyOob from "@/components/issue/IssueCredentialIndyOob.vue";
import IssueCredentialIndy from "@/components/issue/IssueCredentialIndy.vue";
import { CredentialTypes } from "@/constants";
import IssueCredentialJsonLd from "@/components/issue/IssueCredentialJsonLd.vue";

export default {
  name: "CredentialManagement",
  components: {
    IssueCredentialJsonLd,
    VBpaButton,
    IssueCredentialIndy,
    IssueCredentialIndyOob,
    CredExList,
  },
  created() {
    EventBus.$emit("title", this.$t("view.issueCredentials.title"));
  },
  data: () => {
    return {
      isLoadingCredentials: false,
      issuedCredentials: [],
      partner: {},
      partnerId: "",
      credDef: {},
      credDefId: "",
      schemaJsonLd: {},
      schemaJsonLdId: "",
      issueCredentialDisabled: true,
      issueCredentialDialog: false,
      issueOutOfBoundCredential: false,
      addSchemaDialog: false,
      createSchemaDialog: false,
      useJsonLd: false,
    };
  },
  computed: {
    partnerList: {
      get() {
        return [
          ...(!this.useJsonLd
            ? [
                {
                  text: this.$t(
                    "view.issueCredentials.cards.action.invitationWithAttachmentLabel"
                  ),
                  id: "invitationWithAttachment",
                },
                { divider: true },
              ]
            : []),
          ...this.$store.getters.getPartnerSelectList,
        ];
      },
    },
    credDefOrSchemasList: {
      get() {
        if (this.useJsonLd) {
          const documentTypes = this.$store.getters.getSchemas;

          return documentTypes.filter(
            (schema) => schema.type === CredentialTypes.JSON_LD.type
          );
        } else {
          return this.$store.getters.getCredDefSelectList;
        }
      },
    },
    // TODO
    credDefIndyList: {
      get() {
        return this.$store.getters.getCredDefSelectList;
      },
    },
    // TODO
    schemasJsonLdList: {
      get() {
        const documentTypes = this.$store.getters.getSchemas;

        return documentTypes.filter(
          (schema) => schema.type === CredentialTypes.JSON_LD.type
        );
      },
    },
  },
  watch: {
    partner(value) {
      const isNotSelected: boolean = !value || !value.id;
      this.issueCredentialDisabled =
        isNotSelected || !this.credDef || !this.credDef.id;

      this.issueOutOfBoundCredential =
        !isNotSelected && value.id === "invitationWithAttachment";

      this.partnerId = value ? value.id : "";
    },
    credDef(value) {
      this.issueCredentialDisabled =
        !value || !value.id || !this.partner || !this.partner.id;
      this.credDefId = value ? value.id : "";
    },
    schemaJsonLd(value) {
      // TODO
      this.schemaJsonLdId = value ? value.id : "";
    },
  },
  methods: {
    partnerStateColor(p) {
      return partnerUtils.getPartnerStateColor(p.state);
    },
    async loadCredentials() {
      this.partner = {};
      this.credDef = {};
      await this.$refs.credExList.loadCredentials();
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
