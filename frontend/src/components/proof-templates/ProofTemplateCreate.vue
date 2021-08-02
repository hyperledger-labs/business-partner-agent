<!--
  - Copyright (c) 2020-2021 - for information on the respective copyright owner
  - see the NOTICE file and/or the repository at
  - https://github.com/hyperledger-labs/business-partner-agent
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
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
    <!-- the form object -->
    <v-form ref="form" v-model="valid">
      <v-card class="mx-auto">
        <!-- component title -->
        <v-card-title class="bg-light">Create Proof Template</v-card-title>
        <v-card-text>
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
          </v-container>

          <v-divider />

          <!-- Attribute Groups -->
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title>Attribute Groups</v-list-item-title>
              <v-list-item-subtitle
                >Description of what attribute groups are...
              </v-list-item-subtitle>

              <v-container
                v-for="attributeGroup in proofTemplate.attributeGroups"
                :key="attributeGroup.schemaId"
              >
                <v-expansion-panels>
                  <v-expansion-panel
                    class="my-5"
                    v-for="attributeGroup in proofTemplate.attributeGroups"
                    :key="attributeGroup.schemaId"
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
                            v-slot:expanded-item="{
                              attributeGroupHeaders,
                              item,
                            }"
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

                      <!-- Schema Restrictions -->
                      <v-container>
                        <h4>Restrictions</h4>
                        <v-simple-table>
                          <tbody>
                            <tr
                              v-if="
                                attributeGroup.schemaLevelRestrictions.schemaId
                              "
                            >
                              <td>Schema ID</td>
                              <td>
                                {{
                                  attributeGroup.schemaLevelRestrictions
                                    .schemaId
                                }}
                              </td>
                            </tr>
                            <tr
                              v-if="
                                attributeGroup.schemaLevelRestrictions
                                  .schemaName
                              "
                            >
                              <td>Schema Name</td>
                              <td>
                                {{
                                  attributeGroup.schemaLevelRestrictions
                                    .schemaName
                                }}
                              </td>
                            </tr>
                            <tr
                              v-if="
                                attributeGroup.schemaLevelRestrictions
                                  .schemaVersion
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
                                  attributeGroup.schemaLevelRestrictions
                                    .issuerDid
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
            </v-list-item-content>
          </v-list-item>

          <v-divider />
        </v-card-text>
      </v-card>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="secondary" @click="cancel()"
            >Cancel</v-bpa-button
          >
          <v-bpa-button
            :loading="this.isBusy"
            color="primary"
            @click="submit()"
          >
            Submit
          </v-bpa-button>
        </v-layout>
      </v-card-actions>
    </v-form>
  </v-container>
</template>

<script>
import VBpaButton from "@/components/BpaButton";

export default {
  name: "ProofTemplateCreate",
  components: { VBpaButton },
  props: {
    dialog: {
      type: Boolean,
      default: () => false,
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
  mounted() {
    this.schemas = this.$store.getters.getSchemas.filter(
      (schema) => schema.type === "SCHEMA_BASED"
    );
  },
  data: () => {
    return {
      valid: true,
      isBusy: false,
      schemas: [],
      proofTemplate: {
        name: "",
        nonRevoked: true,
        attributeGroups: [
          {
            schemaId: "",
            name: "",
            attributes: [],
            schemaLevelRestrictions: {
              schemaId: "",
              schemaName: "",
              schemaVersion: "",
              schemaIssuerDid: "",
              credentialDefinitionId: "",
              issuerDid: "",
            },
          },
        ],
      },
    };
  },
  computed: {},
  methods: {
    resetForm() {
      // reset validation
      this.$refs.form.reset();

      // reset form data
      const initialData = this.$options.data.call(this);
      Object.assign(this.$data, initialData);
    },
    submit() {
      // convert unorganized list of attributes into proper attribute groups that the backend needs
      let attributeGroups = this.attributes.reduce(function (map, attr) {
        if (!map[attr.schemaId]) {
          map[attr.schemaId] = [attr];
        } else {
          map[attr.schemaId] = [...map[attr.schemaId], attr];
        }

        return map;
      }, {});

      console.log(JSON.stringify(attributeGroups));
    },
    cancel() {
      this.resetForm();
      this.$emit("cancelled");
    },
    addAttribute() {
      this.attributes.push({
        schemaId: "",
        attributeName: "",
        possibleAttributes: [],
        restrictions: [],
      });
    },
    deleteAttribute(idx) {
      this.attributes.splice(idx, 1);
    },
    setPossibleAttributes(idx, schemaId) {
      this.attributes[idx].possibleAttributes = this.schemas.find(
        (schema) => schemaId === schema.id
      ).schemaAttributeNames;
    },
  },
};
</script>
