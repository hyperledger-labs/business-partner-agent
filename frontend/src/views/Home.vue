<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-container text-center>
    <div v-if="isWelcome">
        <!-- Image from undraw.co -->
        <v-img class="mx-auto" src="@/assets/undraw_welcome_3gvl_grey.png" max-width="50%" aspect-ratio="1"></v-img>
        <p v-bind:style="{ fontSize: `180%` }" class="grey--text text--darken-2 font-weight-medium">Hi, we've already set up an identity for you!</p>
        <p v-bind:style="{ fontSize: `140%` }" class="grey--text text--darken-2 font-weight-medium">Start by adding a public profile that your business partners will see</p>
        <br />
        <v-btn color="primary" :to="{ name: 'Profile', params: { add: true} }">Setup your Profile</v-btn>
    </div>
    <v-row v-else>
        <v-col>
            <v-card class="mx-auto" max-width="400" :to="{ name: 'Wallet' }">
                <v-img class="align-end" height="300px" src="@/assets/undraw_certification_aif8.png">
                    
                </v-img>
                 <v-card-title style="font-size:400%" class="justify-center">{{ status.credentials }}</v-card-title>
                <v-card-title  class="justify-center">Verified Credentials</v-card-title>

            </v-card>
        </v-col>
        <v-col>
          <v-card class="mx-auto" max-width="400" :to="{ name: 'Partners' }">
                <v-img class="align-end" height="300px" src="@/assets/undraw_agreement_aajr.png">
                    
                </v-img>
                 <v-card-title style="font-size:400%" class="justify-center">{{ status.partners }}</v-card-title>
                <v-card-title  class="justify-center">Business Partners</v-card-title>

            </v-card>
        </v-col>

    </v-row>
</v-container>
</template>

<script>
import {
    EventBus
} from "../main";
export default {
    name: "Home",
    created() {
        EventBus.$emit("title", "Home")
        this.getStatus()

    },
    data: () => {
        return {
            isWelcome: true
        };
    },
    methods: {
        getStatus() {
            console.log('Getting status...')
            this.$axios.get(`${this.$apiBaseUrl}/status`)
                .then((result) => {
                    console.log(result);
                    this.isWelcome = !result.data.profile
                    this.status = result.data;
                })
                .catch((e) => {
                    console.error(e)
                    EventBus.$emit('error', e)
                });
        }

    }
};
</script>