<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-container justify-center>
    <v-card class="mx-auto" max-width="600" flat>
        <v-card-title class="grey--text text--darken-2">Settings</v-card-title>
        <v-list-item>
            <v-list-item-content>
                <v-list-item-title class="grey--text text--darken-2 font-weight-medium">Expert mode</v-list-item-title>
                <v-list-item-subtitle>Enable demo features</v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action>
                <v-switch v-model="expertMode"></v-switch>
            </v-list-item-action>
        </v-list-item>
        <v-list-item v-for="setting in settings" :key="setting.text">
           <!-- <v-list-item-content> -->
            <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
                {{setting.text}}
            </v-list-item-title>
            <v-list-item-subtitle align="end">
                {{setting.value}}
            </v-list-item-subtitle>
             <!-- </v-list-item-content> -->
        </v-list-item>
    </v-card>
</v-container>
</template>

<script>
import {
    EventBus
} from "../main";
export default {
    name: "Settings",
    created() {
        EventBus.$emit("title", "Settings");
        this.fetch()
    },
    data: () => {
        return {
            settingsHeader: [{
                text: "Host",
                value: "host"
            }, {
                text: "Universal Resolver",
                value: "uniResolverUrl"
            }, {
                text: "Ledger Browser",
                value: "ledgerBrowser"
            }, {
                text: "Ledger DID Prefix",
                value: "ledgerPrefix"
            }, {
                text: "Aries Agent Url",
                value: "acaPyUrl"
            },{
                text: "Aries API Key",
                value: "acaPyApiKey"
            }],
            settings: []
        }
    },
    computed: {
        expertMode: {
            set(body) {
                this.$store.commit({
                    type: "setSettings",
                    isExpert: body
                });
            },
            get() {
                return this.$store.state.expertMode;
            }
        }
    },
    methods: {
        fetch() {
            this.$axios
                .get(`${this.$apiBaseUrl}/admin/config`)
                .then(result => {
                    if ({}.hasOwnProperty.call(result, "data")) {

                        this.settings = this.settingsHeader.map(setting => {
                            return {
                                text: setting.text,
                                value: result.data[setting.value]
                            }

                        })
                    }
                })
                .catch(e => {
                    console.error(e);
                    EventBus.$emit("error", e);
                });
        },

    }
};
</script>
