<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">{{$t("component.issueCredential.title")}}</v-card-title>
      <v-card-text>
        <v-select
          :label="$t('component.issueCredential.partnerLabel')"
          v-model="partner"
          :items="partnerList"
          outlined
          :disabled="this.$props.partnerId !== undefined"
          dense
        ></v-select>
        <v-select
          :label="$t('component.issueCredential.credDefLabel')"
          return-object
          v-model="credDef"
          :items="credDefList"
          outlined
          :disabled="this.$props.credDefId !== undefined"
          dense
          @change="credDefSelected"
        ></v-select>
        <v-card v-if="credDefLoaded && expertMode" class="my-4">
          <v-card-title class="bg-light" style="font-size: small">{{$t('component.issueCredential.expertLoad.title')}}
            <v-btn icon @click="showExpertLoad = !showExpertLoad" style="margin-left: auto;">
              <v-icon v-if="showExpertLoad">$vuetify.icons.up</v-icon>
              <v-icon v-else>$vuetify.icons.down</v-icon>
            </v-btn>
          </v-card-title>
          <v-expand-transition>
            <div v-show="showExpertLoad">
              <v-card-text>
                <v-row>
                  <v-textarea
                    rows="5"
                    outlined
                    dense
                    v-model="expertLoadData"
                    :placeholder="$t('component.issueCredential.expertLoad.dataPlaceholder')"
                ></v-textarea>
                </v-row>
                <v-row>
                  <v-file-input
                      v-model="expertLoadFile"
                      :label="$t('component.issueCredential.expertLoad.filePlaceholder')"
                      outlined
                      dense
                      @change="uploadExpertLoadFile"
                      accept="text/plain,application/json"
                      prepend-icon="$vuetify.icons.attachment"></v-file-input>
                </v-row>
              </v-card-text>
              <v-card-actions>
                <v-layout align-end justify-end>
                  <v-bpa-button color="secondary"
                                @click="clearExpertLoad()"
                                :disabled="!expertLoadEnabled">{{$t('component.issueCredential.expertLoad.buttons.clear')}}
                  </v-bpa-button>
                  <v-bpa-button color="primary"
                                @click="parseExpertLoad()"
                                :disabled="!expertLoadEnabled">{{$t('component.issueCredential.expertLoad.buttons.load')}}
                  </v-bpa-button>
                </v-layout>
              </v-card-actions>
            </div>
          </v-expand-transition>
        </v-card>
        <v-card v-if="credDefLoaded">
          <v-card-title class="bg-light" style="font-size: small">{{$t('component.issueCredential.attributesTitle')}}</v-card-title>
          <v-card-text>
            <v-row>
              <v-col>
                <v-text-field
                    v-for="field in credDef.fields"
                    :key="field.type"
                    :label="field.label"
                    v-model="credentialFields[field.type]"
                    placeholder=""
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
        <h4>{{$t('component.issueCredential.options.title')}}</h4>
        <v-col>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
                {{$t('component.issueCredential.options.useV2')}}
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
          <v-bpa-button color="secondary" @click="cancel()"
            >Cancel</v-bpa-button
          >
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
            :disabled="submitDisabled"
            >Submit</v-bpa-button
          >
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import Vue from "vue";
import { EventBus } from "@/main";
import { issuerService } from "@/services";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "IssueCredential",
  components: { VBpaButton },
  props: {
    partnerId: String,
    credDefId: String,
    open: Boolean,
  },
  mounted() {
    this.load();
  },
  data: () => {
    return {
      isLoading: false,
      isBusy: false,
      partner: {},
      credDef: {},
      credential: {},
      credentialFields: {},
      submitDisabled: true,
      useV2Credential: null,
      showExpertLoad: false,
      expertLoadData: '',
      expertLoadFile: null,
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    partnerList: {
      get() {
        return this.$store.getters.getPartnerSelectList;
      },
    },
    credDefList: {
      get() {
        return this.$store.getters.getCredDefSelectList;
      }
    },
    credDefLoaded: {
      get() {
        return this.credDef?.fields?.length;
      }
    },
    expertLoadEnabled() {
      return this.expertLoadData?.trim().length > 0;
    }
  },
  watch: {
    partnerId(val) {
      if (val) {
        this.partner = this.partnerList.find((p) => p.value === val);
      }
    },
    credDefId(val) {
      if (val) {
        this.credDef = this.credDefList.find((p) => p.value === val);
        this.credDefSelected();
      }
    },
    open(val) {
      if (val) {
        // load up our partner and cred def (if needed)
        if (!this.partner?.id) {
          this.partner = this.partnerList.find((p) => p.value === this.$props.partnerId);
        }
        // this will happen if the form was opened with credDefId and then is cancelled and re-opened with the same credDefId
        // the credDef is empty and won't initialize unless credDefId changes.
        if (!this.credDef?.fields) {
          this.credDef = this.credDefList.find((p) => p.value === this.$props.credDefId);
          this.credDefSelected();
        }
      }
    }
  },
  methods: {
    async load() {
      this.isLoading = true;
      this.partner = {};
      this.credDef = {};

      if (this.$props.partnerId) {
        this.partner = this.partnerList.find(
          (p) => p.value === this.$props.partnerId
        );
      }

      if (this.$props.credDefId) {
        this.credDef = this.credDefList.find((c) => c.value === this.$props.credDefId);
      }

      this.isLoading = false;
    },
    async issueCredential() {
      let exVersion = null;
      if (this.useV2Credential) {
        exVersion = "V2";
      }
      const data = {
        credDefId: this.credDef.id,
        partnerId: this.partner.id,
        document: this.credentialFields,
        exchangeVersion: exVersion,
      };
      try {
        const resp = await issuerService.issueCredentialSend(data);
        if (resp.status === 200) {
          return resp.data;
        }
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
          EventBus.$emit("success", this.$t("component.issueCredential.successMessage"));
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
      // clear out selected credential definition, will select (or have pre-populated) when re-open form.
      this.credDef = {};
      this.credentialFields = {};
      this.clearExpertLoad();
      this.$emit("cancelled");
    },
    credDefSelected() {
      this.credentialFields = {};
      this.credDef.fields.forEach((x) => Vue.set(this.credentialFields, x.type, ""));
      this.submitDisabled = true;
    },
    enableSubmit() {
      let enabled = false;
      if (this.credDef && this.credDef.fields && this.credDef.fields.length) {
        //ok, we have some fields to check.
        console.log(this.credentialFields);
        enabled = this.credDef.fields.some(
          (x) =>
            this.credentialFields[x.type] &&
            this.credentialFields[x.type]?.trim().length > 0
        );
      }
      this.submitDisabled = !enabled;
    },
    uploadExpertLoadFile(v) {
      this.expertLoadFile = v;
      if (v) {
        try {
          let reader = new FileReader();
          reader.readAsText(v, "UTF-8");
          reader.onload =  evt => {
            this.expertLoadData = evt.target.result;
          }
          reader.onerror = (evt) => {
            EventBus.$emit("error", `Error reading file '${v.name}'. ${evt.message}`);
          }
        } catch (e) {
          EventBus.$emit("error", `Error reading file '${v.name}'. ${e.message}`);
        }
      }
    },
    clearExpertLoad() {
      this.expertLoadData = "";
      this.expertLoadFile = null;
    },
    parseExpertLoad() {
      if (this.expertLoadData) {
        // ok, turn this text into json object...
        let jsonObject = undefined;
        try {
          jsonObject = JSON.parse(this.expertLoadData);
        } catch (e) {
          EventBus.$emit("error", `Could not convert to JSON.`);
        }

        if (jsonObject) {
          // see if we can populate the credential fields...
          this.credDef.fields.forEach((x) => {
            if (jsonObject[x.type] &&
              !(Object.prototype.toString.call(jsonObject[x.type]) === "[object Object]" ||
                Object.prototype.toString.call(jsonObject[x.type]) === "[object Function]")) {
              Vue.set(this.credentialFields, x.type, jsonObject[x.type].toString());
            }
          });
          this.enableSubmit();
        }
      }
    }
  },
};
</script>
