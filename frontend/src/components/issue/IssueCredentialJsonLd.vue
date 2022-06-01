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
          outlined
          :disabled="this.$props.partnerId !== undefined"
          dense
        ></v-select>
        <v-select
          :label="$t('component.issueCredential.credDefLabel')"
          return-object
          v-model="credDefJsonLd"
          :items="credListJsonLd"
          item-value="id"
          outlined
          :disabled="this.$props.credDefId !== undefined"
          dense
          @change="credDefSelected"
        >
          <template v-slot:item="data"
            >{{ data.item.label }} - {{ data.item.ldType }}</template
          >
          <template v-slot:selection="data"
            >{{ data.item.label }} - {{ data.item.ldType }}</template
          >
        </v-select>
        <v-card v-if="credDefLoaded">
          <v-card-title class="bg-light" style="font-size: small">{{
            $t("component.issueCredential.attributesTitle")
          }}</v-card-title>
          <v-card-text>
            <v-row>
              <v-col>
                <v-text-field
                  v-for="field in credDefJsonLd.fields"
                  :key="field.type"
                  :label="field.label"
                  v-model="credentialFields[field.type]"
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
import { IssueCredentialRequestJsonLd, issuerService } from "@/services";
import VBpaButton from "@/components/BpaButton";
import { CredentialTypes } from "@/constants";

export default {
  name: "IssueCredentialJsonLd",
  components: { VBpaButton },
  props: {
    partnerId: String,
    schemaId: String,
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
      partner: {},
      credDefJsonLd: {},
      credential: {},
      credentialFields: {},
      submitDisabled: true,
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
    // TODO: Find correct naming as it is not a credential definition nor a credential
    credListJsonLd: {
      get() {
        // TODO: Get JSON-LD "cred defs"
        let documentTypes = this.$store.getters.getSchemas;

        if (this.$store.getters.getOrganizationalProfile) {
          documentTypes = documentTypes.filter(
            (schema) => schema.type === CredentialTypes.JSON_LD.type
          );
        }

        return documentTypes;
      },
    },
    credDefLoaded: {
      get() {
        return this.credDefJsonLd?.fields?.length;
      },
    },
  },
  watch: {
    partnerId(value: string) {
      if (value) {
        this.partner = this.partnerList.find((p) => p.value === value);
      }
    },
    credDefId(value: string) {
      if (value) {
        this.credDefJsonLd = this.credListJsonLd.find((p) => p.value === value);
        this.credDefSelected();
      }
    },
    open(value: boolean) {
      if (value) {
        // load up our partner and cred def (if needed)
        if (!this.partner?.id) {
          this.partner = this.partnerList.find(
            (p) => p.value === this.$props.partnerId
          );
        }
        // this will happen if the form was opened with credDefId and then is cancelled and re-opened with the same credDefId
        // the credDef is empty and won't initialize unless credDefId changes.
        if (!this.credDefJsonLd?.fields) {
          this.credDefJsonLd = this.credListJsonLd.find(
            (p) => p.value === this.$props.credDefId
          );
          this.credDefSelected();
        }
      }
    },
  },
  methods: {
    async load() {
      this.isLoading = true;
      this.partner = {};
      this.credDefJsonLd = {};

      if (this.$props.partnerId) {
        this.partner = this.partnerList.find(
          (p) => p.value === this.$props.partnerId
        );
      }

      if (this.$props.credDefId) {
        this.credDefJsonLd = this.credListJsonLd.find(
          (c) => c.value === this.$props.credDefId
        );
      }

      this.isLoading = false;
    },
    async issueCredential() {
      let document: any = {};
      for (const x of this.credDefJsonLd.fields) document[x.type] = "";
      Object.assign(document, this.credentialFields);

      const data: IssueCredentialRequestJsonLd = {
        schemaId: "TODO", // TODO
        partnerId: this.partner.id,
        document: document,
      };
      try {
        const resp = await issuerService.issueCredentialSendJsonLd(data);

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
          EventBus.$emit(
            "success",
            this.$t("component.issueCredential.successMessage")
          );
          this.credDefJsonLd = {};
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
      this.credDefJsonLd = {};
      this.credentialFields = {};
      this.$emit("cancelled");
    },
    credDefSelected() {
      this.credentialFields = {};
      for (const x of this.credDefJsonLd.fields)
        Vue.set(this.credentialFields, x.type, "");
      this.submitDisabled = true;
    },
    enableSubmit() {
      let enabled = false;
      if (
        this.credDefJsonLd &&
        this.credDefJsonLd.fields &&
        this.credDefJsonLd.fields.length > 0
      ) {
        console.log(this.credentialFields);
        enabled = this.credDefJsonLd.fields.some(
          (x) =>
            this.credentialFields[x.type] &&
            this.credentialFields[x.type]?.trim().length > 0
        );
      }
      this.submitDisabled = !enabled;
    },
  },
};
</script>
