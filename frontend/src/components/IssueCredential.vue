<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-card class="mx-auto">
      <v-card-title class="bg-light"> Issue Credential </v-card-title>
      <v-card-text>
        <v-select
          label="Partner"
          v-model="partner"
          :items="partners"
          outlined
          :disabled="this.$props.partnerId !== undefined"
          dense
        ></v-select>
      </v-card-text>
      <v-card-text>
        <v-select
          label="Credential"
          return-object
          v-model="credDef"
          :items="credDefs"
          outlined
          :disabled="this.$props.credDefId !== undefined"
          dense
          @change="credDefSelected"
        ></v-select>
      </v-card-text>
      <v-card-text>
        <h4 v-if="credDef && credDef.fields && credDef.fields.length">
          Credential Content
        </h4>
        <v-row>
          <v-col>
            <v-text-field
              v-for="field in credDef.fields"
              :key="field.type"
              :label="field.label"
              placeholder=""
              :rules="[(v) => !!v || 'Item is required']"
              :required="field.required"
              outlined
              dense
              @change="fieldChanged(field.type, $event)"
            ></v-text-field>
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-text v-if="expertMode">
        <h4>
          Options
        </h4>
        <v-col>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
                Use V2 Protocol
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
import { EventBus } from "@/main";
import { issuerService, partnerService } from "@/services";
import * as textUtils from "@/utils/textUtils";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "IssueCredential",
  components: { VBpaButton },
  props: {
    partnerId: String,
    credDefId: String,
    partnerList: Array,
    credDefList: Array
  },
  mounted() {
    this.load();
  },
  data: () => {
    return {
      isLoading: false,
      isBusy: false,
      partners: [],
      credDefs: [],
      partner: {},
      credDef: {},
      credential: {},
      credentialFields: {},
      submitDisabled: true,
      useV2Credential: null
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    }
  },
  watch: {
    partnerId(val) {
      if (val) {
        this.partner = this.partners.find((p) => p.value === val);
      }
    },
    credDefId(val) {
      if (val) {
        this.credDef = this.credDefs.find((p) => p.value === val);
        this.credDefSelected();
      }
    },
    partnerList(val) {
      this.partners = Array.from(val);
    },
    credDefList(val) {
      this.credDefs = Array.from(val);
    },
  },
  methods: {
    async load() {
      this.isLoading = true;
      this.partners = [];
      this.partner = {};
      this.credDefs = [];
      this.credDef = {};

      // get partner list
      if (!this.$props.partnerList || this.$props.partnerList.length === 0) {
        const presp = await partnerService.listPartners();
        if (presp.status === 200) {
          this.partners = presp.data.map((p) => {
            return { value: p.id, text: p.name, ...p };
          });
        }
      } else {
        this.partners = Array.from(this.$props.partnerList);
      }
      if (this.$props.partnerId) {
        this.partner = this.partners.find(
          (p) => p.value === this.$props.partnerId
        );
      }

      if (!this.$props.credDefList || this.$props.credDefList.length === 0) {
        const cresp = await issuerService.listCredDefs();
        if (cresp.status === 200) {
          this.credDefs = cresp.data.map((c) => {
            return {
              value: c.id,
              text: c.displayText,
              fields: c.schema.schemaAttributeNames.map((key) => {
                return {
                  type: key,
                  label: textUtils.schemaAttributeLabel(key),
                };
              }),
              ...c,
            };
          });
        }
      } else {
        this.credDefs = Array.from(this.$props.credDefList);
      }
      if (this.$props.credDefId) {
        this.credDef = this.credDefs.find(
          (c) => c.value === this.$props.credDefId
        );
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
        exchangeVersion: exVersion
      };
      try {
        const resp = await issuerService.issueCredentialSend(data);
        if (resp.status === 200) {
          return resp.data;
        }
      } catch (error) {
        EventBus.$emit("error", error);
      }
    },
    async submit() {
      this.isBusy = true;
      try {
        const _credexId = await this.issueCredential();
        this.isBusy = false;
        if (_credexId) {
          EventBus.$emit("success", "Credential issued.");
          this.credDef = {};
          this.submitDisabled = true;
          this.$emit("success");
        }
      } catch (error) {
        this.isBusy = false;
        EventBus.$emit("error", error);
      }
    },
    cancel() {
      this.$emit("cancelled");
    },
    credDefSelected() {
      this.credentialFields = {};
      this.credDef.fields.forEach((x) => (this.credentialFields[x.type] = ""));
      this.submitDisabled = true;
    },

    fieldChanged(propertyName, event) {
      this.credentialFields[propertyName] = event;
      // once all fields are populated, then enable the submit button
      let allPopulated = false;
      if (this.credDef && this.credDef.fields && this.credDef.fields.length) {
        //ok, we have some fields to check.
        allPopulated = this.credDef.fields.every(
          (x) =>
            this.credentialFields[x.type] &&
            this.credentialFields[x.type].trim().length > 0
        );
      }
      this.submitDisabled = !allPopulated;
    },
  },
};
</script>
