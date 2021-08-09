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
  <v-container>
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1)">
          <v-icon dark>$vuetify.icons.prev</v-icon>
        </v-btn>
        <span>{{ proofTemplate.name }}</span>
        <v-layout align-end justify-end>
          <v-btn depressed color="red" icon @click="deleteProofTemplate">
            <v-icon dark>$vuetify.icons.delete</v-icon>
          </v-btn>
        </v-layout>
      </v-card-title>

      <!-- Basic Data -->
      <v-container>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateName"
            v-model="proofTemplate.name"
            readonly
            dense
            label="Name"
            :append-icon="'$vuetify.icons.copy'"
          ></v-text-field>
        </v-list-item>
        <v-list-item class="mt-4">
          <v-text-field
            id="proofTemplateCreatedAt"
            v-model="proofTemplate.createdAt"
            readonly
            dense
            label="Created At"
            :append-icon="'$vuetify.icons.copy'"
            >{{ new Date(proofTemplate.createdAt).toLocaleString() }}
          </v-text-field>
        </v-list-item>
      </v-container>
      <v-divider></v-divider>

      <!-- Attribute Groups -->
      <v-container>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title>Attribute Groups</v-list-item-title>
            <v-list-item-subtitle
              >Description of what attribute groups are...
            </v-list-item-subtitle>

            <v-expansion-panels>
              <v-expansion-panel
                class="my-5"
                v-for="attributeGroup in proofTemplate.attributeGroups"
                :key="attributeGroup.schemaId"
              >
                <v-expansion-panel-header>
                  {{
                    schemas.find((s) => s.id === attributeGroup.schemaId)
                      .schemaId
                  }}
                </v-expansion-panel-header>
                <v-expansion-panel-content>
                  <v-container>
                    <h4>Attributes</h4>
                    <v-data-table
                      disable-sort
                      :headers="attributeGroupHeaders"
                      :items="attributeGroup.attributes"
                      item-key="name"
                      class="elevation-1"
                      show-expand
                      hide-default-footer
                    >
                      <template
                        v-slot:expanded-item="{ attributeGroupHeaders, item }"
                      >
                        <td :colspan="attributeConditionHeaders.length">
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

                  <v-container>
                    <h4>Restrictions</h4>
                    <v-simple-table>
                      <tbody>
                        <tr
                          v-if="attributeGroup.schemaLevelRestrictions.schemaId"
                        >
                          <td>Schema ID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.schemaId
                            }}
                          </td>
                        </tr>
                        <tr
                          v-if="
                            attributeGroup.schemaLevelRestrictions.schemaName
                          "
                        >
                          <td>Schema Name</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.schemaName
                            }}
                          </td>
                        </tr>
                        <tr
                          v-if="
                            attributeGroup.schemaLevelRestrictions.schemaVersion
                          "
                        >
                          <td>Schema Version</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .schemaVersion
                            }}
                          </td>
                        </tr>
                        <tr
                          v-if="
                            attributeGroup.schemaLevelRestrictions
                              .schemaIssuerDid
                          "
                        >
                          <td>Schema Issuer DID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .schemaIssuerDid
                            }}
                          </td>
                        </tr>
                        <tr
                          v-if="
                            attributeGroup.schemaLevelRestrictions
                              .credentialDefinitionId
                          "
                        >
                          <td>Credential Definition ID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .credentialDefinitionId
                            }}
                          </td>
                        </tr>
                        <tr
                          v-if="
                            attributeGroup.schemaLevelRestrictions.issuerDid
                          "
                        >
                          <td>Issuer DID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.issuerDid
                            }}
                          </td>
                        </tr>
                      </tbody>
                    </v-simple-table>
                  </v-container>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-list-item-content>
        </v-list-item>
      </v-container>

      <!-- Proof Templates Actions -->
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="$router.go(-1)">
            Close
          </v-bpa-button>
          <v-bpa-button color="primary" disabled>
            Create Proof Request
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import proofTemplateService from "@/services/proofTemplateService";
import VBpaButton from "@/components/BpaButton";

export default {
  name: "ProofTemplates",
  props: {
    id: {
      type: String,
      required: false,
    },
    attributeGroupHeaders: {
      type: Array,
      default: () => [
        {
          text: "name",
          value: "name",
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
      ],
    },
  },
  components: {
    VBpaButton,
  },
  created() {
    EventBus.$emit("title", "Proof Templates");
    this.getProofTemplate();
  },
  data: () => {
    return {
      schemas: [],
    };
  },
  mounted() {
    // load schemas
    this.schemas = this.$store.getters.getSchemas.filter(
      (schema) => schema.type === "SCHEMA_BASED"
    );
  },
  computed: {},
  watch: {},
  methods: {
    getProofTemplate() {
      this.proofTemplate = this.$store.state.proofTemplates.find(
        (pt) => pt.id === this.id
      );
      console.log("found: {}", JSON.stringify(this.proofTemplate));
    },
    deleteProofTemplate() {
      proofTemplateService
        .deleteProofTemplate(this.proofTemplate.id)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Proof Template Deleted");

            this.$router.push({
              name: "ProofTemplates",
              params: {},
            });
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
  },
};
</script>
