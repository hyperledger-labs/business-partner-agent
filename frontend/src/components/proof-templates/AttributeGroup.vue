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

<template>
  <v-container>
    <v-expansion-panels>
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
            <h4 class="mb-4">Data fields</h4>
            <v-data-table
              disable-sort
              :headers="attributeGroupHeaders"
              :items="attributeGroup.attributes"
              item-key="name"
              class="elevation-1"
              show-expand
              hide-default-footer
            >
              <template v-slot:expanded-item="{ item }">
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

          <v-container
            v-if="
              Object.keys(attributeGroup.schemaLevelRestrictions).Length > 0
            "
          >
            <h4 class="mb-4">Restrictions</h4>
            <v-simple-table>
              <tbody>
                <tr v-if="attributeGroup.schemaLevelRestrictions.schemaId">
                  <td>Schema ID</td>
                  <td>
                    {{ attributeGroup.schemaLevelRestrictions.schemaId }}
                  </td>
                </tr>
                <tr v-if="attributeGroup.schemaLevelRestrictions.schemaName">
                  <td>Schema Name</td>
                  <td>
                    {{ attributeGroup.schemaLevelRestrictions.schemaName }}
                  </td>
                </tr>
                <tr v-if="attributeGroup.schemaLevelRestrictions.schemaVersion">
                  <td>Schema Version</td>
                  <td>
                    {{ attributeGroup.schemaLevelRestrictions.schemaVersion }}
                  </td>
                </tr>
                <tr
                  v-if="attributeGroup.schemaLevelRestrictions.schemaIssuerDid"
                >
                  <td>Schema Issuer DID</td>
                  <td>
                    {{ attributeGroup.schemaLevelRestrictions.schemaIssuerDid }}
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
                <tr v-if="attributeGroup.schemaLevelRestrictions.issuerDid">
                  <td>Issuer DID</td>
                  <td>
                    {{ attributeGroup.schemaLevelRestrictions.issuerDid }}
                  </td>
                </tr>
              </tbody>
            </v-simple-table>
          </v-container>

          <!-- Select matching credential -->

          <v-container v-if="attributeGroup.matchingCredentials">
            <h4 class="mb-4">Select data for presentation</h4>
            <v-select
              label="Matching Credentials"
              return-object
              :items="attributeGroup.matchingCredentials"
              item-text="credentialInfo.referent"
              v-model="attributeGroup.selectedCredential"
              outlined
              @change="selectedCredential(idx, $event)"
              dense
            ></v-select>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-container>
</template>
<script>
import Vue from "vue";
export default {
  props: {
    requestData: Array,
  },
  data: () => {
    return {
      attributeGroupHeaders: [
        {
          text: "name",
          value: "name",
        },
        { text: "value", value: "value" },
      ],
      attributeConditionHeaders: [
        {
          text: "operator",
          value: "operator",
        },
        {
          text: "value",
          value: "value",
        },
      ],
    };
  },
  methods: {
    selectedCredential(idx, credential) {
      const newValue = this.requestData[idx].attributes.map((attr) => {
        return { ...attr, value: credential.credentialInfo.attrs[attr.name] };
      });

      Vue.set(this.requestData[idx], "attributes", newValue);
    },
    renderSchemaLabelId(attributeGroup) {
      // FIXME: This needs refactoring
      // This tries to show a schema and label but will show the attribute group if
      let schemaId = null;
      let internalSchemaId = null;
      if (attributeGroup.schemaId) {
        internalSchemaId = attributeGroup.schemaId;
      } else if (
        attributeGroup.schemaLevelRestrictions &&
        attributeGroup.schemaLevelRestrictions.schemaId
      ) {
        schemaId = attributeGroup.schemaLevelRestrictions.schemaId;
      }

      let schema = null;
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
        return `${schema.label}<i>&nbsp;(${schema.schemaId})</i>`;
      } else if (schemaId) {
        return schemaId;
      } else {
        return attributeGroup.attributeGroupName;
      }
    },
  },
};
</script>
