import axios from 'axios';
import Vue from 'vue';

/**
 * @function appAxios
 * Returns an Axios instance with pre-configuration
 * @param {integer} [timeout=10000] Number of milliseconds before timing out the request
 * @returns {object} An axios instance
 */
export function appAxios(timeout = 30000) {
  const axiosOptions = { timeout: timeout };
  if (Vue.prototype.$config) {
    // any other options we can set here?
    axiosOptions.baseURL = Vue.prototype.$apiBaseUrl;
  }

  const instance = axios.create(axiosOptions);

  instance.interceptors.request.use(cfg => {
    // we could inject auth headers in here if needed.
    return Promise.resolve(cfg);
  }, error => {
    return Promise.reject(error);
  });

  return instance;
}
