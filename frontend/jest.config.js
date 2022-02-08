/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
module.exports = {
  preset: "@vue/cli-plugin-unit-jest/presets/typescript-and-babel",
  verbose: true,
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
    "^@@/(.*)$": "<rootDir>/test-config/$1",
  },
  collectCoverage: false,
  coverageDirectory: "coverage",
  collectCoverageFrom: [
    "src/**/*.{ts,js,vue}",
    "public/**/*.{ts,js,vue}",
    "!**/node_modules/**",
    "!**/dist/**",
  ],
  transformIgnorePatterns: [
    "/node_modules/(?!vuetify|vue-markdown-render|vue-advanced-chat|vue-json-pretty|qrcode.vue)",
  ],
  testEnvironment: "jest-environment-jsdom-sixteen",
  testMatch: ["**/*.spec.{ts,js}", "!**/node_modules/**"],
  setupFiles: ["<rootDir>/test-config/setup-jest.ts"],
};
