<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-container justify-center>
    <v-card class="mx-auto" flat>
        <v-card-title class="bg-light">
            <v-btn depressed color="secondary" icon @click="$router.go(-1)">
                <v-icon dark>mdi-chevron-left</v-icon>
            </v-btn>
            <span>Schemas</span>
        </v-card-title>
        <v-data-table 
            :hide-default-footer="data.length < 10"
            :headers="headers" 
            :items="data" 
            :loading="isBusy"
            @click:row="open"    
        >
            <template v-slot:[`item.actions`]="{ item }">
                <v-icon small @click="deleteSchema(item.id)">
                    mdi-delete
                </v-icon>
            </template>
        </v-data-table>

        <v-card-title class="grey--text text--darken-2">Add Schema</v-card-title>

        <v-row>

            <v-col cols="4">
                <v-text-field placeholder="Name" v-model="newSchema.label" outlined dense required></v-text-field>
            </v-col>
            <v-col cols="6">
                <v-text-field placeholder="Schema ID" v-model="newSchema.schemaId" outlined dense required>
                </v-text-field>
            </v-col>
            <v-col cols="2">
                <v-btn :loading="this.isBusyAddSchema" color="primary" class="" @click="addSchema">
                    Submit
                </v-btn>
            </v-col>

        </v-row>

    </v-card>
</v-container>
</template>

<script>
import {
    EventBus
} from "../main";
export default {
    name: "SchemaSettings",
    created() {
        EventBus.$emit("title", "Schema Settings");
        this.fetch()
    },
    data: () => {
        return {
            data: [],
            newSchema: {
                label: '',
                schemaId: ''
            },
            isBusy: true,
            isBusyAddSchema: false,
            headers: [{
                text: 'Name',
                value: 'label'
            }, {
                text: 'Schema ID',
                value: 'schemaId'
            }, {
                text: 'Actions',
                value: 'actions'
            }]
        };
    },
    computed: {

    },
    methods: {
        fetch() {
            this.$axios.get(`${this.$apiBaseUrl}/admin/schema`)
                .then((result) => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, 'data')) {

                        this.isBusy = false

                        this.data = result.data

                        console.log(this.data)

                    }
                })
                .catch((e) => {

                    this.isBusy = false
                    if (e.response.status === 404) {

                        this.data = []

                    } else {
                        console.error(e)
                        EventBus.$emit('error', e)
                    }

                });
        },
        open(schema) {

             this.$router.push({
                    name: 'Schema',
                    params: {
                        id: schema.id
                    }
                });
            
        },
        addSchema() {

            this.isBusyAddSchema = true

            this.$axios
                .post(`${this.$apiBaseUrl}/admin/schema`, this.newSchema)
                .then((result) => {
                    console.log(result);
                    this.isBusyAddSchema = false

                    if (result.status === 200 || result.status === 200   ) {
                        
                        EventBus.$emit("success", "Schema added successfully");
                        this.fetch();
                    }
                })
                .catch((e) => {
                    this.isBusyAddSchema = false
                    if (e.response.status === 400) {
                        EventBus.$emit("error", "Schema already exists");
                    } else {
                        console.error(e);
                        EventBus.$emit("error", e);
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
                        this.fetch()
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
