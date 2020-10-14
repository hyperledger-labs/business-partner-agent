<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
    <v-container text-center>
        <v-card flat v v-if="!isLoading" class="mx-auto">
            <v-text-field
                id="did"
                v-model="status.did"
                readonly
                outlined
                dense
                label="DID"
                :append-icon="'mdi-content-copy'"
                @click:append="copyDid"
            ></v-text-field>
        </v-card>
        <div v-if="isWelcome && !isLoading">
            <!-- Image from undraw.co -->
            <v-img
                class="mx-auto"
                src="@/assets/undraw_welcome_3gvl_grey.png"
                max-width="300"
                aspect-ratio="1"
            ></v-img>
            <p
                v-bind:style="{ fontSize: `180%` }"
                class="grey--text text--darken-2 font-weight-medium"
            >
                Hi, we've already set up an identity for you!
            </p>
            <!-- <p v-bind:style="{ fontSize: `140%` }" class="grey--text text--darken-2 font-weight-medium">Start by adding a public profile that your business partners will see</p> -->
            <br />
            <v-btn
                color="primary"
                :to="{
                    name: 'DocumentAdd',
                    params: { type: 'ORGANIZATIONAL_PROFILE_CREDENTIAL' }
                }"
                >Setup your Profile</v-btn
            >
        </div>
        <div v-if="!isWelcome && !isLoading">
            <v-row>
                <v-col class="col-sm-6">
                    <v-card class="mx-auto" :to="{ name: 'Wallet' }">
                        <v-img
                            class="align-end"
                            src="@/assets/undraw_certification_aif8.png"
                        ></v-img>
                        <v-card-title
                            style="font-size:400%"
                            class="justify-center"
                            >{{ status.credentials }}</v-card-title
                        >
                        <v-card-title class="justify-center"
                            >Verified Credentials</v-card-title
                        >
                    </v-card>
                </v-col>
                <v-col class="col-sm-6">
                    <v-card class="mx-auto" :to="{ name: 'Partners' }">
                        <!-- FIXME Used aspect ratio as a hacky way to make the cards the same height -->
                        <v-img
                            class="align-end"
                            aspect-ratio="1.29"
                            src="@/assets/undraw_agreement_aajr.png"
                        ></v-img>
                        <v-card-title
                            style="font-size:400%"
                            class="justify-center"
                            >{{ status.partners }}</v-card-title
                        >
                        <v-card-title class="justify-center"
                            >Business Partners</v-card-title
                        >
                    </v-card>
                </v-col>
            </v-row>
        </div>
    </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
    name: "Dashboard",
    created() {
        EventBus.$emit("title", "Dashboard");
        this.getStatus();
    },
    data: () => {
        return {
            isWelcome: true,
            isLoading: true
        };
    },
    methods: {
        getStatus() {
            console.log("Getting status...");
            this.$axios
                .get(`${this.$apiBaseUrl}/status`)
                .then(result => {
                    console.log(result);
                    this.isWelcome = !result.data.profile;
                    this.status = result.data;
                    this.isLoading = false;
                })
                .catch(e => {
                    console.error(e);
                    this.isLoading = false;
                    EventBus.$emit("error", e);
                });
        },
        copyDid() {
            let didEl = document.querySelector("#did");
            didEl.select();
            let successfull;
            try {
                successfull = document.execCommand("copy");
            } catch (err) {
                successfull = false;
            }
            successfull
                ? EventBus.$emit("success", "DID copied")
                : EventBus.$emit("error", "Can't copy DID");
            didEl.blur();
            window.getSelection().removeAllRanges();
        }
    }
};
</script>
