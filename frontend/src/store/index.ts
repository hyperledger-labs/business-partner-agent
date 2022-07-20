/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { createStore } from "vuex";
import {
  chat,
  notifications,
  socketEvents,
  taa,
  credentialDefinitions,
  schemas,
  settings,
  partners,
  partnerSelectList,
  proofTemplates,
  credentialsAndDocuments,
  expertMode,
  tags,
  status,
} from "@/store/modules";

const store = createStore({
  modules: {
    taa,
    socketEvents,
    chat,
    notifications,
    credentialDefinitions,
    schemas,
    settings,
    partners,
    partnerSelectList,
    proofTemplates,
    credentialsAndDocuments,
    tags,
    expertMode,
    status,
  },
});

export default store;
