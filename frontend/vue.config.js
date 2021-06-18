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
  configureWebpack: {
    devtool: "source-map",
  },
};
