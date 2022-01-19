/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
process.env.VUE_APP_VERSION = require("./package.json").version;

module.exports = {
  devServer: {
    disableHostCheck: true,
    proxy: {
      "^/api/*": {
        target: "http://localhost:8080",
        ws: true,
      },
    },
  },
  transpileDependencies: ["vuetify"],
  pluginOptions: {
    i18n: {
      locale: "en",
      fallbackLocale: "en",
      localeDir: "locales",
      enableInSFC: false,
    },
  },
  configureWebpack: {
    devtool: "source-map",
  },
};
