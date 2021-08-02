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
          <v-list-item-title>Attribute Groups</v-list-item-title>
          <v-list-item-subtitle
            >Description of what attribute groups are...</v-list-item-subtitle
          >

          <v-container>
            <v-expansion-panels>
              <v-expansion-panel
                class="my-5"
                v-for="(attributeGroup, idx) in proofTemplate.attributeGroups"
                :key="idx"
              >
                <v-expansion-panel-header>
                  {{ attributeGroup.schemaId }}
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
                          >
                            <template v-slot:item="{ item }">
                              <tr>
                                <td>
                                  <v-select
                                    :items="operators"
                                    v-model="item.operator"
                                    dense
                                    item-text="name"
                                    item-value="value"
                                  />
                                </td>
                                <td>
                                  <v-text-field v-model="item.value" dense />
                                </td>
                                <td>{{ JSON.stringify(item) }}</td>
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
                    <h4>Restrictions</h4>
                    <v-simple-table>
                      <tbody>
                        <tr>
                          <td>Schema ID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.schemaId
                            }}
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Name</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.schemaName
                            }}
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Version</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .schemaVersion
                            }}
                          </td>
                        </tr>
                        <tr>
                          <td>Schema Issuer DID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .schemaIssuerDid
                            }}
                          </td>
                        </tr>
                        <tr>
                          <td>Issuer DID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions.issuerDid
                            }}
                          </td>
                        </tr>
                        <tr>
                          <td>Credential Definition ID</td>
                          <td>
                            {{
                              attributeGroup.schemaLevelRestrictions
                                .credentialDefinitionId
                            }}
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
                  v-for="schema in schemas"
                  :key="schema.id"
                  @click="addAttributeGroup(schema.id)"
                >
                  <v-list-item-title>{{ schema.label }}</v-list-item-title>
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
          <v-bpa-button color="primary" @click="createProofTemplate">
            Create
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-card>
  </v-container>
</template>

<script>
import { EventBus } from "@/main";
import VBpaButton from "@/components/BpaButton";

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
          text: "",
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
  },
  components: { VBpaButton },
  created() {
    EventBus.$emit("title", "Proof Templates");
  },
  mounted() {
    this.schemas = this.$store.getters.getSchemas.filter(
      (schema) => schema.type === "SCHEMA_BASED"
    );
    console.log(JSON.stringify(this.schemas));
  },
  data: () => {
    return {
      schemas: [],
      operators: [
        {
          name: ">",
          value: "GREATER_THAN",
        },
        {
          name: "<",
          value: "LESS_THAN",
        },
      ],
      proofTemplate: {
        name: "",
        attributeGroups: [],
      },
    };
  },
  computed: {},
  watch: {},
  methods: {
    addAttributeGroup(schemaId) {
      // add a blank attribute group template
      this.proofTemplate.attributeGroups.push({
        schemaId: schemaId,
        nonRevoked: true,
        attributes: [],
        schemaLevelRestrictions: {
          schemaId: "",
          schemaName: "",
          schemaVersion: "",
          schemaIssuerDid: "",
          credentialDefinitionId: "",
          issuerDid: "",
        },
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
    createProofTemplate() {
      console.log(JSON.stringify(this.proofTemplate));
    },
  },
};
</script>
