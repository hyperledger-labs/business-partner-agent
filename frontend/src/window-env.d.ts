/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
export {};

interface RuntimeVariables {
  SIDEBAR_CLOSE_ON_STARTUP: string;
  SIDEBAR_HIDE_BURGER_BUTTON: string;
}

declare global {
  interface Window {
    env: RuntimeVariables;
  }
}
