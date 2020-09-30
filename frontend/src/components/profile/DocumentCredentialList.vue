<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
    <div>
        <div v-for="(item, index) in credentials" v-bind:key="index">
            <v-row>
                <v-col cols="4">
                    <span  class="grey--text text--darken-2 font-weight-medium">
                        <span v-if="item.type === CredentialTypes.OTHER.name">{{ item.credentialDefinitionId | credentialTag }}</span>
                        <span v-else>{{ item.type | credentialLabel }}</span>
                    </span>
                    <v-icon v-if="item.state === 'verified'" color="green">mdi-check</v-icon>
                    <v-icon v-if="item.actions" small @click="deleteItem(item)">mdi-delete</v-icon>
                </v-col>
                <v-col>
                    <DocumentCredential v-bind:document="item" isReadOnly showOnlyContent></DocumentCredential>
                    
                    <!-- TODO Timestamp Component -->
                    <h4 v-if="(item.sentAt || item.receivedAt)" class="grey--text text--darken-2">Timestamp</h4>
                    <v-row v-if="item.sentAt">
                        <v-col class="pb-0">
                            <v-text-field label="Sent at" :placeholder="$options.filters.moment(item.sentAt, 'YYYY-MM-DD HH:mm')" disabled outlined dense></v-text-field>
                        </v-col>
                    </v-row>
                    <v-row v-if="item.receivedAt">
                        <v-col class="py-0">
                            <v-text-field label="Received at" :placeholder="$options.filters.moment(item.receivedAt, 'YYYY-MM-DD HH:mm')" disabled outlined dense></v-text-field>
                        </v-col>
                    </v-row>
                </v-col>
            </v-row>
            <v-divider></v-divider>
        </div>
    </div>
</template>

<script>
import DocumentCredential from "@/components/profile/DocumentCredential";
import {
    EventBus
} from "../../main";
import {
    CredentialTypes
} from "../../constants"
export default {
    props: {
        credentials: Array,
         selectable: {
            type: Boolean,
            default: false
        },
        expandable: {
            type: Boolean,
            default: true
        },
        headers: {
            type: Array,
            default: () => [{
                    text: "Type",
                    value: "type"
                },
                {
                    text: "Issuer",
                    value: "issuer"
                },
                {
                    text: "Received at",
                    value: "receivedAt"
                },
                {
                    text: "Verified",
                    value: "verified"
                },
                // {
                //     text: "Actions",
                //     value: "actions"
                // }

            ]
        }
    },
    created() {
    },
    data: () => {
        return {
            selected: [],
            CredentialTypes: CredentialTypes,
            expanded: []
        };
    },
    computed: {
        // Add an unique index, because elements do not have unique id
        credentialsWithIndex: function () {
            return this.credentials
                .map(
                    (credentials, index) => ({
                        ...credentials,
                        index: index + 1
                    }))
            // .map(credential => {
            //   credential.verified = true
            // })
        }

    },
    methods: {
         openPresentation(presentation) {
            if (presentation.id) {
                this.$router.push({ path: `presentation/${presentation.id}`, append: true})
            } else {
                EventBus.$emit('error', 'No details view available for presentations in public profile.')

            }
        }

    },
    components: {
        DocumentCredential
    }
};
</script>
