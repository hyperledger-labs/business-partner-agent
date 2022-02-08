<!--
 Copyright (c) 2020-2022 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/business-partner-agent

 SPDX-License-Identifier: Apache-2.0
-->
<template>
  <v-form ref="form" v-model="valid" lazy-validation>
    <div style="min-width: 250px">
      <v-text-field
        outlined
        dense
        :rules="hexReg"
        required
        :value="intColorField"
        @input="changeColor($event)"
        maxlength="7"
        color="intColor"
      >
        <template v-slot:append>
          <v-menu
            v-model="menu"
            top
            nudge-bottom="300"
            nudge-left="0"
            :close-on-content-click="false"
          >
            <template v-slot:activator="{ on }">
              <div :style="swatchStyle" v-on="on" />
            </template>
            <v-card>
              <v-card-text class="pa-0">
                <v-color-picker
                  v-model="intColor"
                  hide-mode-switch
                  flat
                  type="hex"
                >
                </v-color-picker>
              </v-card-text>
            </v-card>
          </v-menu>
          <span>
            <v-btn class="" x-small text @click="cancel()">{{
              $t("button.cancel")
            }}</v-btn>
            <v-btn class="" color="#000" x-small text @click="saveColor()">
              {{ $t("button.save") }}
            </v-btn>
          </span>
        </template>
      </v-text-field>
    </div>
  </v-form>
</template>
<script lang="ts">
export default {
  props: {
    mode: {
      type: String,
      default() {
        return "hex";
      },
    },
    baseColor: {
      type: String,
      default() {
        return this.$vuetify.theme.themes.light.primary;
      },
    },
  },
  data() {
    return {
      valid: true,
      intColorField: "",
      intColor: "",
      menu: false,
      hexReg: [
        (value: string) => !!value || this.$t("app.rules.colorPrefix"),
        (value: string) => {
          const pattern = /^#([\da-f]{3}){1,2}$/i;
          return pattern.test(value) || this.$t("app.rules.colorInvalid");
        },
      ],
    };
  },
  computed: {
    swatchStyle() {
      const { intColor, menu } = this;
      this.pickColor(intColor);
      return {
        backgroundColor: intColor,
        cursor: "pointer",
        height: "20px",
        width: "20px",
        borderRadius: menu ? "50%" : "4px",
        transition: "border-radius 200ms ease-in-out",
        border: "1px solid black",
      };
    },
  },
  created() {
    this.intColor = this.baseColor;
    this.intColorField = this.baseColor;
  },
  methods: {
    saveColor() {
      this.$emit("on-save", this.intColor);
    },
    cancel() {
      this.$emit("on-cancel");
    },
    changeColor(value: string) {
      if (this.$refs.form.validate()) {
        this.intColor = value;
      }
    },
    pickColor(newColor: string) {
      this.intColorField = newColor;
    },
  },
};
</script>
