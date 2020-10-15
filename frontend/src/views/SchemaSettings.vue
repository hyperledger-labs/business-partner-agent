<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
    <v-container justify-center>
        <v-card max-width="600" class="mx-auto">
            <v-card-title class="bg-light">
                <v-btn depressed color="secondary" icon @click="$router.go(-1)">
                    <v-icon dark>mdi-chevron-left</v-icon>
                </v-btn>
                <span>Schemas</span>
            </v-card-title>
            <v-data-table
                class="mb-4"
                :hide-default-footer="data.length < 10"
                :headers="headers"
                :items="data"
                :loading="isBusy"
                @click:row="open"
            >
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
                    :to="{ name: 'AddSchema' }"
                >
                    <v-icon>mdi-plus</v-icon>
                </v-btn>
            </v-card-actions>
        </v-card>
    </v-container>
</template>

<script>
import { EventBus } from "../main";
export default {
    name: "SchemaSettings",
    created() {
        EventBus.$emit("title", "Schema Settings");
        this.fetch();
    },
    data: () => {
        return {
            data: [],
            newSchema: {
                label: "",
                schemaId: ""
            },
            isBusy: true,
            isBusyAddSchema: false,
            headers: [
                {
                    text: "Name",
                    value: "label"
                },
                {
                    text: "Schema ID",
                    value: "schemaId"
                }
            ]
        };
    },
    computed: {},
    methods: {
        fetch() {
            this.$axios
                .get(`${this.$apiBaseUrl}/admin/schema`)
                .then(result => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, "data")) {
                        this.isBusy = false;

                        this.data = result.data;

                        console.log(this.data);
                    }
                })
                .catch(e => {
                    this.isBusy = false;
                    if (e.response.status === 404) {
                        this.data = [];
                    } else {
                        console.error(e);
                        EventBus.$emit("error", e);
                    }
                });
        },
        open(schema) {
            this.$router.push({
                name: "Schema",
                params: {
                    id: schema.id,
                    schema: schema
                }
            });
        },
        deleteSchema(schemaId) {
            this.$axios
                .delete(`${this.$apiBaseUrl}/admin/schema/${schemaId}`)
                .then(result => {
                    console.log(result);
                    if (result.status === 200) {
                        EventBus.$emit("success", "Schema deleted");
                        this.fetch();
                    }
                })
                .catch(e => {
                    console.error(e);
                    EventBus.$emit("error", e);
                });
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
