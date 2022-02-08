#!/bin/sh
# Copyright (c) 2020-2022 - for information on the respective copyright owner
# see the NOTICE file and/or the repository at
# https://github.com/hyperledger-labs/business-partner-agent
#
# SPDX-License-Identifier: Apache-2.0

# Keep this shell script POSIX compliant
# First argument: Path to env.js which needs to be modified
# Second argument: Path to BPA jar file to replace env.js
echo "Start setting runtime variables"

# Add new runtime variable names in this string delimited by a space character ' '
definedVariables="SIDEBAR_CLOSE_ON_STARTUP SIDEBAR_HIDE_BURGER_BUTTON"
modifiedVariablesCounter=0

for item in $definedVariables ; do # Do not use double-quotes for $definedVariables
  eval envvar=\"\$"$item"\"
  if [ -n "$envvar" ]; then
    echo "Runtime variable $item is set to '$envvar'"

    # Overwrite frontend runtime variables in env.js with environment variables from container
    sed -i "s#__${item}__#""$envvar"'#g' "$1"
    modifiedVariablesCounter=$((modifiedVariablesCounter+1))
  fi
done

# Overwrite env.js file in given jar file (path) if at least one runtime variable is set
if [ "$modifiedVariablesCounter" -gt 0 ]; then
  echo "Updating env.js file in jar"
  jar uf "$2" "$1"
fi

echo "Finish setting runtime variables"
