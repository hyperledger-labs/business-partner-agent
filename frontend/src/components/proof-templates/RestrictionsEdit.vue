<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <div>
    <v-container>
      <h4 class="pb-5">
        {{ $t("view.proofTemplate.restrictions.title") }}
      </h4>
      <v-row
        v-show="
          attributeGroup.ui.selectedRestrictionsByTrustedIssuer.length === 0
        "
      >
        <v-col>
          <v-alert type="info"
            >{{ $t("view.proofTemplate.restrictions.errorNoTrustedIssuer") }}
          </v-alert>
        </v-col>
      </v-row>
      <v-data-table
        show-select
        disable-sort
        :hide-default-footer="
          attributeGroup.schemaLevelRestrictions.length < 10
        "
        :headers="restrictionsHeaders"
        :items="attributeGroup.schemaLevelRestrictions"
        v-model="attributeGroup.ui.selectedRestrictionsByTrustedIssuer"
        item-key="issuerDid"
        class="elevation-1"
        show-expand
      >
        <template v-slot:expanded-item="{ headers, item }">
          <td :colspan="headers.length" style="padding: 0">
            <v-simple-table>
              <tbody>
                <tr>
                  <td>
                    {{ $t("view.proofTemplate.restrictions.schemaName") }}
                  </td>
                  <td>
                    <v-text-field
                      id="proofTemplateName"
                      v-model="item.schemaName"
                      dense
                    ></v-text-field>
                  </td>
                </tr>
                <tr>
                  <td>
                    {{ $t("view.proofTemplate.restrictions.schemaVersion") }}
                  </td>
                  <td>
                    <v-text-field
                      id="proofTemplateName"
                      v-model="item.schemaVersion"
                      dense
                    ></v-text-field>
                  </td>
                </tr>
                <tr>
                  <td>
                    {{ $t("view.proofTemplate.restrictions.schemaIssuerDid") }}
                  </td>
                  <td>
                    <v-text-field
                      id="proofTemplateName"
                      v-model="item.schemaIssuerDid"
                      dense
                    ></v-text-field>
                  </td>
                </tr>
                <tr>
                  <td>
                    {{ $t("view.proofTemplate.restrictions.trustedIssuerDid") }}
                  </td>
                  <td>
                    <v-text-field
                      disabled
                      id="proofTemplateName"
                      v-model="item.issuerDid"
                      dense
                    ></v-text-field>
                  </td>
                </tr>
                <tr>
                  <td>
                    {{
                      $t(
                        "view.proofTemplate.restrictions.credentialDefinitionId"
                      )
                    }}
                  </td>
                  <td>
                    <v-text-field
                      id="proofTemplateName"
                      v-model="item.credentialDefinitionId"
                      dense
                    ></v-text-field>
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </td>
        </template>
        <template v-slot:[`item.issuerLabel`]="{ item }">
          {{ getIssuerLabel(item) }}
        </template>
      </v-data-table>
    </v-container>
    <v-dialog
      v-model="addTrustedIssuerDialog.visible"
      :retain-focus="false"
      persistent
      max-width="600px"
    >
      <template v-slot:activator="{ on, attrs }">
        <v-bpa-button color="secondary" v-bind="attrs" v-on="on">{{
          $t("view.proofTemplate.restrictions.dialog.addTrustedIssuer")
        }}</v-bpa-button>
      </template>
      <v-card>
        <v-card-title class="headline">{{
          $t("view.proofTemplate.restrictions.dialog.addTrustedIssuer")
        }}</v-card-title>
        <v-card-text>
          <v-container>
            <v-text-field
              :label="$t('view.proofTemplate.restrictions.dialog.label')"
              :hint="$t('view.proofTemplate.restrictions.dialog.hint')"
              v-model="addTrustedIssuerDialog.did"
              persistent-hint
              outlined
            ></v-text-field>
          </v-container>
        </v-card-text>
        <v-card-actions>
          <v-layout align-end justify-end>
            <v-bpa-button color="secondary" @click="addTrustedIssuerCancel">
              {{ $t("button.cancel") }}
            </v-bpa-button>
            <v-bpa-button
              color="primary"
              :disabled="addTrustedIssuerDialog.did.length === 0"
              @click="addTrustedIssuerRestrictionObject(attributeGroup)"
            >
              {{ $t("button.create") }}
            </v-bpa-button>
          </v-layout>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
<script lang="ts">
import VBpaButton from "@/components/BpaButton";

export default {
  name: "RestrictionsEdit",
  components: { VBpaButton },
  props: {
    value: {},
  },
  data: () => {
    return {
      addTrustedIssuerDialog: {
        visible: false,
        did: "",
      },
    };
  },
  computed: {
    attributeGroup: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
    restrictionsHeaders() {
      return [
        {
          text: this.$t(
            "view.proofTemplate.restrictions.header.trustedIssuerLabel"
          ),
          value: "issuerLabel",
        },
        {
          text: this.$t(
            "view.proofTemplate.restrictions.header.trustedIssuerDid"
          ),
          value: "issuerDid",
        },
        {
          text: "",
          value: "data-table-expand",
        },
      ];
    },
    schemaTrustedIssuers() {
      const schemas = this.$store.getters.getSchemas.filter(
        (schema) => schema.type === "INDY"
      );

      let trustedIssuerArrays = [];

      for (const schema of schemas) {
        if (schema.trustedIssuer !== undefined) {
          for (const trustedIssuerElement of schema.trustedIssuer) {
            trustedIssuerArrays.push(trustedIssuerElement);
          }
        }
      }

      return trustedIssuerArrays;
    },
  },
  methods: {
    getIssuerLabel(restriction) {
      const filteredIssuers = this.schemaTrustedIssuers.find(
        (s) => s.issuerDid === restriction.issuerDid
      );

      return filteredIssuers !== undefined &&
        Object.prototype.hasOwnProperty.call(filteredIssuers, "label")
        ? filteredIssuers.label
        : "";
    },
    addTrustedIssuerCancel() {
      this.addTrustedIssuerDialog.visible = false;
      this.addTrustedIssuerDialog.did = "";
    },
    addTrustedIssuerRestrictionObject(attributeGroup) {
      attributeGroup.schemaLevelRestrictions.push({
        schemaId: "",
        schemaName: "",
        schemaVersion: "",
        schemaIssuerDid: "",
        issuerDid: this.addTrustedIssuerDialog.did,
        credentialDefinitionId: "",
      });

      this.addTrustedIssuerDialog.visible = false;
      this.addTrustedIssuerDialog.did = "";
    },
  },
};
</script>
