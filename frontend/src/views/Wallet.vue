<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
    <v-container>
        <v-card class="my-4">
            <v-card-title>
                Documents
                <!-- <v-text-field
          v-model="search"
          prepend-icon="mdi-magnify"
          label="Search"
          single-line
          hide-details
        ></v-text-field>-->
            </v-card-title>
            <MyCredentialList
                v-bind:headers="docHeaders"
                type="document"
            ></MyCredentialList>
            <v-card-actions>
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
                            <v-icon>mdi-plus</v-icon>
                        </v-btn>
                    </template>
                    <v-list>
                        <v-list-item
                            v-for="(type, i) in types"
                            :key="i"
                            @click="createDocument(type.name)"
                        >
                            <v-list-item-title>{{
                                type.label
                            }}</v-list-item-title>
                        </v-list-item>
                    </v-list>
                </v-menu>
            </v-card-actions>
        </v-card>
        <v-card class="my-10">
            <v-card-title>Verified Credentials</v-card-title>
            <MyCredentialList
                v-bind:headers="credHeaders"
                type="credential"
            ></MyCredentialList>
        </v-card>
    </v-container>
</template>

<script>
import { CredentialTypes } from "../constants";
import MyCredentialList from "@/components/MyCredentialList";
import { EventBus } from "../main";
export default {
    name: "Wallet",
    components: {
        MyCredentialList
    },
    created() {
        EventBus.$emit("title", "Wallet");
        this.$store.dispatch("loadDocuments");
        this.fetchSchemas();
    },
    data: () => {
        return {
            search: "",
            scheams: [],
            credHeaders: [
                {
                    text: "Type",
                    value: "type"
                },
                {
                    text: "Issuer",
                    value: "issuer"
                },
                {
                    text: "Issued at",
                    value: "issuedAt"
                },
                {
                    text: "Public",
                    value: "isPublic"
                }
            ],
            docHeaders: [
                {
                    text: "Type",
                    value: "type"
                },
                {
                    text: "Created at",
                    value: "createdDate"
                },
                {
                    text: "Updated at",
                    value: "updatedDate"
                },
                {
                    text: "Public",
                    value: "isPublic"
                }
            ]
        };
    },
    methods: {
        fetchSchemas() {
            this.$axios
                .get(`${this.$apiBaseUrl}/admin/schema`)
                .then(result => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, "data")) {
                        this.schemas = result.data;
                    }
                })
                .catch(e => {
                    this.isBusy = false;
                    if (e.response.status === 404) {
                        this.schemas = [];
                    } else {
                        console.error(e);
                        EventBus.$emit("error", e);
                    }
                });
        },
        createDocument: function(type) {
            console.log(type);

            this.$router.push({
                name: "DocumentAdd",
                params: {
                    type: type
                }
            });
        }
    },
    computed: {
        types() {
            let docTypes = Object.values(CredentialTypes).filter(type => {
                return type.name !== CredentialTypes.OTHER.name;
            });
            // here is the store really useful
            if (this.$store.getters.organizationalProfile != undefined) {
                docTypes = docTypes.filter(type => {
                    return type.name !== CredentialTypes.PROFILE.name;
                });
            }
            // docTypes = docTypes.concat(this.schemas)
            return docTypes;
        }
    }
};
</script>

<style scoped>
.truncate {
    max-width: 1px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>
