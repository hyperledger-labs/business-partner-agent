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
          :disabled="this.partnerId !== undefined"
          dense
        ></v-select>
        <v-select
          :label="$t('component.issueCredential.credDefLabel')"
          return-object
          v-model="schemaJsonLd"
          :items="schemasJsonLd"
          item-value="id"
          outlined
          :disabled="this.schemaId !== undefined"
          dense
          @change="schemaSelected"
        >
          <template v-slot:item="data"
            >{{ data.item.label }} - {{ data.item.ldType }}</template
          >
          <template v-slot:selection="data"
            >{{ data.item.label }} - {{ data.item.ldType }}</template
          >
        </v-select>
        <v-card v-if="schemaLoaded">
          <v-card-title class="bg-light" style="font-size: small">{{
            $t("component.issueCredential.attributesTitle")
          }}</v-card-title>
          <v-card-text>
            <v-row>
              <v-col>
                <v-text-field
                  v-for="field in schemaJsonLd.schemaAttributeNames"
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
      schemaJsonLd: {},
      credential: {},
      credentialFields: {},
      submitDisabled: true,
    };
  },
  computed: {
    partnerList: {
      get() {
        return this.$store.getters.getPartnerSelectList;
      },
    },
    schemasJsonLd: {
      get() {
        let documentTypes = this.$store.getters.getSchemas;

        if (this.$store.getters.getOrganizationalProfile) {
          documentTypes = documentTypes.filter(
            (schema) => schema.type === CredentialTypes.JSON_LD.type
          );
        }

        return documentTypes;
      },
    },
    schemaLoaded: {
      get() {
        return this.schemaJsonLd?.schemaAttributeNames?.length;
      },
    },
  },
  watch: {
    partnerId(value: string) {
      if (value) {
        this.partner = this.partnerList.find((p) => p.value === value);
      }
    },
    schemaId(value: string) {
      if (value) {
        this.schemaJsonLd = this.schemasJsonLd.find((p) => p.id === value);
        this.schemaSelected();
      }
    },
    open(value: boolean) {
      if (value) {
        if (!this.partner?.id) {
          this.partner = this.partnerList.find(
            (p) => p.value === this.partnerId
          );
        }
        if (!this.schemaJsonLd?.schemaAttributeNames) {
          this.schemaJsonLd = this.schemasJsonLd.find(
            (p) => p.id === this.schemaId
          );
          this.schemaSelected();
        }
      }
    },
  },
  methods: {
    async load() {
      this.isLoading = true;
      this.partner = {};
      this.schemaJsonLd = {};

      if (this.partnerId) {
        this.partner = this.partnerList.find((p) => p.value === this.partnerId);
      }

      if (this.schemaId) {
        this.schemaJsonLd = this.schemasJsonLd.find(
          (c) => c.id === this.schemaId
        );
      }

      this.isLoading = false;
    },
    async issueCredential() {
      let document: any = {};
      for (const x of this.schemaJsonLd.schemaAttributeNames) document[x] = "";
      Object.assign(document, this.credentialFields);

      const data: IssueCredentialRequestJsonLd = {
        schemaId: this.schemaJsonLd.id,
        partnerId: this.partner.id,
        document: document,
        type: CredentialTypes.JSON_LD.type,
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
          this.schemaJsonLd = {};
          this.submitDisabled = true;
          this.$emit("success");
        }
      } catch (error) {
        this.isBusy = false;
        EventBus.$emit("error", this.$axiosErrorMessage(error));
      }
    },
    cancel() {
      this.schemaJsonLd = {};
      this.credentialFields = {};
      this.$emit("cancelled");
    },
    schemaSelected() {
      this.credentialFields = {};
      for (const x of this.schemaJsonLd.schemaAttributeNames)
        Vue.set(this.credentialFields, x, "");
      this.submitDisabled = true;
    },
    enableSubmit() {
      let enabled = false;
      if (
        this.schemaJsonLd &&
        this.schemaJsonLd.schemaAttributeNames &&
        this.schemaJsonLd.schemaAttributeNames.length > 0
      ) {
        enabled = this.schemaJsonLd.schemaAttributeNames.some(
          (x) =>
            this.credentialFields[x] &&
            this.credentialFields[x]?.trim().length > 0
        );
      }
      this.submitDisabled = !enabled;
    },
  },
};
</script>
