<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
<v-data-table hide-default-footer v-model="selected" :show-select="selectable" single-select :headers="headers" :items="credentialsWithIndex" :expanded.sync="expanded" item-key="index" show-expand>
    <template v-slot:[`item.type`]="{ item }">
        {{ ( item.type ? item.type : CredentialTypes.OTHER.name ) | credentialLabel }}
    </template>
    <template v-slot:[`item.verified`]="{ item }">
        <v-btn v-if="item.indyCredential" color="primary" text>verify</v-btn>
    </template>
    <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
            <Credential v-bind:document="item" isReadOnly showOnlyContent></Credential>
        </td>
    </template>
</v-data-table>
</template>

<script>
import Credential from "@/components/Credential";
import {
    CredentialTypes
} from "../constants"
export default {
    props: {
        credentials: Array,
         selectable: {
            type: Boolean,
            default: false
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
                    // value: "createdDate"
                },
                {
                    text: "Verified",
                    value: "verified"
                }

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
        // Add an unique index, because elements to not have unique id
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

    },
    components: {
        Credential
    }
};
</script>
