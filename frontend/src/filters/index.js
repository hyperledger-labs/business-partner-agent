import Vue from 'vue';
import moment from 'moment';

/**
 * @function formatDate
 * Converts a date to an 'YYYY-MM-DD' formatted string
 * @param {Date, String, Long} value A date object
 * @returns {String} A string representation of `value`
 */
export function formatDate(value) {
  if (value) {
    return moment(value).format('YYYY-MM-DD');
  }
}

/**
 * @function formatDateLong
 * Converts a date to an 'YYYY-MM-DD HH:mm' formatted string
 * @param {Date, String, Long} value A date object
 * @returns {String} A string representation of `value`
 */
export function formatDateLong(value) {
  if (value) {
    return moment(value).format('YYYY-MM-DD HH:mm');
  }
}

export function credentialTag(credDefId) {
  if (!credDefId) return "";
  let pos = credDefId.lastIndexOf(":");
  return credDefId.substring(pos + 1);
}

export function capitalize(string) {
  return string && string !== ""
    ? string.replace(/\w\S*/g, (w) => w.replace(/^\w/, (c) => c.toUpperCase()))
    : "";
}

// Define Global Vue Filters
//
// {{ expression | filter }}
//
Vue.filter('formatDate', formatDate);
Vue.filter('formatDateLong', formatDateLong);
Vue.filter("credentialTag", credentialTag);
Vue.filter("capitalize", capitalize);
