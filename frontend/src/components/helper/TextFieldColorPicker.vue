<template>
  <div style="min-width: 250px">
    <v-text-field
      v-model="intColor"
      hide-details
      outlined
      dense
      :rules="hexReg"
      required
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
              <v-color-picker v-model="intColor" flat hide-mode-switch>
              </v-color-picker>
            </v-card-text>
          </v-card>
        </v-menu>
        <span justify>
          <v-btn class="" x-small text @click="cancel()"> Cancel </v-btn>
          <v-btn class="" x-small text color="primary" @click="saveColor()">
            Save
          </v-btn>
        </span>
      </template>
    </v-text-field>
  </div>
</template>
<script>
export default {
  props: {
    mode: {
      type: String,
      default() {
        return "hex";
      },
    },
    currentColor: {
      type: String,
      default() {
        return this.$vuetify.theme.themes.light.primary;
      },
    },
  },
  data() {
    return {
      intColor: "",
      menu: false,
      hexReg: [
        (v) => !!v || "#EEEEEE",
        (v) => /^#([0-9A-F]{3}){1,2}$/i.test(v) || "Color should be valid",
      ],
    };
  },
  computed: {
    swatchStyle() {
      const { intColor, menu } = this;
      return {
        backgroundColor: intColor,
        cursor: "pointer",
        height: "20px",
        width: "20px",
        borderRadius: menu ? "50%" : "4px",
        transition: "border-radius 200ms ease-in-out",
      };
    },
  },
  created() {
    this.intColor = this.currentColor;
  },
  methods: {
    saveColor() {
      this.$emit("on-save", this.intColor);
    },
    cancel() {
      this.$emit("on-cancel");
    },
  },
};
</script>
