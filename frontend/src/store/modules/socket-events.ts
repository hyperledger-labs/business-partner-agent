/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import Vue from "vue";
import { IStateSocketEvents } from "@/store/state-type";

const state: IStateSocketEvents = {
  socket: {
    isConnected: false,
    message: "",
    reconnectError: false,
  },
};

const getters = {};

const mutations = {
  SOCKET_ONOPEN(state: IStateSocketEvents, event) {
    console.log(event);
    Vue.prototype.$socket = event.currentTarget;
    state.socket.isConnected = true;
  },
  SOCKET_ONCLOSE(state: IStateSocketEvents, event) {
    console.log(event);
    state.socket.isConnected = false;
  },
  SOCKET_ONERROR(state: IStateSocketEvents, event) {
    console.error(state, event);
  },
  // default handler called for all methods
  SOCKET_ONMESSAGE(state: IStateSocketEvents, message) {
    console.log(message);
    state.socket.message = message;
  },
  // mutations for reconnect methods
  SOCKET_RECONNECT(state: IStateSocketEvents, count) {
    console.info(state, count);
  },
  SOCKET_RECONNECT_ERROR(state: IStateSocketEvents) {
    state.socket.reconnectError = true;
  },
};

const actions = {};

export default {
  state,
  getters,
  actions,
  mutations,
};
