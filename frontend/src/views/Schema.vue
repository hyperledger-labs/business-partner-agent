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
            <span>{{data.label}}</span>
            <v-btn icon @click="deleteSchema">
                <v-icon>
                    mdi-delete
                </v-icon>
            </v-btn>
        </v-card-title>

    </v-card>
</v-container>
</template>

<script>
import {
    EventBus
} from "../main";
export default {
    name: "Schema",
     props: {
        id: String, //schema ID
    },
    created() {
        EventBus.$emit("title", "Schema");
        this.fetch()
    },
    data: () => {
        return {
            data: [],
            isBusy: true,
            
        };
    },
    computed: {

    },
    methods: {
        fetch() {
            this.$axios.get(`${this.$apiBaseUrl}/schema/${this.id}`)
                .then((result) => {
                    console.log(result);
                    if ({}.hasOwnProperty.call(result, 'data')) {

                        this.isBusy = false

                        this.data = result.data

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
        
        deleteSchema() {
            this.$axios
                .delete(`${this.$apiBaseUrl}/admin/schema/${this.id}`)
                .then(result => {
                    console.log(result);
                    if (result.status === 200) {
                        EventBus.$emit("success", "Schema deleted");
                        this.$router.push({ name: "SchemaSettings"})
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
