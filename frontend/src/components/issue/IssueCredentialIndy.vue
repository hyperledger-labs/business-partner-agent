<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title v-show="!hideTitle" class="bg-light">{{
        $t("component.issueCredential.title")
      }}</v-card-title>
      <v-card-text>
        <v-select
          :label="$t('component.issueCredential.partnerLabel')"
          v-model="partner"
          :items="partnerList"
          item-value="id"
          item-text="name"
          outlined
          :disabled="this.$props.partnerId !== undefined"
          dense
        ></v-select>
        <v-select
          :label="$t('component.issueCredential.credDefLabel')"
          return-object
          v-model="credDef"
          item-value="id"
          item-text="displayText"
          :items="credDefList"
          outlined
          :disabled="this.$props.credDefId !== undefined"
          dense
          @change="credDefSelected"
        ></v-select>
        <v-card v-if="credDefLoaded && expertMode" class="my-4">
          <v-card-title class="bg-light" style="font-size: small"
            >{{ $t("component.issueCredential.expertLoad.title") }}
            <v-btn
              icon
              @click="expertLoad.show = !expertLoad.show"
              style="margin-left: auto"
            >
              <v-icon v-if="expertLoad.show">$vuetify.icons.up</v-icon>
              <v-icon v-else>$vuetify.icons.down</v-icon>
            </v-btn>
          </v-card-title>
          <v-expand-transition>
            <div v-show="expertLoad.show">
              <v-card-text>
                <v-row>
                  <v-textarea
                    rows="5"
                    outlined
                    dense
                    v-model="expertLoad.data"
                    :placeholder="
                      $t('component.issueCredential.expertLoad.dataPlaceholder')
                    "
                  ></v-textarea>
                </v-row>
                <v-row>
                  <v-file-input
                    v-model="expertLoad.file"
                    :label="
                      $t('component.issueCredential.expertLoad.filePlaceholder')
                    "
                    outlined
                    dense
                    @change="uploadExpertLoadFile"
                    :accept="expertLoad.fileAccept"
                    prepend-icon="$vuetify.icons.attachment"
                  ></v-file-input>
                </v-row>
              </v-card-text>
              <v-card-actions>
                <v-layout align-start justify-start>
                  <v-radio-group
                    v-model="expertLoad.type"
                    row
                    @change="expertLoadTypeChanged"
                  >
                    <v-radio label="JSON" value="json"></v-radio>
                    <v-radio label="CSV" value="csv"></v-radio>
                  </v-radio-group>
                </v-layout>
                <v-layout align-end justify-end>
                  <v-bpa-button
                    color="secondary"
                    @click="clearExpertLoad()"
                    :disabled="!expertLoadEnabled"
                    >{{
                      $t("component.issueCredential.expertLoad.buttons.clear")
                    }}
                  </v-bpa-button>
                  <v-bpa-button
                    color="primary"
                    @click="parseExpertLoad()"
                    :disabled="!expertLoadEnabled"
                    >{{
                      $t("component.issueCredential.expertLoad.buttons.load")
                    }}
                  </v-bpa-button>
                </v-layout>
              </v-card-actions>
            </div>
          </v-expand-transition>
        </v-card>
        <v-card v-if="credDefLoaded">
          <v-card-title class="bg-light" style="font-size: small">{{
            $t("component.issueCredential.attributesTitle")
          }}</v-card-title>
          <v-card-text>
            <v-row>
              <v-col>
                <v-text-field
                  v-for="field in credDef.schema.schemaAttributeNames"
                  :key="field"
                  :label="field"
                  v-model="credentialFields[field]"
                  outlined
                  dense
                  @blur="enableSubmit"
                  @keyup="enableSubmit"
                ></v-text-field>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-card-text>
      <v-card-text v-if="expertMode">
        <h4>{{ $t("component.issueCredential.options.title") }}</h4>
        <v-col>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title
                class="grey--text text--darken-2 font-weight-medium"
              >
                {{ $t("button.useV2") }}
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch v-model="useV2Credential"></v-switch>
            </v-list-item-action>
          </v-list-item>
        </v-col>
      </v-card-text>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()">{{
            $t("button.cancel")
          }}</v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
            :disabled="submitDisabled"
            >{{ $t("button.submit") }}</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script lang="ts">
import Vue from "vue";
import { EventBus } from "@/main";
import {
  CredDef,
  IssueCredentialRequestIndy,
  issuerService,
  PartnerAPI,
} from "@/services";
import VBpaButton from "@/components/BpaButton";
import * as textUtils from "@/utils/textUtils";
import * as CSV from "csv-string";
import { ExchangeVersion } from "@/constants";

export default {
  name: "IssueCredentialIndy",
  components: { VBpaButton },
  props: {
    partnerId: String,
    credDefId: String,
    open: Boolean,
    hideTitle: {
      type: Boolean,
      default: false,
    },
  },
  mounted() {
    this.load();
  },
  data: () => {
    return {
      isLoading: false,
      isBusy: false,
      partner: {} as PartnerAPI,
      credDef: {} as CredDef,
      credential: {},
      credentialFields: {},
      submitDisabled: true,
      useV2Credential: undefined as boolean,
      expertLoad: {
        show: false,
        data: "",
        file: undefined as File,
        type: "json",
        fileAccept: "text/plain,application/json",
      },
    };
  },
  computed: {
    expertMode() {
      return this.$store.getters.getExpertMode;
    },
    partnerList: {
      get(): PartnerAPI[] {
        return this.$store.getters.getPartnerSelectList;
      },
    },
    credDefList: {
      get(): CredDef[] {
        return this.$store.getters.getCredDefSelectList;
      },
    },
    credDefLoaded: {
      get() {
        return this.credDef?.schema?.schemaAttributeNames?.length;
      },
    },
    expertLoadEnabled() {
      return this.expertLoad.data?.trim().length > 0;
    },
  },
  watch: {
    partnerId(value: string) {
      if (value) {
        this.partner = this.partnerList.find((p: PartnerAPI) => p.id === value);
      }
    },
    credDefId(value: string) {
      if (value) {
        this.credDef = this.credDefList.find((p: CredDef) => p.id === value);
        this.credDefSelected();
      }
    },
    open(value: boolean) {
      if (value) {
        // load up our partner and cred def (if needed)
        if (!this.partner?.id) {
          this.partner = this.partnerList.find(
            (p: PartnerAPI) => p.id === this.$props.partnerId
          );
        }
        // this will happen if the form was opened with credDefId and then is cancelled and re-opened with the same credDefId
        // the credDef is empty and won't initialize unless credDefId changes.
        if (!this.credDef?.schema?.schemaAttributeNames) {
          this.credDef = this.credDefList.find(
            (p: CredDef) => p.id === this.$props.credDefId
          );
          this.credDefSelected();
        }
        this.clearExpertLoad();
        this.expertLoad.show = false;
      }
    },
  },
  methods: {
    async load() {
      this.isLoading = true;
      this.partner = {};
      this.credDef = {};

      if (this.$props.partnerId) {
        this.partner = this.partnerList.find(
          (p: PartnerAPI) => p.id === this.$props.partnerId
        );
      }

      if (this.$props.credDefId) {
        this.credDef = this.credDefList.find(
          (c: CredDef) => c.id === this.$props.credDefId
        );
      }

      this.isLoading = false;
    },
    async issueCredential() {
      let exVersion: ExchangeVersion;
      let document: any = {};

      if (this.useV2Credential) {
        exVersion = ExchangeVersion.V2;
      }

      for (const x of this.credDef.schema.schemaAttributeNames)
        document[x] = "";
      Object.assign(document, this.credentialFields);

      const data: IssueCredentialRequestIndy = {
        credDefId: this.credDef.id,
        partnerId: this.partner.id,
        document: document,
        exchangeVersion: exVersion,
      };
      try {
        const resp = await issuerService.issueCredentialSendIndy(data);
        return resp.data;
      } catch (error) {
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    async submit() {
      this.isBusy = true;
      try {
        const _credexId = await this.issueCredential();
        this.isBusy = false;
        if (_credexId) {
          EventBus.$emit(
            "success",
            this.$t("component.issueCredential.successMessage")
          );
          this.credDef = {};
          this.submitDisabled = true;
          this.$emit("success");
        }
      } catch (error) {
        this.isBusy = false;
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    cancel() {
      this.credDef = {};
      this.credentialFields = {};
      this.clearExpertLoad();
      this.$emit("cancelled");
    },
    credDefSelected() {
      this.credentialFields = {};
      for (const x of this.credDef.schema.schemaAttributeNames)
        Vue.set(this.credentialFields, x, "");
      this.submitDisabled = true;
    },
    enableSubmit() {
      let enabled = false;
      if (
        this.credDef &&
        this.credDef.schema &&
        this.credDef.schema.schemaAttributeNames &&
        this.credDef.schema.schemaAttributeNames.length > 0
      ) {
        enabled = this.credDef.schema.schemaAttributeNames.some(
          (attributeName: string) =>
            this.credentialFields[attributeName] &&
            this.credentialFields[attributeName]?.trim().length > 0
        );
      }
      this.submitDisabled = !enabled;
    },
    expertLoadTypeChanged(value: string) {
      this.expertLoad.fileAccept =
        value === "json"
          ? "text/plain,application/json"
          : "text/plain,text/csv";
    },
    uploadExpertLoadFile(file: File) {
      this.expertLoad.file = file;
      if (file) {
        try {
          let reader = new FileReader();
          reader.readAsText(file, "UTF-8");
          reader.addEventListener("load", (event_) => {
            this.expertLoad.data = event_.target.result;
          });
          reader.addEventListener("error", () => {
            EventBus.$emit(
              "error",
              `${this.$t(
                "component.issueCredential.expertLoad.errorMessages.readFile"
              )} '${file.name}'.`
            );
          });
        } catch (error) {
          EventBus.$emit(
            "error",
            `${this.$t(
              "component.issueCredential.expertLoad.errorMessages.readFile"
            )} '${file.name}'. ${error.message}`
          );
        }
      }
    },
    clearExpertLoad() {
      this.expertLoad = {
        show: true,
        data: "",
        file: undefined,
        type: "json",
        fileAccept: "text/plain,application/json",
      };
    },
    parseExpertLoad() {
      if (this.expertLoad.data) {
        let object;
        let formatErrorMessage = this.$t(
          "component.issueCredential.expertLoad.errorMessages.format.json"
        );
        if (this.expertLoad.type === "json") {
          object = this.jsonToObject(this.expertLoad.data);
        } else {
          formatErrorMessage = this.$t(
            "component.issueCredential.expertLoad.errorMessages.format.csv"
          );
          object = this.csvToObject(this.expertLoad.data);
        }

        if (object) {
          let count = 0;
          for (const x of this.credDef.schema.schemaAttributeNames) {
            if (
              object[x] &&
              !(
                Object.prototype.toString.call(object[x]) ===
                  "[object Object]" ||
                Object.prototype.toString.call(object[x]) ===
                  "[object Function]"
              )
            ) {
              count = count + 1;
              Vue.set(this.credentialFields, x, object[x].toString());
            }
          }
          if (count) {
            this.enableSubmit();
          } else {
            EventBus.$emit(
              "error",
              this.$t(
                "component.issueCredential.expertLoad.errorMessages.attributes"
              )
            );
          }
        } else {
          let errorMessage = this.$t(
            "component.issueCredential.expertLoad.errorMessages.parse"
          );
          EventBus.$emit("error", `${errorMessage} ${formatErrorMessage}`);
        }
      }
    },
    jsonToObject(data: any) {
      let object;
      if (data && Object.prototype.toString.call(data) === "[object String]") {
        try {
          object = JSON.parse(data);
        } catch {
          console.log("Error converting JSON string to Object");
        }
      }
      return object;
    },
    csvToObject(data: any) {
      let object: any;
      if (data && Object.prototype.toString.call(data) === "[object String]") {
        try {
          const array = CSV.parse(data);
          if (array?.length > 1) {
            const names = array[0];
            const values = array[1];
            const namesOk = names.every((value) =>
              textUtils.isValidSchemaAttributeName(value)
            );
            if (namesOk) {
              object = {};
              for (const [index, a] of names.entries()) {
                object[a] = values[index];
              }
            }
          }
        } catch {
          console.log("Error converting CSV string to Object");
        }
      }
      return object;
    },
  },
};
</script>
