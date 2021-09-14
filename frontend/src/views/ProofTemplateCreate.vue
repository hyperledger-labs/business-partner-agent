<!--
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <v-bpa-button depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-bpa-button>
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
            :rules="[rules.required]"
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
            <v-expansion-panels
              focusable
              multiple
              v-model="openAttributeGroupPanels"
            >
              <v-expansion-panel
                class="my-5"
                v-for="(attributeGroup, idx) in proofTemplate.attributeGroups"
                :key="attributeGroup.schemaId"
              >
                <v-expansion-panel-header>
                  <div>
                    <span v-html="renderSchemaLabelId(attributeGroup)"></span>
                    <v-icon
                      right
                      color="error"
                      v-show="
                        attributeGroup.ui.selectedAttributes.length === 0 ||
                        attributeGroup.ui.predicateConditionsErrorCount > 0
                      "
                      >$vuetify.icons.validationError</v-icon
                    >
                    <v-icon
                      v-show="
                        attributeGroup.ui.selectedRestrictionsByTrustedIssuer
                          .length === 0
                      "
                      right
                      color="warning"
                      >$vuetify.icons.validationWarning</v-icon
                    >
                  </div>
                </v-expansion-panel-header>
                <v-expansion-panel-content>
                  <AttributeEdit :attribute-group="attributeGroup" />

                  <!-- Schema Restrictions -->
                  <v-container>
                    <h4 class="pb-5">Restrictions</h4>
                    <v-row
                      v-show="
                        attributeGroup.ui.selectedRestrictionsByTrustedIssuer
                          .length === 0
                      "
                    >
                      <v-col>
                        <v-alert type="warning"
                          >There is no trusted issuer DID selected</v-alert
                        >
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
                      v-model="
                        attributeGroup.ui.selectedRestrictionsByTrustedIssuer
                      "
                      item-key="issuerDid"
                      class="elevation-1"
                      show-expand
                    >
                      <template v-slot:expanded-item="{ headers, item }">
                        <td :colspan="headers.length" style="padding: 0">
                          <v-simple-table>
                            <tbody>
                              <tr>
                                <td>Schema Name</td>
                                <td>
                                  <v-text-field
                                    id="proofTemplateName"
                                    v-model="item.schemaName"
                                    dense
                                  ></v-text-field>
                                </td>
                              </tr>
                              <tr>
                                <td>Schema Version</td>
                                <td>
                                  <v-text-field
                                    id="proofTemplateName"
                                    v-model="item.schemaVersion"
                                    dense
                                  ></v-text-field>
                                </td>
                              </tr>
                              <tr>
                                <td>Schema Issuer DID</td>
                                <td>
                                  <v-text-field
                                    id="proofTemplateName"
                                    v-model="item.schemaIssuerDid"
                                    dense
                                  ></v-text-field>
                                </td>
                              </tr>
                              <tr>
                                <td>Trusted issuer DID</td>
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
                                <td>Credential Definition ID</td>
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
                    </v-data-table>
                  </v-container>
                  <v-dialog
                    v-model="addTrustedIssuerDialog.visible"
                    :retain-focus="false"
                    persistent
                    max-width="600px"
                  >
                    <template v-slot:activator="{ on, attrs }">
                      <v-bpa-button color="secondary" v-bind="attrs" v-on="on"
                        >Add trusted issuer</v-bpa-button
                      >
                    </template>
                    <v-card>
                      <v-card-title class="headline">
                        Add Trusted Issuer
                      </v-card-title>
                      <v-card-text>
                        <v-container>
                          <v-text-field
                            label="DID"
                            hint="The decentralized ID of a trusted issuer"
                            v-model="addTrustedIssuerDialog.did"
                            persistent-hint
                            outlined
                          ></v-text-field>
                        </v-container>
                      </v-card-text>
                      <v-card-actions>
                        <v-layout align-end justify-end>
                          <v-bpa-button
                            color="secondary"
                            @click="addTrustedIssuerCancel"
                          >
                            Cancel
                          </v-bpa-button>
                          <v-bpa-button
                            color="primary"
                            :disabled="addTrustedIssuerDialog.did.length === 0"
                            @click="
                              addTrustedIssuerRestrictionObject(attributeGroup)
                            "
                          >
                            Add
                          </v-bpa-button>
                        </v-layout>
                      </v-card-actions>
                    </v-card>
                  </v-dialog>
                  <v-card-actions>
                    <v-bpa-button
                      fab
                      absolute
                      small
                      bottom
                      right
                      color="error"
                      @click="deleteAttributeGroup(idx)"
                    >
                      <v-icon>$vuetify.icons.delete</v-icon>
                    </v-bpa-button>
                  </v-card-actions>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-container>

          <!-- add new attribute group -->
          <v-container>
            <v-menu>
              <template v-slot:activator="{ on, attrs }">
                <v-bpa-button
                  color="primary"
                  bottom
                  left
                  fab
                  v-bind="attrs"
                  v-on="on"
                >
                  <v-icon>$vuetify.icons.add</v-icon>
                </v-bpa-button>
              </template>
              <v-list max-height="50vh" class="overflow-y-auto">
                <v-list-item
                  v-for="(schema, idx) in schemas"
                  :key="idx"
                  @click="addAttributeGroup(schema.id)"
                  :disabled="
                    proofTemplate.attributeGroups.some(
                      (existingAttributeGroup) =>
                        existingAttributeGroup.schemaId === schema.id
                    )
                  "
                >
                  <v-list-item-content>
                    <v-list-item-title>
                      {{ schema.label }}
                    </v-list-item-title>
                    <v-list-item-subtitle>
                      {{ schema.schemaId }}
                    </v-list-item-subtitle>
                  </v-list-item-content>
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
            :loading="this.createButtonIsBusy"
            :disabled="overallValidationErrors"
            color="primary"
            @click="createProofTemplate"
          >
            {{ createButtonLabel }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>

    <!-- Notification for deletion of attribute group -->
    <v-snackbar v-model="snackbar.deleteShow" :timeout="snackbar.timeout">
      {{ snackbar.text }}
      <template v-slot:action="{ attrs }">
        <v-bpa-button text v-bind="attrs" @click="snackbar.deleteShow = false">
          Close
        </v-bpa-button>
      </template>
    </v-snackbar>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import proofTemplateService from "@/services/proofTemplateService";
import AttributeEdit from "@/components/proof-templates/AttributeEdit";

export default {
  name: "ProofTemplates",
  props: {
    restrictionsHeaders: {
      type: Array,
      default: () => [
        {
          text: "Trusted Issuer",
          value: "issuerDid",
        },
        {
          text: "",
          value: "data-table-expand",
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
  components: { AttributeEdit, VBpaButton },
  created() {
    EventBus.$emit("title", "Proof Templates");
  },
  data: () => {
    return {
      addTrustedIssuerDialog: {
        visible: false,
        did: "",
      },
      openAttributeGroupPanels: [],
      createButtonIsBusy: false,
      proofTemplate: {
        name: "",
        attributeGroups: [],
      },
      snackbar: {
        timeout: 3000,
        deleteShow: false,
        text: "",
      },
      rules: {
        required: (value) => !!value || "Required",
      },
    };
  },
  computed: {
    schemas() {
      return this.$store.getters.getSchemas.filter(
        (schema) => schema.type === "INDY"
      );
    },
    overallValidationErrors() {
      const proofTemplateNameInvalid = this.proofTemplate.name === "";
      const noAttributeGroups = this.proofTemplate.attributeGroups.length === 0;
      const attributeGroupsInvalid = this.proofTemplate.attributeGroups.some(
        (ag) => ag.ui.selectedAttributes.length === 0
      );
      const predicateConditionsInvalid = this.proofTemplate.attributeGroups.some(
        (ag) => ag.ui.predicateConditionsErrorCount > 0
      );

      return (
        proofTemplateNameInvalid ||
        noAttributeGroups ||
        attributeGroupsInvalid ||
        predicateConditionsInvalid
      );
    },
  },
  methods: {
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
    closeOtherPanelsOnOpen() {
      this.openAttributeGroupPanels.splice(
        0,
        this.openAttributeGroupPanels.length,
        this.proofTemplate.attributeGroups.length - 1
      );
    },
    renderSchemaLabelId(attributeGroup) {
      const schema = this.$store.getters.getSchemas.find(
        (s) => s.id === attributeGroup.schemaId
      );
      return `${schema.label}<em>&nbsp;(${schema.schemaId})</em>`;
    },
    addAttributeGroup: function (schemaId) {
      const schemaLevelRestrictions = [];

      const {
        schemaAttributeNames,
        trustedIssuer,
        version,
        label,
      } = this.schemas.find((s) => s.id === schemaId);

      if (trustedIssuer) {
        for (const issuer of trustedIssuer) {
          schemaLevelRestrictions.push({
            schemaId: schemaId,
            schemaName: label,
            schemaVersion: version,
            schemaIssuerDid: "",
            issuerDid: issuer.issuerDid,
            credentialDefinitionId: "",
          });
        }
      }

      let attributes = [];

      for (const attributeName of schemaAttributeNames) {
        attributes.push({
          name: attributeName,
          conditions: [
            {
              operator: "",
              value: "",
            },
          ],
        });
      }

      // add a basic attribute group template with all available attributes
      // and restriction objects for each trusted issuer
      this.proofTemplate.attributeGroups.push({
        schemaId,
        nonRevoked: true,
        attributes,
        ui: {
          selectedAttributes: attributes,
          selectedRestrictionsByTrustedIssuer: schemaLevelRestrictions,
          predicateConditionsErrorCount: 0,
        },
        schemaLevelRestrictions,
      });

      this.closeOtherPanelsOnOpen();
    },
    deleteAttributeGroup(attributeGroupIdx) {
      const schema = this.$store.getters.getSchemas.find(
        (s) =>
          s.id ===
          this.proofTemplate.attributeGroups[attributeGroupIdx].schemaId
      );

      this.snackbar.text = `Removed attribute group ${schema.label} (${schema.schemaId})`;
      this.proofTemplate.attributeGroups.splice(attributeGroupIdx, 1);
      this.snackbar.deleteShow = true;
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
    prepareProofTemplateData() {
      let sanitizedAttributeGroupObjects = [];

      for (const ag of this.proofTemplate.attributeGroups) {
        let attributesInGroup = [];
        let restrictionsInGroup = [];

        // sanitize attribute conditions (remove empty conditions)
        for (const a of ag.attributes) {
          a.conditions = a.conditions.filter(
            (c) => c.operator !== "" && c.value !== ""
          );

          // only use selected attributes
          if (ag.ui.selectedAttributes.includes(a)) {
            attributesInGroup.push(a);
          }
        }

        // sanitize restrictions (remove empty restrictions)
        ag.schemaLevelRestrictions.forEach(
          (schemaLevelRestrictionObject, index) => {
            ag.schemaLevelRestrictions[index] = Object.fromEntries(
              Object.entries(schemaLevelRestrictionObject).filter(
                ([, v]) => v !== ""
              )
            );

            ag.ui.selectedRestrictionsByTrustedIssuer.map(
              (selectedRestrictions) => {
                if (
                  selectedRestrictions.issuerDid ===
                  schemaLevelRestrictionObject.issuerDid
                ) {
                  restrictionsInGroup.push(schemaLevelRestrictionObject);
                }
              }
            );
          }
        );

        // add empty restrictions object to satisfy backend
        if (ag.schemaLevelRestrictions.length === 0) {
          restrictionsInGroup.push({});
        }

        // sanitize ui data (remove ui helper values)
        sanitizedAttributeGroupObjects.push({
          schemaId: ag.schemaId,
          nonRevoked: ag.nonRevoked,
          attributes: attributesInGroup,
          schemaLevelRestrictions: restrictionsInGroup,
        });
      }

      return {
        name: this.proofTemplate.name,
        attributeGroups: sanitizedAttributeGroupObjects,
      };
    },
    async createProofTemplate() {
      this.createButtonIsBusy = true;

      const proofTemplate = this.prepareProofTemplateData();

      proofTemplateService
        .createProofTemplate(proofTemplate)
        .then((res) => {
          this.$emit("received-proof-template-id", res.data.id);
          EventBus.$emit("success", "Proof Template Created");

          if (!this.disableRouteBack) {
            this.$router.push({
              name: "ProofTemplates",
              params: {},
            });
          }

          this.createButtonIsBusy = false;
        })
        .catch((e) => {
          EventBus.$emit("error", this.$axiosErrorMessage(e));
          this.createButtonIsBusy = false;
        });
    },
  },
};
</script>
