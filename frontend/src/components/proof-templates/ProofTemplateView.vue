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
    <v-card class="mx-auto">
      <!-- Title -->
      <v-card-title class="bg-light">
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
      <v-container
        v-for="attributeGroup in proofTemplate.attributeGroups"
        :key="attributeGroup.schemaId"
      >
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

      <!-- Actions -->
      <v-divider></v-divider>
      <v-card-actions>
        <v-layout align-end justify-end>
          <v-bpa-button color="primary" @click="closed">Close</v-bpa-button>
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
  name: "ProofTemplateView",
  props: {
    dialog: {
      type: Boolean,
      default: () => false,
    },
    proofTemplate: Object,
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
  watch: {},
  created() {},
  computed: {},
  methods: {
    deleteProofTemplate() {
      proofTemplateService
        .deleteProofTemplate(this.proofTemplate.id)
        .then((result) => {
          console.log(result);
          if (result.status === 200) {
            EventBus.$emit("success", "Proof Template deleted");
            this.$emit("changed");
            this.$emit("deleted");
          }
        })
        .catch((e) => {
          console.error(e);
          EventBus.$emit("error", e);
        });
    },
    onChanged() {
      this.$emit("changed");
    },
    closed() {
      this.$emit("closed");
    },
  },
};
</script>
