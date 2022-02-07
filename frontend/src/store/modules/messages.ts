/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { StateMessages } from "@/store/state-type";

const messagesState: StateMessages = {
  messages: [],
};

export default {
  state: messagesState,
  getters: {
    messages: (state: StateMessages) => {
      return state.messages;
    },
    messagesCount: (state: StateMessages) => {
      return state.messages.length;
    },
  },
  actions: {},
  mutations: {
    onMessageReceived(state: StateMessages, payload: any) {
      const basicMessage = payload.message.info;

      const msgs: any[] = state.messages ? state.messages : [];
      msgs.push(basicMessage);
      state.messages = msgs;
    },
    markMessagesSeen(state: StateMessages, partnerId: string) {
      // seen means we remove them from the store for a given partner/room
      const msgs: any[] = state.messages ? state.messages : [];
      state.messages = msgs.filter((m) => m.partnerId !== partnerId);
    },
  },
};
