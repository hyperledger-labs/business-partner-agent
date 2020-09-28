<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->
<template>
<v-container justify-center>
<<<<<<< HEAD
    <v-card v-if="!isLoading" class="mx-auto" flat>
=======
    <v-card class="mx-auto" flat>
>>>>>>> schema
        <v-card-title class="bg-light">
            <v-btn depressed color="secondary" icon @click="$router.go(-1)">
                <v-icon dark>mdi-chevron-left</v-icon>
            </v-btn>
            <span>{{data.label}}</span>
<<<<<<< HEAD
            <v-layout align-end justify-end>
                <!-- <v-btn depressed icon @click="isUpdatingName = !isUpdatingName">
                    <v-icon dark>mdi-pencil</v-icon>
                </v-btn> -->
                <v-btn depressed color="red" icon @click="deleteSchema">
                    <v-icon dark>mdi-delete</v-icon>
                </v-btn>
            </v-layout>

        </v-card-title>

        <v-list-item class="mt-4">
            <v-list-item-title class="grey--text text--darken-2 font-weight-medium">
                Schema ID
            </v-list-item-title>
        <v-list-item-subtitle>
            {{data.schemaId}}
        </v-list-item-subtitle>
        </v-list-item>

        <h4 class="my-4 grey--text text--darken-3">Schema Attributes</h4>

        <v-list-item v-for="attribute in data.schemaAttributeNames" :key="attribute.id">

            <p class="grey--text text--darken-2 font-weight-medium">
                {{attribute}}
            </p>

        </v-list-item>

=======
            <v-btn icon @click="deleteSchema">
                <v-icon>
                    mdi-delete
                </v-icon>
            </v-btn>
        </v-card-title>

>>>>>>> schema
    </v-card>
</v-container>
</template>

<script>
import {
    EventBus
} from "../main";
export default {
    name: "Schema",
<<<<<<< HEAD
    props: {
        id: String, //schema ID
        schema: Object
    },
    created() {
        EventBus.$emit("title", "Schema");

        if (this.schema) {
            this.isLoading = false
            this.data = this.schema;
        } else {
            this.fetch();
        }
=======
     props: {
        id: String, //schema ID
    },
    created() {
        EventBus.$emit("title", "Schema");
        this.fetch()
>>>>>>> schema
    },
    data: () => {
        return {
            data: [],
<<<<<<< HEAD
            isLoading: true,

=======
            isBusy: true,
            
>>>>>>> schema
        };
    },
    computed: {

    },
    methods: {
        fetch() {
<<<<<<< HEAD
            this.isLoading = true;
            this.$axios.get(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
=======
            this.$axios.get(`${this.$apiBaseUrl}/schema/${this.id}`)
>>>>>>> schema
                .then((result) => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, 'data')) {

<<<<<<< HEAD
                        this.data = result.data;
                        this.isLoading = false;
=======
                        this.isBusy = false

                        this.data = result.data
>>>>>>> schema

                    }
                })
                .catch((e) => {

<<<<<<< HEAD
                    this.isLoading = false
=======
                    this.isBusy = false
>>>>>>> schema
                    if (e.response.status === 404) {

                        this.data = []

                    } else {
                        console.error(e)
                        EventBus.$emit('error', e)
                    }

                });
        },
<<<<<<< HEAD

=======
        
>>>>>>> schema
        deleteSchema() {
            this.$axios
                .delete(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
                .then(result => {
                    console.log(result);
                    if (result.status === 200) {
                        EventBus.$emit("success", "Schema deleted");
<<<<<<< HEAD
                        this.$router.push({
                            name: "SchemaSettings"
                        })
=======
                        this.$router.push({ name: "SchemaSettings"})
>>>>>>> schema
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
