/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { StateBpa } from "@/store/state-type";

export const isBusy = (state: StateBpa) => {
  return state.busyStack > 0;
};
