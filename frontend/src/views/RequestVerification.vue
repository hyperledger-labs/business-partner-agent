<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<div>
<v-card class="mx-auto">
    <v-card-title class="bg-light">
        <v-btn depressed color="secondary" icon @click="$router.go(-1);">
            <v-icon dark>mdi-chevron-left</v-icon>
        </v-btn>
        Select Business Partner
    </v-card-title>
    <v-card-text>
        <h4 class="pt-4">Select the business partner you would like to request a verififcation from</h4>
        <PartnerList v-bind:selectable="true" ref="partnerList" v-bind:onlyAries="true"></PartnerList>
    </v-card-text>

    <v-card-actions>
        <v-layout align-end justify-end>
            <v-btn color="secondary" text @click="cancel()">Cancel</v-btn>
            <v-btn :loading="this.isBusy" color="primary" text @click="checkRequest">Submit</v-btn>
        </v-layout>
    </v-card-actions>
</v-card>
<v-dialog v-model="attentionPartnerStateDialog" max-width="500">
      <v-card>
        <v-card-title class="headline">Connection State {{partner.state}} </v-card-title>

        <v-card-text>
          The connection with your Business Partner is marked as {{partner.state}}. This could mean that your request will fail. Do you want to try anyways?
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn color="secondary" text @click="attentionPartnerStateDialog = false">No</v-btn>

          <v-btn color="primary" text @click="submitRequest">Yes</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</div>
</template>

<script>
import {
    EventBus
} from '../main'
import PartnerList from "@/components/PartnerList";
// import { CredentialTypes } from "../constants";
// import VueJsonPretty from "vue-json-pretty";

export default {
    name: "RequestVerification",
    components: {
        PartnerList
    },
    props: {
        documentId: String,
    },
    created() {
        EventBus.$emit('title', 'Request Verification')
    },
    data: () => {
        return {
            document: {},
            isBusy: false,
            isReady: false,
            attentionPartnerStateDialog: false,
            partner: {}
        };
    },
    computed: {},
    methods: {
        checkRequest() {
            if (this.$refs.partnerList.selected.length === 1) {
                if (this.$refs.partnerList.selected[0].id) {

                    this.partner = this.$refs.partnerList.selected[0]

                    if (this.partner.state === 'response' || this.partner.state === 'active') {
                        this.submitRequest();
                    } else {
                        this.attentionPartnerStateDialog = true;
                    }
                }

                    
            } else {
                EventBus.$emit("error", 'No partner for verification request selected');
            }

        },
        submitRequest() {
             this.attentionPartnerStateDialog = false;
            this.isBusy = true;
                    this.$axios
                        .post(`${this.$apiBaseUrl}/partners/${this.partner.id}/credential-request`, {
                            documentId: this.documentId
                        })
                        .then((res) => {
                            console.log(res)
                            this.isBusy = false;
                            EventBus.$emit('success', 'Verification Request sent')
                            this.$router.push({
                                name: 'Wallet'
                            });
                        })
                        .catch((e) => {
                            this.isBusy = false;
                            console.error(e);
                            EventBus.$emit("error", e);
                        });
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
