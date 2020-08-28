<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-card class="mx-auto">
    <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1);">
            <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
       Send a Credential Presentation
    </v-card-title>
    
    <v-card-text>
         <MyCredentialList v-bind:headers="credHeaders" type="credential" selectable></MyCredentialList>
         <h3>Sorry, this is not implemented yet yet</h3>
    </v-card-text>

    <v-card-actions>
        <v-layout align-end justify-end>
            <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
            <v-btn :loading="this.isBusy" color="primary" disabled text @click="submitRequest()">Submit</v-btn>
        </v-layout>
    </v-card-actions>
</v-card>
</template>

<script>
import {
    EventBus
} from '../main'

// import { CredentialTypes } from "../constants";
// import VueJsonPretty from "vue-json-pretty"
import MyCredentialList from "@/components/MyCredentialList"

export default {
    name: "SendPresentation",
    components: {
        MyCredentialList
    },
    props: {
        id: String,
    },
    created() {
        EventBus.$emit('title', 'Send Presentation')
    },
    data: () => {
        return {
            isBusy: false,
            credHeaders: [{
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
        };
    },
    computed: {},
    methods: {
        
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
