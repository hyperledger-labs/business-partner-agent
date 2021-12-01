/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
  },
  plugins: ["unicorn"],
  extends: [
    "plugin:vue/essential",
    "eslint:recommended",
    "plugin:unicorn/recommended",
  ],
  parserOptions: {
    parser: "@babel/eslint-parser",
  },
  rules: {
    "no-console": process.env.NODE_ENV === "production" ? "warn" : "off",
    "no-debugger": process.env.NODE_ENV === "production" ? "warn" : "off",
    "vue/multi-word-component-names": "off",
    "vue/no-mutating-props": "warn",
    "vue/valid-v-slot": "warn",
    "unicorn/prefer-module": "off",
    "unicorn/filename-case": "off",
  },
};
