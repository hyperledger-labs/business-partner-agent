<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
<v-data-table 
    :hide-default-footer="credentialsWithIndex.length < 10" 
    v-model="selected" 
    :show-select="selectable" 
    single-select 
    :headers="headers" 
    :items="credentialsWithIndex" 
    :expanded.sync="expanded" 
    item-key="index" 
    :show-expand="expandable"
    @click:row="openPresentation"
>
    <template v-slot:[`item.type`]="{ item }">
        <div v-if="item.type === CredentialTypes.OTHER.name">{{ item.credentialDefinitionId | credentialTag }}</div>
        <div v-else>{{ item.type | credentialLabel }}</div>
    </template>
    <template v-slot:[`item.state`]="{ item }">
        <v-icon v-if="item.state === 'verified' || item.state== 'presentation_acked'" color="green">mdi-check</v-icon>
        <!-- <v-btn v-if="item.indyCredential" color="primary" text>verify</v-btn> -->
    </template>
    <template v-slot:[`item.sentAt`]="{ item }">
       {{item.sentAt | moment("YYYY-MM-DD HH:mm") }}
    </template>
    <template v-slot:[`item.receivedAt`]="{ item }">
       {{item.receivedAt | moment("YYYY-MM-DD HH:mm") }}
    </template>
    <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
            <Credential v-bind:document="item" isReadOnly showOnlyContent></Credential>
        </td>
    </template>
    <template v-slot:[`item.actions`]="{ item }">
      <v-icon
        small
        @click="deleteItem(item)"
      >
        mdi-delete
      </v-icon>
    </template>
</v-data-table>
</template>

<script>
import Credential from "@/components/Credential";
import {
    EventBus
} from "../main";
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
                    text: "State",
                    value: "state"
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
        Credential
    }
};
</script>