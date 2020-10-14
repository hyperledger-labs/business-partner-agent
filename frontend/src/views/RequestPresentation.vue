<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
    <v-card class="mx-auto">
        <v-card-title class="bg-light">
            <v-btn depressed color="secondary" icon @click="$router.go(-1)">
                <v-icon dark>mdi-chevron-left</v-icon>
            </v-btn>
            Create a Presentation Request
        </v-card-title>
        <v-card-text>
            <h4 class="pt-4">Select a credential type to request</h4>
            <v-data-table
                hide-default-footer
                v-model="selected"
                :show-select="true"
                single-select
                :headers="headers"
                :items="templates"
                item-key="credentialDefinitionId"
            >
                <template v-slot:[`item.credentialDefinitionId`]="{ item }">
                    {{ item.credentialDefinitionId | credentialTag }}
                </template>
            </v-data-table>
            <h4 class="pt-4">Or enter a custom Credential Definition ID</h4>
            <v-text-field
                label="Credential Definition ID"
                placeholder=""
                v-model="credDefId"
                outlined
                dense
            >
            </v-text-field>
        </v-card-text>

        <v-card-actions>
            <v-layout align-end justify-end>
                <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
                <v-btn
                    :loading="this.isBusy"
                    color="primary"
                    text
                    @click="submitRequest()"
                    >Submit</v-btn
                >
            </v-layout>
        </v-card-actions>
    </v-card>
</template>

<script>
import { EventBus } from "../main";

import { CredentialTypes } from "../constants";
// import VueJsonPretty from "vue-json-pretty";

export default {
    name: "RequestPresentation",
    components: {},
    props: {
        id: String //partner ID
    },
    created() {
        EventBus.$emit("title", "Request Presentation");
    },
    data: () => {
        return {
            isBusy: false,
            credDefId: "",
            selected: [],
            CredentialTypes: CredentialTypes,
            headers: [
                {
                    text: "Type",
                    value: "credentialDefinitionId"
                },
                {
                    text: "Issuer",
                    value: "issuer"
                }
            ],
            templates: [
                {
                    credentialDefinitionId:
                        "nJvGcV7hBSLRSUvwGk2hT:3:CL:734:IATF Certificate",
                    issuer: "IATF Proxy Issuer"
                },
                {
                    credentialDefinitionId:
                        "4QybVurJnPDTHcmcbiGUnU:3:CL:894:commercial register entry",
                    issuer: "did:sov:iil:4QybVurJnPDTHcmcbiGUnU"
                },
                {
                    credentialDefinitionId:
                        "8faozNpSjFfPJXYtgcPtmJ:3:CL:894:Commercial Registry Entry",
                    issuer: "Commercial Registry (Open Corporates Proxy)"
                },
                {
                    credentialDefinitionId:
                        "M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc",
                    issuer: "did:sov:iil:M6Mbe3qx7vB4wpZF4sBRjt"
                }
            ]
        };
    },
    computed: {},
    methods: {
        submitRequest() {
            this.isBusy = true;
            console.log(this.selected);
            if (this.selected.length === 1 || this.credDefId.length > 0) {
                let credDefId =
                    this.selected.length === 1
                        ? this.selected[0].credentialDefinitionId
                        : this.credDefId;

                this.$axios
                    .post(
                        `${this.$apiBaseUrl}/partners/${this.id}/proof-request`,
                        {
                            credentialDefinitionId: credDefId
                        }
                    )
                    .then(res => {
                        console.log(res);
                        this.isBusy = false;
                        EventBus.$emit("success", "Presentation request sent");
                        this.$router.go(-1);
                    })
                    .catch(e => {
                        this.isBusy = false;
                        console.error(e);
                        EventBus.$emit("error", e);
                    });
            } else {
                this.isBusy = false;
                EventBus.$emit("error", "No credential type selected");
            }
        },
        cancel() {
            this.$router.go(-1);
        }
    }
};
</script>

<style scoped>
.bg-light {
    background-color: #fafafa;
}

.bg-light-2 {
    background-color: #ececec;
}
</style>
