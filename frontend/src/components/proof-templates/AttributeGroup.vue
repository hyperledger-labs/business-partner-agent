<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-container>
    <v-expansion-panels focusable>
      <v-expansion-panel
        class="my-5"
        v-for="(attributeGroup, idx) in requestData"
        :key="idx"
      >
        <v-expansion-panel-header>
          <span v-html="renderSchemaLabelId(attributeGroup)"></span>
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-container>
            <v-list-item
              v-if="Object.hasOwnProperty.call(attributeGroup, 'identifier')"
              class="ml-n4 mb-4"
            >
              <v-list-item-title>
                <strong>{{
                  $t("view.proofTemplate.view.attributeGroup.issuerTitle")
                }}</strong>
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ attributeGroup.identifier.issuerLabel }}
              </v-list-item-subtitle>
            </v-list-item>
            <h4 class="mb-4">
              {{ $t("view.proofTemplate.view.attributeGroup.titleAttributes") }}
            </h4>
            <v-data-table
              disable-sort
              :headers="attributeGroupHeaders"
              :items="attributeGroup.attributes"
              item-key="name"
              class="elevation-1"
              show-expand
              hide-default-footer
            >
              <template v-slot:expanded-item="{ headers, item }">
                <td :colspan="headers.length" style="padding: 0">
                  <v-data-table
                    disable-sort
                    class="sub-table elevation-0"
                    :headers="attributeConditionHeaders"
                    :items="item.conditions"
                    hide-default-footer
                  />
                </td>
              </template>
            </v-data-table>
          </v-container>

          <v-container v-if="attributeGroup.schemaLevelRestrictions.length > 0">
            <h4 class="mb-4">
              {{
                $t("view.proofTemplate.view.attributeGroup.titleRestrictions")
              }}
            </h4>
            <v-data-table
              disable-sort
              :hide-default-footer="
                attributeGroup.schemaLevelRestrictions.length < 10
              "
              :headers="restrictionsHeaders"
              :items="attributeGroup.schemaLevelRestrictions"
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
                            disabled
                            v-model="item.schemaName"
                            dense
                          ></v-text-field>
                        </td>
                      </tr>
                      <tr>
                        <td>
                          {{
                            $t("view.proofTemplate.restrictions.schemaVersion")
                          }}
                        </td>
                        <td>
                          <v-text-field
                            id="proofTemplateName"
                            disabled
                            v-model="item.schemaVersion"
                            dense
                          ></v-text-field>
                        </td>
                      </tr>
                      <tr>
                        <td>
                          {{
                            $t(
                              "view.proofTemplate.restrictions.schemaIssuerDid"
                            )
                          }}
                        </td>
                        <td>
                          <v-text-field
                            id="proofTemplateName"
                            disabled
                            v-model="item.schemaIssuerDid"
                            dense
                          ></v-text-field>
                        </td>
                      </tr>
                      <tr>
                        <td>
                          {{
                            $t(
                              "view.proofTemplate.restrictions.trustedIssuerDid"
                            )
                          }}
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
                            disabled
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
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-container>
</template>
<script lang="ts">
export default {
  props: {
    requestData: Array,
  },
  computed: {
    attributeGroupHeaders() {
      return [
        {
          text: this.$t("view.proofTemplate.attributes.header.name"),
          value: "name",
        },
        {
          text: this.$t("view.proofTemplate.attributes.header.value"),
          value: "value",
        },
      ];
    },
    attributeConditionHeaders() {
      return [
        {
          text: this.$t("view.proofTemplate.attributes.header.operator"),
          value: "operator",
        },
        {
          text: this.$t("view.proofTemplate.attributes.header.value"),
          value: "value",
        },
      ];
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
    renderSchemaLabelId(attributeGroup) {
      // FIXME: This needs refactoring
      // This tries to show a schema and label but will show the attribute group if
      let schemaId;
      let internalSchemaId;
      if (attributeGroup.schemaId) {
        internalSchemaId = attributeGroup.schemaId;
      } else if (
        attributeGroup.schemaLevelRestrictions &&
        attributeGroup.schemaLevelRestrictions.schemaId
      ) {
        schemaId = attributeGroup.schemaLevelRestrictions.schemaId;
      }

      let schema;
      if (schemaId) {
        schema = this.$store.getters.getSchemas.find(
          (s) => s.schemaId === schemaId
        );
      } else if (internalSchemaId) {
        schema = this.$store.getters.getSchemas.find(
          (s) => s.id === internalSchemaId
        );
      }

      if (schema) {
        return `<strong>${schema.label}</strong><em>&nbsp;(${schema.schemaId})</em>`;
      } else if (schemaId) {
        return schemaId;
      } else {
        return attributeGroup.attributeGroupName;
      }
    },
  },
};
</script>
