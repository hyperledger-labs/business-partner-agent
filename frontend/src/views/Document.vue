<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card v-if="isReady" class="mx-auto">
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        {{ schemaLabel }}
        <v-layout align-end justify-end>
          <v-btn
            v-if="this.id"
            depressed
            color="red"
            icon
            @click="deleteDocument()"
          >
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>
      <v-card-text>
        <OrganizationalProfile
          v-if="isProfile(intDoc.type)"
          v-model="document.documentData"
          ref="doc"
        ></OrganizationalProfile>
        <Credential
          v-else
          v-bind:document="document"
          ref="doc"
          @doc-field-changed="fieldModified"
          @doc-data-field-changed="documentDataFieldChanged"
        ></Credential>
        <v-divider></v-divider>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>{{
              $t("view.wallet.visibility.title")
            }}</v-list-item-title>
            <v-list-item-subtitle>{{
              $t("view.wallet.visibility.subtitle")
            }}</v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action>
            <v-switch
              :disabled="document.type === CredentialTypes.UNKNOWN.type"
              v-model="document.isPublic"
              @change="fieldModified()"
            ></v-switch>
          </v-list-item-action>
        </v-list-item>
        <v-divider></v-divider>

        <div
          v-if="
            !disableVerificationRequest && this.id && !isProfile(document.type)
          "
        >
          <v-list-item :disabled="docModified()">
            <v-tooltip right v-model="showTooltip">
              <template v-slot:activator="{ attrs }">
                <v-list-item-content>
                  <v-list-item-title>{{
                    $t("view.document.verification")
                  }}</v-list-item-title>
                  <v-list-item-subtitle>{{
                    $t("view.document.verificationSubtitle")
                  }}</v-list-item-subtitle>
                </v-list-item-content>
                <v-list-item-action>
                  <v-btn
                    v-bind="attrs"
                    icon
                    :to="{
                      name: 'RequestVerification',
                      params: { documentId: id, schemaId: intDoc.schemaId },
                    }"
                    :disabled="docModified()"
                  >
                    <v-icon color="grey">$vuetify.icons.next</v-icon>
                  </v-btn>
                </v-list-item-action>
              </template>
              <span>{{ $t("view.document.modified") }}</span>
            </v-tooltip>
          </v-list-item>
          <v-divider></v-divider>
        </div>
      </v-card-text>

      <v-card-actions>
        <v-layout align-center align-end justify-end>
          <v-switch
            v-if="expertMode && enableV2Switch"
            v-model="useV2Exchange"
            :label="$t('button.useV2')"
          ></v-switch>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="saveDocument(isProfile(intDoc.type))"
            >{{ getCreateButtonLabel }}</v-bpa-button
          >
          <v-bpa-button
            v-show="this.id && !isProfile(intDoc.type)"
            :loading="this.isBusy"
            color="primary"
            @click="saveDocument(!isProfile(intDoc.type))"
            >{{ $t("button.saveAndClose") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>

      <v-expansion-panels v-if="expertMode" accordion flat>
        <v-expansion-panel>
          <v-expansion-panel-header
            class="grey--text text--darken-2 font-weight-medium bg-light"
            >{{ $t("showRawData") }}</v-expansion-panel-header
          >
          <v-expansion-panel-content class="bg-light">
            <vue-json-pretty :data="document"></vue-json-pretty>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import { CredentialTypes } from "@/constants";
import OrganizationalProfile from "@/components/OrganizationalProfile.vue";
import Credential from "@/components/Credential.vue";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "Document",
  props: {
    id: {
      type: String,
      required: false,
    },
    type: {
      type: String,
      required: false,
    },
    schemaId: {
      type: String,
      required: false,
    },
    disableVerificationRequest: {
      type: Boolean,
      required: false,
      default: false,
    },
    enableV2Switch: {
      type: Boolean,
      required: false,
      default: false,
    },
    createButtonLabel: {
      type: String,
      default: undefined,
    },
  },
  created() {
    if (this.id && !this.type) {
      EventBus.$emit("title", this.$t("view.document.titleExisting"));
      this.getDocument();
    } else {
      EventBus.$emit("title", this.$t("view.document.titleNew"));
      this.document.type = this.type;
      this.document.schemaId = this.schemaId;
      this.document.isPublic = this.isProfile(this.document.type);
      this.isReady = true;
      this.intDoc = { ...this.document };
    }
  },
  data: () => {
    return {
      document: {},
      intDoc: {},
      isBusy: false,
      isReady: false,
      useV2Exchange: false,
      CredentialTypes,
      docChanged: false,
      credChanged: false,
      showTooltip: false,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    schemaLabel() {
      if (this.schemaId) {
        return this.$store.getters.getSchemaById(this.schemaId).label;
      } else if (this.type) {
        return this.$store.getters.getSchemaByType(this.type).label;
      }

      return "";
    },
    getCreateButtonLabel() {
      return this.createButtonLabel
        ? this.createButtonLabel
        : this.$t("button.save");
    },
  },
  watch: {},
  methods: {
    getDocument() {
      this.$axios
        .get(`${this.$apiBaseUrl}/wallet/document/${this.id}`)
        .then((result) => {
          if (Object.prototype.hasOwnProperty.call(result, "data")) {
            this.document = result.data;
            this.intDoc = { ...this.document };
            this.isReady = true;
            EventBus.$emit(
              "title",
              `${this.$t("view.document.titleEdit")} (${
                this.document.typeLabel
              })`
            );
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    saveDocument(closeDocument) {
      this.isBusy = true;
      if (this.id) {
        this.$axios
          .put(`${this.$apiBaseUrl}/wallet/document/${this.id}`, {
            document: this.document.documentData,
            isPublic: this.document.isPublic,
            label: this.isProfile(this.document.type)
              ? this.document.documentData.legalName
              : this.document.label,
            type: this.document.type,
            schemaId: this.document.schemaId,
          })
          .then((response) => {
            console.log(response);
            this.isBusy = false;
            if (closeDocument) {
              this.$router.go(-1);
            } else {
              this.$router.go(this.$router.currentRoute);
            }
            EventBus.$emit(
              "success",
              this.$t("view.document.eventSuccessSaveEdit")
            );
          })
          .catch((error) => {
            this.isBusy = false;
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          });

        // create new document
      } else {
        const documentForSave = {
          document: this.$refs.doc.intDoc.documentData,
          label: this.$refs.doc.intDoc.label,
          isPublic: this.document.isPublic,
          type: this.type,
          schemaId: this.schemaId,
        };
        this.$axios
          .post(`${this.$apiBaseUrl}/wallet/document`, documentForSave)
          .then((response) => {
            console.log(response);
            this.$emit("received-document-id", {
              documentId: response.data.id,
              useV2Exchange: this.useV2Exchange,
            });
            this.isBusy = false;
            this.$router.go(-1);
            EventBus.$emit(
              "success",
              this.$t("view.document.eventSuccessSaveNew")
            );
          })
          .catch((error) => {
            this.isBusy = false;
            EventBus.$emit("error", this.$axiosErrorMessage(error));
          });
      }
    },
    deleteDocument() {
      this.$axios
        .delete(`${this.$apiBaseUrl}/wallet/document/${this.id}`)
        .then((result) => {
          if (result.status === 200) {
            EventBus.$emit(
              "success",
              this.$t("view.document.eventSuccessDelete")
            );
            this.$router.go(-1);
          }
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
        });
    },
    cancel() {
      this.$router.go(-1);
    },
    isProfile(schemaType) {
      return !this.schemaId && schemaType === CredentialTypes.PROFILE.type;
    },
    fieldModified() {
      this.docChanged = !!Object.keys(this.intDoc).some((key) => {
        return this.document[key] !== this.intDoc[key];
      });
      this.docModified();
    },
    documentDataFieldChanged(credChanged) {
      this.credChanged = credChanged;
      this.docModified();
    },
    docModified() {
      this.showTooltip = this.docChanged || this.credChanged;
      return this.docChanged || this.credChanged;
    },
  },
  components: {
    VBpaButton,
    OrganizationalProfile,
    Credential,
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
