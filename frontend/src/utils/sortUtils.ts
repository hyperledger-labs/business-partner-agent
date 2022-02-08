/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/* eslint-disable unicorn/consistent-function-scoping */
/*
    Sample for params
    var keyToSort = {
    MYKEY1:"desc",mykey2:"asc", myLastKey:"asc"
  };
*/
export default Object.defineProperty(Array.prototype, "sortByKeys", {
  value: function (keys) {
    keys = keys || {};

    const objectLength = function (object) {
      let size = 0,
        key;
      for (key in object) {
        if (Object.prototype.hasOwnProperty.call(object, key)) size++;
      }
      return size;
    };

    const objectKeyIndex = function (object, ix) {
      let size = 0,
        key;
      for (key in object) {
        if (Object.prototype.hasOwnProperty.call(object, key)) {
          if (size === ix) return key;
          size++;
        }
      }
      return false;
    };

    const isNumberValue = function (v) {
      return !Number.isNaN(Number.parseFloat(v)) && Number.isFinite(v);
    };

    const keySort = function (a, b, d) {
      d = d !== null ? d : 1;
      a = isNumberValue(a) ? a * 1 : String(a).toLowerCase(); // restore numbers
      b = isNumberValue(b) ? b * 1 : String(b).toLowerCase();
      if (a === b) return 0;
      return a > b ? 1 * d : -1 * d;
    };

    const keyLength = objectLength(keys);

    if (!keyLength) return this.sort(keySort);

    for (const k in keys) {
      // asc unless desc or skip
      if (keys[k] === "desc" || keys[k] === -1) {
        keys[k] = -1;
      } else {
        keys[k] = keys[k] === "skip" || keys[k] === 0 ? 0 : 1;
      }
    }

    this.sort(function (a, b) {
      let sorted = 0;
      let index = 0;
      while (sorted === 0 && index < keyLength) {
        const field = objectKeyIndex(keys, index);
        if (field) {
          const direction = keys[field];
          sorted = keySort(a[field], b[field], direction);
          index++;
        }
      }
      return sorted;
    });
    return this;
  },
});
