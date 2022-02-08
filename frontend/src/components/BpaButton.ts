/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { VBtn } from "vuetify/lib";
import Routable from "vuetify/es5/mixins/routable/index";

export default {
  name: "v-bpa-button",
  extends: VBtn,

  computed: {
    // see if we have a loaded configuration override.
    // we will use the color property to determine our config
    // ux.buttons.primary or ux.buttons.secondary...
    getRuntimeConfiguration() {
      const runtimeConfig = {
        primary: {
          text: false,
          outlined: false,
        },
        secondary: {
          text: true,
          outlined: false,
        },
      };
      // check for configuration overrides from the server...
      if (
        this.$config &&
        this.$config.ux &&
        this.$config.ux.buttons &&
        this.$config.ux.buttons[this.color]
      ) {
        Object.assign(
          runtimeConfig[this.color],
          this.$config.ux.buttons[this.color]
        );
      }
      // return the config for primary or secondary (or return undefined)
      return runtimeConfig[this.color];
    },
    // since we are mutating properties, we have to compute our value and use it wherever VBtn was performing logic with the property
    _text() {
      const config = this.getRuntimeConfiguration;
      if (config) {
        return config.text;
      }
      return this.text;
    },
    _outlined() {
      const config = this.getRuntimeConfiguration;
      if (config) {
        return config.outlined;
      }
      return this.outlined;
    },
    // replace the VBtn classes computation with our new "properties"
    classes() {
      return {
        "v-btn": true,
        ...Routable.options.computed.classes.call(this),
        "v-btn--absolute": this.absolute,
        "v-btn--block": this.block,
        "v-btn--bottom": this.bottom,
        "v-btn--disabled": this.disabled,
        "v-btn--is-elevated": this.isElevated,
        "v-btn--fab": this.fab,
        "v-btn--fixed": this.fixed,
        "v-btn--has-bg": this.hasBg,
        "v-btn--icon": false,
        "v-btn--left": this.left,
        "v-btn--loading": this.loading,
        "v-btn--outlined": this._outlined,
        "v-btn--plain": this.plain,
        "v-btn--right": this.right,
        "v-btn--round": this.isRound,
        "v-btn--rounded": this.rounded,
        "v-btn--router": this.to,
        "v-btn--text": this._text,
        "v-btn--tile": this.tile,
        "v-btn--top": this.top,
        ...this.themeClasses,
        ...this.groupClasses,
        ...this.elevationClasses,
        ...this.sizeableClasses,
      };
    },
    // replace the VBtn hasBg computation with our new "properties"
    hasBg() {
      return !this._text && !this.plain && !this._outlined && !this.icon;
    },
    // replace the VBtn isElevated computation with our new "properties"
    isElevated() {
      return Boolean(
        !this.icon &&
          !this._text &&
          !this._outlined &&
          !this.depressed &&
          !this.disabled &&
          !this.plain &&
          (this.elevation === undefined || Number(this.elevation) > 0)
      );
    },
  },
};
