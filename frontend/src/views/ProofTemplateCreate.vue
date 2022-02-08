<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->

<template>
  <v-container justify-center>
    <v-card class="mx-auto">
      <v-card-title class="bg-light">
        <v-bpa-button depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-bpa-button>
        {{ $t("view.proofTemplate.create.title") }}
      </v-card-title>

      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateName"
            v-model="proofTemplate.name"
            dense
            :label="$t('view.proofTemplate.create.labelName')"
            :rules="[rules.required]"
          ></v-text-field>
        </v-list-item>
      </v-container>

      <v-divider />

      <!-- Attribute Groups -->
      <v-list-item>
        <v-list-item-content>
          <v-list-item-title>{{
            $t("view.proofTemplate.create.requestedDataTitle")
          }}</v-list-item-title>
          <v-list-item-subtitle>{{
            $t("view.proofTemplate.create.requestedDataSubtitle")
          }}</v-list-item-subtitle>
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
                      color="info"
                      >$vuetify.icons.about</v-icon
                    >
                  </div>
                </v-expansion-panel-header>
                <v-expansion-panel-content>
                  <AttributeEdit v-model="proofTemplate.attributeGroups[idx]" />

                  <!-- Schema Restrictions -->
                  <RestrictionsEdit
                    v-model="proofTemplate.attributeGroups[idx]"
                  />

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
        <v-layout align-center align-end justify-end>
          <v-switch
            v-if="expertMode && enableV2Switch"
            v-model="useV2Exchange"
            :label="$t('button.useV2')"
          ></v-switch>
          <v-bpa-button color="secondary" @click="$router.go(-1)">
            {{ $t("button.cancel") }}
          </v-bpa-button>
          <v-bpa-button
            :loading="this.createButtonIsBusy"
            :disabled="overallValidationErrors"
            color="primary"
            @click="createProofTemplate"
          >
            {{ getCreateButtonLabel }}
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>

    <!-- Notification for deletion of attribute group -->
    <v-snackbar v-model="snackbar.deleteShow" :timeout="snackbar.timeout">
      {{ snackbar.text }}
      <template v-slot:action="{ attrs }">
        <v-bpa-button text v-bind="attrs" @click="snackbar.deleteShow = false">
          {{ $t("app.snackBar.close") }}
        </v-bpa-button>
      </template>
    </v-snackbar>
  </v-container>
</template>

<script lang="ts">
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";
import proofTemplateService from "@/services/proofTemplateService";
import AttributeEdit from "@/components/proof-templates/AttributeEdit.vue";
import RestrictionsEdit from "@/components/proof-templates/RestrictionsEdit.vue";
import { SchemaLevelRestriction } from "@/components/proof-templates/attribute-group";

export default {
  name: "ProofTemplates",
  props: {
    disableRouteBack: {
      type: Boolean,
      default: false,
    },
    createButtonLabel: {
      type: String,
      default: undefined,
    },
    enableV2Switch: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  components: { RestrictionsEdit, AttributeEdit, VBpaButton },
  created() {
    EventBus.$emit("title", this.$t("view.proofTemplates.title"));
  },
  data: () => {
    return {
      openAttributeGroupPanels: [],
      createButtonIsBusy: false,
      useV2Exchange: false,
      proofTemplate: {
        name: "",
        attributeGroups: [],
      },
      snackbar: {
        timeout: 3000,
        deleteShow: false,
        text: "",
      },
    };
  },
  computed: {
    expertMode() {
      return this.$store.state.expertMode;
    },
    getCreateButtonLabel() {
      return this.createButtonLabel
        ? this.createButtonLabel
        : this.$t("button.create");
    },
    rules() {
      return {
        required: (value) => !!value || this.$t("app.rules.required"),
      };
    },
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
      const predicateConditionsInvalid =
        this.proofTemplate.attributeGroups.some(
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
      const schemaLevelRestrictions: SchemaLevelRestriction[] = [];

      const { schemaAttributeNames, trustedIssuer } = this.schemas.find(
        (s) => s.id === schemaId
      );

      if (trustedIssuer) {
        for (const issuer of trustedIssuer) {
          schemaLevelRestrictions.push({
            schemaId: schemaId,
            schemaName: "",
            schemaVersion: "",
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
    deleteAttributeGroup(attributeGroupIndex: number) {
      const schema = this.$store.getters.getSchemas.find(
        (schema) =>
          schema.id ===
          this.proofTemplate.attributeGroups[attributeGroupIndex].schemaId
      );

      this.snackbar.text = `${this.$t(
        "view.proofTemplate.create.snackbarContent"
      )} ${schema.label} (${schema.schemaId})`;
      this.proofTemplate.attributeGroups.splice(attributeGroupIndex, 1);
      this.snackbar.deleteShow = true;
    },
    prepareProofTemplateData() {
      let sanitizedAttributeGroupObjects = [];

      for (const ag of this.proofTemplate.attributeGroups) {
        let attributesInGroup = [];
        let restrictionsInGroup = [];

        // sanitize attribute conditions (remove empty conditions)
        for (const a of ag.attributes) {
          const sanitizedConditions = a.conditions.filter(
            (c) => c.operator !== "" && c.value !== ""
          );

          // only use selected attributes
          if (ag.ui.selectedAttributes.includes(a)) {
            attributesInGroup.push({
              name: a.name,
              conditions: sanitizedConditions,
            });
          }
        }

        // sanitize restrictions (remove empty restrictions)
        for (const [
          index,
          schemaLevelRestrictionObject,
        ] of ag.schemaLevelRestrictions.entries()) {
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
        .then((response) => {
          this.$emit("received-proof-template-id", {
            documentId: response.data.id,
            useV2Exchange: this.useV2Exchange,
          });
          EventBus.$emit(
            "success",
            this.$t("view.proofTemplate.create.success")
          );

          if (!this.disableRouteBack) {
            this.$router.push({
              name: "ProofTemplates",
              params: {},
            });
          }

          this.createButtonIsBusy = false;
        })
        .catch((error) => {
          EventBus.$emit("error", this.$axiosErrorMessage(error));
          this.createButtonIsBusy = false;
        });
    },
  },
};
</script>
