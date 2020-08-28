<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-card flat>
    <v-card-title>
        <v-btn depressed color="secondary" icon @click="cancel()">
            <v-icon dark>mdi-chevron-left</v-icon>Back
        </v-btn>
    </v-card-title>
    <slot v-bind:document="document" v-bind:isLoading="isBusy"></slot>
    <v-card-actions>
        <v-layout align-end justify-end>
            <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
            <v-btn :loading="this.isBusy" color="primary" text @click="submitData()">Save</v-btn>
        </v-layout>
    </v-card-actions>
</v-card>
</template>

<script>
export default {
    name: "DocumentEditor",
    props: {
        type: String,
        id: String, // ignore if add==true
        add: Boolean
    },
    created: function () {
        this.initData();
    },
    computed: {
        document() {
            return this.$store.state.editedDocument.document;
        },
        isBusy() {
            return this.$store.getters.isBusy;
        }
    },

    methods: {
        initData() {
            this.$store.commit({
                type: "initEditDocument",
                documentType: this.type,
                id: this.id,
                add: this.add
            });
        },
        cancel() {
            this.$store.commit("cancelEditDocument");
            this.$router.go(-1);
        },
        submitData() {
            this.$store.dispatch("completeEditDocument").then(() => {
                this.$router.go(-1);
            });
        }
    }
};
</script>
