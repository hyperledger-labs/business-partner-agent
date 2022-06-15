#!/bin/bash
# Copyright (c) 2020-2022 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/business-partner-agent
#
# SPDX-License-Identifier: Apache-2.0

# Update swagger file by building backend
(cd ../backend/ || exit ; mvn clean compile)

# Convert swagger file to raw TypeScript typings
npx openapi-typescript ../backend/business-partner-agent/target/classes/META-INF/swagger/business-partner-agent-0.1.yml --output backend-types.ts

# Add license header
npm run license-file-headers-add
