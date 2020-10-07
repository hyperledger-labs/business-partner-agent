<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
<v-container>
    <v-data-table
    :hide-default-footer="data.length < 10" 
    v-model="selected" 
    :loading="isBusy" 
    :headers="headers" 
    :items="data" 
    :show-select="selectable" 
    single-select
    @click:row="open"
    >
        <template v-slot:[`item.name`]="{ item }">
            <PartnerStateIndicator v-if="item.state" v-bind:state="item.state"></PartnerStateIndicator>
            <span class="font-weight-medium"> {{ item.name }}</span>
        </template>

        <template v-slot:[`item.createdAt`]="{ item }">
            {{ item.createdAt | moment("YYYY-MM-DD HH:mm") }}
        </template>

        <template v-slot:[`item.updatedAt`]="{ item }">
            {{ item.updatedAt | moment("YYYY-MM-DD HH:mm") }}
        </template>
    </v-data-table>
</v-container>
</template>

<script>
import { EventBus } from "../main";
import { getPartnerProfile, getPartnerName } from "../utils/partnerUtils";
import PartnerStateIndicator from "@/components/PartnerStateIndicator"; 
export default {
    name: "PartnerList",
    components: {
        PartnerStateIndicator
    },
    props: {
        selectable: {
            type: Boolean,
            default: false
        },
        headers: {
            type: Array,
            default: () => [{
                text: "Name",
                value: "name"
            }]
        },
        onlyAries: {
            type: Boolean,
            default: false
        }
    },
    created() {

        this.fetch()

    },
    data: () => {
        return {
            selected: [],
            data: [],
            isBusy: true
        }
    },
    computed: {
        expertMode() {
            return this.$store.state.expertMode;
        }
    },
    methods: {
        open(partner) {

            this.$router.push({
                    name: 'Partner',
                    params: {
                        id: partner.id,
                    }
                });

        },
        fetch() {
            this.$axios.get(`${this.$apiBaseUrl}/partners`)
                .then((result) => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, 'data')) {

                        this.isBusy = false

                        if (this.onlyAries) {
                            result.data = result.data.filter(item => {
                                return item.ariesSupport === true
                            })
                        }

                        // Get profile of each partner and merge with partner data
                        this.data = result.data.map(partner => {
                            let profile = getPartnerProfile(partner)
                            if (profile) {
                                delete Object.assign(profile, {
                                    ['did']: profile['id']
                                })['id'];
                            }
                            delete partner.credential
                            partner.profile = profile
                            partner.name = getPartnerName(partner)
                            return partner
                        })

                        console.log(this.data)

                    }
                })
                .catch((e) => {

                    this.isBusy = false

                    console.error(e)
                    EventBus.$emit('error', e)

                });
        }
    }
};
</script>
