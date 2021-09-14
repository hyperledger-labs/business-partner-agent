<template>
  <div>
    <v-container>
      <h4 class="pb-5">Restrictions</h4>
      <v-row
        v-show="
          attributeGroup.ui.selectedRestrictionsByTrustedIssuer.length === 0
        "
      >
        <v-col>
          <v-alert type="warning"
            >There is no trusted issuer DID selected
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
        <v-card-title class="headline"> Add Trusted Issuer </v-card-title>
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
            <v-bpa-button color="secondary" @click="addTrustedIssuerCancel">
              Cancel
            </v-bpa-button>
            <v-bpa-button
              color="primary"
              :disabled="addTrustedIssuerDialog.did.length === 0"
              @click="addTrustedIssuerRestrictionObject(attributeGroup)"
            >
              Add
            </v-bpa-button>
          </v-layout>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
<script>
import VBpaButton from "@/components/BpaButton";

export default {
  name: "RestrictionsEdit",
  components: { VBpaButton },
  props: {
    attributeGroup: {},
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
  },
  data: () => {
    return {
      addTrustedIssuerDialog: {
        visible: false,
        did: "",
      },
    };
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
  },
};
</script>
