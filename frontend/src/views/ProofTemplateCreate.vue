<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<style scoped>
.sub-table.theme--light.v-data-table {
  background: transparent;
}

.sub-table .v-data-table-header {
  display: none;
}
</style>

<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>Create Proof Template</span>
      </v-card-title>

      <!-- Proof Templates Table -->
      <!-- Basic Data -->
      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateName"
            v-model="proofTemplate.name"
            dense
            label="Name"
            :append-icon="'$vuetify.icons.copy'"
          ></v-text-field>
        </v-list-item>
      </v-container>

      <v-divider />

      <!-- Attribute Groups -->
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title>Data to be requested</v-list-item-title>
          <v-list-item-subtitle
            >Add data to be requested by Schema</v-list-item-subtitle
          >

          <v-container>
            <v-expansion-panels focusable>
              <v-expansion-panel
                class="my-5"
                v-for="(attributeGroup, idx) in proofTemplate.attributeGroups"
                :key="idx"
              >
                <v-expansion-panel-header>
                  <span v-html="renderSchemaLabelId(attributeGroup)"></span>
                </v-expansion-panel-header>
                <v-expansion-panel-content>
                  <v-container>
                    <v-row>
                      <v-col cols="4" class="pb-10">
                        <h4 class="pb-5">Data fields</h4>
                      </v-col>
                    </v-row>
                    <v-data-table
                      disable-sort
                      :headers="attributeGroupHeaders"
                      :items="attributeGroup.attributes"
                      item-key="name"
                      class="elevation-1"
                      show-expand
                      hide-default-footer
                    >
                      <!-- actions on attribute -->
                      <template v-slot:item.actions="{ item: attribute }">
                        <v-btn icon @click="deleteAttribute(idx, attribute)">
                          <v-icon color="error">$vuetify.icons.delete</v-icon>
                        </v-btn>
                      </template>

                      <!-- expanded section for attribute conditions -->
                      <template
                        v-slot:expanded-item="{
                          attributeGroupHeaders,
                          item: attribute,
                        }"
                      >
                        <td :colspan="attributeConditionHeaders.length">
                          <v-data-table
                            disable-sort
                            class="sub-table elevation-0"
                            :headers="attributeConditionHeaders"
                            :items="attribute.conditions"
                            hide-default-footer
                          >
                            <template v-slot:item="{ item: condition }">
                              <tr>
                                <td>
                                  <v-select
                                    :items="operators"
                                    v-model="condition.operator"
                                    dense
                                  />
                                </td>
                                <td>
                                  <v-text-field
                                    v-model="condition.value"
                                    dense
                                  />
                                </td>
                                <td>
                                  <v-btn
                                    icon
                                    @click="addCondition(idx, attribute.name)"
                                  >
                                    <v-icon color="success"
                                      >$vuetify.icons.add</v-icon
                                    >
                                  </v-btn>
                                  <v-btn
                                    icon
                                    @click="
                                      deleteCondition(
                                        idx,
                                        attribute.name,
                                        condition.operator
                                      )
                                    "
                                  >
                                    <v-icon color="error"
                                      >$vuetify.icons.delete</v-icon
                                    >
                                  </v-btn>
                                </td>
                              </tr>
                            </template>
                          </v-data-table>
                        </td>
                      </template>
                    </v-data-table>

                    <!-- add new attribute -->
                    <v-container>
                      <v-menu>
                        <template v-slot:activator="{ on, attrs }">
                          <v-btn
                            color="primary"
                            dark
                            small
                            bottom
                            left
                            fab
                            v-bind="attrs"
                            v-on="on"
                          >
                            <v-icon>$vuetify.icons.add</v-icon>
                          </v-btn>
                        </template>
                        <v-list>
                          <v-list-item
                            v-for="attributeName in schemas.find(
                              (s) => s.id === attributeGroup.schemaId
                            ).schemaAttributeNames"
                            :key="attributeName"
                            :disabled="
                              attributeGroup.attributes.some(
                                (existingAttribute) =>
                                  existingAttribute.name === attributeName
                              )
                            "
                            @click="addAttribute(idx, attributeName)"
                          >
                            <v-list-item-title>{{
                              attributeName
                            }}</v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-menu>
                    </v-container>
                  </v-container>

                  <!-- Schema Restrictions -->
                  <v-container>
                    <h4 class="pb-5">Restrictions</h4>
                    <v-simple-table>
                      <tbody>
                        <tr>
                          <td>Schema ID</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions.schemaId
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Name</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions
                                  .schemaName
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Version</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions
                                  .schemaVersion
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Issuer DID</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions
                                  .schemaIssuerDid
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                        <tr>
                          <td>Issuer DID</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions.issuerDid
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                        <tr>
                          <td>Credential Definition ID</td>
                          <td>
                            <v-text-field
                              id="proofTemplateName"
                              v-model="
                                attributeGroup.schemaLevelRestrictions
                                  .credentialDefinitionId
                              "
                              dense
                            ></v-text-field>
                          </td>
                        </tr>
                      </tbody>
                    </v-simple-table>
                  </v-container>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-container>

          <!-- add new attribute group -->
          <v-container>
            <v-menu>
              <template v-slot:activator="{ on, attrs }">
                <v-btn
                  color="primary"
                  dark
                  small
                  absolute
                  bottom
                  left
                  fab
                  v-bind="attrs"
                  v-on="on"
                >
                  <v-icon>$vuetify.icons.add</v-icon>
                </v-btn>
              </template>
              <v-list>
                <v-list-item
                  v-for="(schema, idx) in schemas"
                  :key="idx"
                  @click="addAttributeGroup(schema.id)"
                >
                  <v-list-item-title>
                    {{ schema.label }}<i>&nbsp;({{ schema.schemaId }})</i>
                  </v-list-item-title>
                </v-list-item>
              </v-list>
            </v-menu>
          </v-container>
        </v-list-item-content>
      </v-list-item>

      <!-- Proof Templates Actions -->
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="$router.go(-1)">
            Cancel
          </v-bpa-button>
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="createProofTemplate"
          >
            {{ createButtonLabel }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import proofTemplateService from "@/services/proofTemplateService";

export default {
  name: "ProofTemplates",
  props: {
    attributeGroupHeaders: {
      type: Array,
      default: () => [
        {
          text: "name",
          value: "name",
        },
        {
          text: "actions",
          value: "actions",
        },
      ],
    },
    attributeConditionHeaders: {
      type: Array,
      default: () => [
        {
          text: "operator",
          value: "operator",
        },
        {
          text: "value",
          value: "value",
        },
        {
          text: "actions",
          value: "actions",
        },
      ],
    },
    createButtonLabel: {
      type: String,
      default: "Create",
    },
    disableRouteBack: {
      type: Boolean,
      default: false,
    },
  },
  components: { VBpaButton },
  created() {
    EventBus.$emit("title", "Proof Templates");
  },
  mounted() {
    // load condition operators (>, <, ==, etc)
    proofTemplateService.getKnownConditionOperators().then((result) => {
      this.operators = result.data;
    });
  },
  data: () => {
    return {
      isBusy: false,
      operators: [],
      proofTemplate: {
        name: "",
        attributeGroups: [],
      },
    };
  },
  computed: {
    schemas() {
      return this.$store.getters.getSchemas.filter(
        (schema) => schema.type === "INDY"
      );
    },
  },
  watch: {},
  methods: {
    renderSchemaLabelId(attributeGroup) {
      const schema = this.$store.getters.getSchemas.find(
        (s) => s.id === attributeGroup.schemaId
      );
      return `${schema.label}<em>&nbsp;(${schema.schemaId})</em>`;
    },
    addAttributeGroup(schemaId) {
      // add a blank attribute group template
      this.proofTemplate.attributeGroups.push({
        schemaId: schemaId,
        nonRevoked: true,
        attributes: [],
        schemaLevelRestrictions: {},
      });
    },
    addAttribute(idx, attributeName) {
      console.log(`adding attribute ${attributeName} to idx ${idx}`);
      this.proofTemplate.attributeGroups[idx].attributes.push({
        name: attributeName,
        conditions: [
          {
            operator: "",
            value: "",
          },
        ],
      });
    },
    deleteAttribute(attributeGroupIdx, attribute) {
      let attributes = this.proofTemplate.attributeGroups[attributeGroupIdx]
        .attributes;
      let attributeIdx = attributes.findIndex((a) => a.name === attribute.name);
      attributes.splice(attributeIdx, 1);
    },
    addCondition(idx, attributeName) {
      this.proofTemplate.attributeGroups[idx].attributes
        .find((a) => a.name === attributeName)
        .conditions.push({
          operator: "",
          value: "",
        });
    },
    deleteCondition(idx, attributeName, operator) {
      let conditions = this.proofTemplate.attributeGroups[idx].attributes.find(
        (a) => a.name === attributeName
      ).conditions;
      let operatorIdx = conditions.findIndex((c) => c.operator === operator);

      if (conditions.length > 1) {
        conditions.splice(operatorIdx, 1);
      } else {
        conditions[0].operator = "";
        conditions[0].value = "";
      }
    },
    async createProofTemplate() {
      this.isBusy = true;

      // sanitize attribute conditions (remove empty conditions)
      this.proofTemplate.attributeGroups.forEach((ag) => {
        ag.attributes.forEach((a) => {
          a.conditions = a.conditions.filter(
            (c) => c.operator !== "" && c.value !== ""
          );
        });
      });

      // sanitize restrictions (remove empty restrictions)
      this.proofTemplate.attributeGroups.forEach((ag) => {
        ag.schemaLevelRestrictions = Object.fromEntries(
          Object.entries(ag.schemaLevelRestrictions).filter(([, v]) => v !== "")
        );
      });

      console.log(JSON.stringify(this.proofTemplate));

      proofTemplateService
        .createProofTemplate(this.proofTemplate)
        .then((res) => {
          this.$emit("received-proof-template-id", res.data.id);
          EventBus.$emit("success", "Proof Template Created");

          if (!this.disableRouteBack) {
            this.$router.push({
              name: "ProofTemplates",
              params: {},
            });
          }

          this.isBusy = false;
        })
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
          this.isBusy = false;
        });
    },
  },
};
</script>
