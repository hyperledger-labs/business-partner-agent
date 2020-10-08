<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<AbstractDocumentEditor v-slot:default="slotProps" v-bind:type="PROFILE" v-bind:id="this.id" v-bind:add="this.add">
    <v-card-text>
        <OganizationalProfile v-bind:document="slotProps.document" />
    </v-card-text>
</AbstractDocumentEditor>
</template>

<script>
import {
    CredentialTypes
} from "../constants";
import AbstractDocumentEditor from "@/components/AbstractDocumentEditor";
import OganizationalProfile from "@/components/OrganizationalProfile";
export default {
    name: "OrganizationalProfileEditor",
    props: {
        id: String, // ignore if add==true
        add: Boolean // if a document should be added instead of edited
    },
    data: () => {
        return {
            PROFILE: CredentialTypes.PROFILE.name
        };
    },
    components: {
        OganizationalProfile,
        AbstractDocumentEditor
    }
};
</script>
