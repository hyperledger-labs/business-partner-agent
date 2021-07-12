/*
 Copyright (c) 2021 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import axios from "axios";
import Vue from "vue";

/**
 * @function appAxios
 * Returns an Axios instance with pre-configuration
 * @param {number} [timeout=10000] Number of milliseconds before timing out the request
 * @returns {object} An axios instance
 */
export function appAxios(timeout = 0) {
  const axiosOptions = {
    timeout: timeout,
    params: {
      t: new Date().getTime()
    }
  };
  if (Vue.prototype.$config) {
    // any other options we can set here?
    axiosOptions.baseURL = Vue.prototype.$apiBaseUrl;
  }

  const instance = axios.create(axiosOptions);

  instance.interceptors.request.use(
    (cfg) => {
      // we could inject auth headers in here if needed.
      return Promise.resolve(cfg);
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  return instance;
}
