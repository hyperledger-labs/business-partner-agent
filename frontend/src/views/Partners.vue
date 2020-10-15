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
            <v-data-table
                :hide-default-footer="partners.length < 10"
                :headers="headers"
                :items="partners"
                :search="search"
                :loading="isBusy"
            >
                <template v-slot:item="{ item }">
                    <router-link tag="tr" :to="`/app/partners/${item.id}`">
                        <td class="font-weight-medium">{{ item.name }}</td>
                        <td>
                            {{ item.createdAt | moment("YYYY-MM-DD HH:mm") }}
                        </td>
                        <td>
                            {{
                                item.updatedAt
                                    ? item.updatedAt
                                    : item.createdAt
                                      | moment("YYYY-MM-DD HH:mm")
                            }}
                        </td>
                    </router-link>
                </template>
            </v-data-table>
            <v-card-actions>
                <v-btn
                    color="primary"
                    small
                    dark
                    absolute
                    bottom
                    left
                    fab
                    :to="{ name: 'AddPartner' }"
                >
                    <v-icon>mdi-plus</v-icon>
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-container>
</template>

<script>
import { EventBus } from "../main";
import PartnerList from "@/components/PartnerList";
export default {
    name: "Partners",
    components: {
        PartnerList
    },
    created() {
        EventBus.$emit("title", "Business Partners");
    },
    data: () => {
        return {
            isBusy: true,
            search: "",
            headers: [
                {
                    text: "Name",
                    value: "name"
                },
                {
                    text: "Created",
                    value: "createdAt"
                },
                {
                    text: "Last Updated",
                    value: "updatedAt"
                }
            ],
            partners: []
        };
    },
    methods: {}
};
</script>
