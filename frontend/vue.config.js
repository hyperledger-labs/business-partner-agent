process.env.VUE_APP_VERSION = require("./package.json").version;

module.exports = {
  devServer: {
    proxy: {
      "^/api": {
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
  devServer: {
    disableHostCheck: true,
  },
};
