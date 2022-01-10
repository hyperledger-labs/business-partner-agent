/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
module.exports = {
  preset: "@vue/cli-plugin-unit-jest/presets/typescript-and-babel",
  verbose: true,
  collectCoverage: false,
  coverageDirectory: "coverage",
  collectCoverageFrom: [
    "src/**/*.{ts,js,vue}",
    "public/**/*.{ts,js,vue}",
    "!**/node_modules/**",
    "!**/dist/**",
  ],
  testMatch: ["**/*.spec.{ts,js}", "!**/node_modules/**"],
};
