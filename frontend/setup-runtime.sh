#!/bin/sh
# Copyright (c) 2020-2021 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/business-partner-agent
#
# SPDX-License-Identifier: Apache-2.0

# First argument: Path to env.js which needs to be modified
# Second argument: Path to BPA jar file to replace env.js
echo "Start setting runtime variables"

# Overwrite frontend runtime variables with environment variables from container
sed -i 's#__SIDEBAR_CLOSE_ON_STARTUP__#'"$SIDEBAR_CLOSE_ON_STARTUP"'#g' "$1"
sed -i 's#__SIDEBAR_HIDE_BURGER_BUTTON__#'"$SIDEBAR_HIDE_BURGER_BUTTON"'#g' "$1"

# Overwrite env.js in given jar file (path)
jar uf "$2" "$1"

echo "Finish setting runtime variables"
