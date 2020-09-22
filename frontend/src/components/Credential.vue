<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
<v-card-text>
    <v-form ref="mdForm">
        <v-row v-if="!showOnlyContent">
            <v-col cols="8">
                <v-text-field v-if="document.issuer" label="Issuer" v-model="document.issuer" disabled outlined dense></v-text-field>
                <v-text-field v-if="document.issuedAt" label="Issued at" :placeholder="$options.filters.moment(document.issuedAt, 'MMMM Do YYYY, hh:mm')" disabled outlined dense></v-text-field>
            </v-col>
        </v-row>

        <h3 v-if="document.credentialData && !showOnlyContent">Credential Content</h3>
        <v-row>
            <v-col cols="8">
                <v-text-field v-for="field in schema.fields" :key="field.type" :label="field.label" placeholder v-model="documentData[field.type]" :disabled="isReadOnly" :rules="[v => !!v || 'Item is required']" :required="field.required" outlined dense></v-text-field>
            </v-col>
        </v-row>
    </v-form>
</v-card-text>
</template>

<script>
import {
    getSchema
} from "../constants";
export default {
    props: {
        isReadOnly: Boolean,
        document: Object,
        showOnlyContent: Boolean
    },
    created() {
        console.log(this.document)
        // New created document
        if (!{}.hasOwnProperty.call(this.document, 'documentData') && !{}.hasOwnProperty.call(this.document, 'credentialData') && !{}.hasOwnProperty.call(this.document, 'proofData')) {

            this.documentData = Object.fromEntries(this.schema.fields.map(field => {
                return [field.type, '']
            }))
        // Existing document or credential
        } else {
            // Check if document or credential data is here. This needs to be improved
            let documentData
            if (this.document.documentData) {
                documentData = this.document.documentData
            } else if (this.document.credentialData) {
                documentData = this.document.credentialData
            } else if (this.document.proofData) {
                documentData = this.document.proofData

            }

            // Only support one nested node for now
            let nestedData = Object.values(documentData).find(value => {
              return typeof value === 'object' && value !== null
            })

            this.documentData = nestedData ? nestedData : documentData
        }

    },
    data: () => {
        return {

        };
    },
    computed: {
        schema: function () {
            let s = getSchema(this.document.type);
            if (s && {}.hasOwnProperty.call(s, 'fields')) {
                return s

                // No known schema. Generate one from data
                // Todo: Support arrays and objects as fields
            } else {
                s = {
                    type: this.document.type,
                    fields: Object.keys(this.documentData).map((key) => {
                        return {
                            type: key,
                            label: key
                        }

                    })
                }
                console.log(s)
                return s

            }
        }

    },
    methods: {

    }
};
</script>
