<!--
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
-->

<template>
<v-card-text>
    <v-form ref="mdForm">
        <v-row>
            <v-col cols="4">
                <p class="grey--text text--darken-2 font-weight-medium">Company Information</p>
            </v-col>
            <v-col cols="8">
                <v-text-field label="Organization Type" placeholder v-model="subject.type" outlined disabled dense></v-text-field>
                <v-text-field label="Company Legal Name" placeholder v-model="subject.legalName" :disabled="isReadOnly" :rules="[v => !!v || 'Item is required']" required outlined dense></v-text-field>
                <v-text-field label="Company Alternative Name" placeholder v-model="subject.altName" :disabled="isReadOnly" required outlined dense></v-text-field>
                <v-row v-for="(identifier, index) in subject.identifier" v-bind:key="identifier.id">
                    <v-col cols="4">
                        <v-select label="Identifier" v-model="identifier.type" :items="identifierTypes" :disabled="isReadOnly" outlined dense></v-select>
                    </v-col>
                    <v-col cols="6">
                        <v-text-field placeholder v-model="identifier.id" :disabled="isReadOnly" outlined dense></v-text-field>
                    </v-col>
                    <v-col cols="2">
                        <v-layout>
                            <v-btn v-if="!isReadOnly && index === subject.identifier.length - 1" color="primary" text @click="addIdentifier()">Add</v-btn>
                            <v-btn icon v-if="!isReadOnly && index !== subject.identifier.length - 1" @click="deleteIdentifier(index)">
                                <v-icon color="error">mdi-delete</v-icon>
                            </v-btn>
                        </v-layout>
                    </v-col>
                </v-row>
            </v-col>
        </v-row>
        <v-divider></v-divider>
        <v-row>
            <v-col cols="4">
                <p class="grey--text text--darken-2 font-weight-medium">Address Information</p>
            </v-col>
            <v-col cols="8">
                <v-text-field label="Street (with number)" placeholder v-model="subject.registeredSite.address.streetAddress" :disabled="isReadOnly" outlined dense></v-text-field>

                <v-row>
                    <v-col cols="4">
                        <v-text-field label="Postal Code" placeholder v-model="subject.registeredSite.address.zipCode" :disabled="isReadOnly" outlined dense></v-text-field>
                    </v-col>
                    <v-col cols="8">
                        <v-text-field label="City" placeholder v-model="subject.registeredSite.address.city" :disabled="isReadOnly" outlined dense></v-text-field>
                    </v-col>
                </v-row>

                <v-row>
                    <v-col cols="6">
                        <v-text-field label="Country" placeholder v-model="subject.registeredSite.address.country" :disabled="isReadOnly" outlined dense></v-text-field>
                    </v-col>
                    <v-col cols="6">
                        <v-text-field label="Region" placeholder v-model="subject.registeredSite.address.region" :disabled="isReadOnly" outlined dense></v-text-field>
                    </v-col>
                </v-row>
            </v-col>
        </v-row>
        <v-divider></v-divider>
        <!-- <v-row id="contact-person">
        <v-col cols="4">
          <p class="grey--text text--darken-2 font-weight-medium">Contact Persons</p>
        </v-col>
        <v-col cols="8">
          <v-list>
            <v-list-item v-for="(person, index) in subject.contactPerson" v-bind:key="index">
              <v-list-item-content>
                <v-list-item-title>
                  {{ person.firstName }}
                  {{ person.lastName }}
                </v-list-item-title>
                <v-list-item-subtitle>{{ person.role }}</v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-layout>
                  <v-btn
                    icon
                    :to="{
                        name: 'ContactPerson',
                        params: { person: person },
                      }"
                  >
                    <v-icon color="secondary">mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn icon @click="deleteContactPerson(index)">
                    <v-icon color="error">mdi-delete</v-icon>
                  </v-btn>
                </v-layout>
              </v-list-item-action>
            </v-list-item>
          </v-list>

          <v-btn
            color="primary"
            text
            :disabled="isReadOnly"
            :to="{ name: 'ContactPerson', params: { person: {} } }"
          >Add Contact Person</v-btn>
        </v-col>
      </v-row>-->
    </v-form>
</v-card-text>
</template>

<script>
export default {
    props: {
        isReadOnly: Boolean,
        document: Object
    },
    data: () => {
        return {
            identifierTypes: ["LEI", "D-U-N-S", "VAT", "USCC"],
            orgTypes: ["Legal Entity", "Business Unit", "Site"]
        };
    },
    computed: {
        subject() {
            return this.document;
        },
    },
    methods: {
        addIdentifier() {
            this.subject.identifier.push({
                identifierType: "",
                identifier: ""
            });
        },
        deleteIdentifier(i) {
            this.subject.identifier.splice(i, 1);
        },
        deleteContactPerson(i) {
            this.subject.contactPerson.splice(i, 1);
        }
    }
};
</script>
