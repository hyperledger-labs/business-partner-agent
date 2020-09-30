<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-container>
    <v-card class="mx-auto px-8">
        <!--
        <v-card-title>
            <v-text-field v-model="search" prepend-icon="mdi-magnify" label="Search" single-line hide-details></v-text-field>
        </v-card-title>
        -->
        <v-data-table hide-default-footer  :headers="headers" :items="partners" :search="search" :loading="isBusy">
            <template v-slot:item="{ item }">
                <router-link tag="tr" :to="`/app/partners/${item.id}`">
                    <td class="font-weight-medium">{{ item.name }}</td>
                    <td> {{ item.createdAt | moment("YYYY-MM-DD HH:mm") }}</td>
                    <td>{{ item.updatedAt ? item.updatedAt : item.createdAt | moment("YYYY-MM-DD HH:mm") }}</td>
                </router-link>
            </template>
        </v-data-table>
         <v-card-actions>
        <v-btn color="primary" small dark absolute bottom left fab :to="{ name: 'AddPartner' }">
            <v-icon>mdi-plus</v-icon>
        </v-btn>
        </v-card-actions>
    </v-card>
</v-container>
</template>

<script>
import {
    EventBus
} from '../main'
import {
    getPartnerProfile, getPartnerName
} from '../utils/partnerUtils'
export default {
    name: "Partners",
    created() {
        EventBus.$emit('title', 'Business Partners')
        this.getPartners();
    },
    data: () => {
        return {
            isBusy: true,
            search: '',
            headers: [{
                    text: "Name",
                    // value: "subject.companyName"
                },
                {
                    text: "Created",
                    // value: "createdDate"
                },
                {
                    text: "Last Updated",
                    // value: "createdDate"
                }

            ],
            partners: []
        };
    },
    methods: {
        getPartners() {
            this.$axios.get(`${this.$apiBaseUrl}/partners`)
                .then((result) => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, 'data')) {

                        this.isBusy = false

                        this.partners = result.data.map(partner => {
                            partner.profile = getPartnerProfile(partner)
                            partner.name = getPartnerName(partner)
                            return partner
                        })

                    }
                })
                .catch((e) => {
                    console.error(e)
                    this.isBusy = false
                    EventBus.$emit('error', e)
                });
        },
    }
};
</script>
