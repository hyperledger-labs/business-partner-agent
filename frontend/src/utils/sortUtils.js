/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/*
    Sample for params
    var keyToSort = {
    MYKEY1:"desc",mykey2:"asc", myLastKey:"asc"
  };
*/
export default Object.defineProperty(Array.prototype, "sortByKeys", {
  value: function (keys) {
    keys = keys || {};

    var objectLength = function (object) {
      var size = 0,
        key;
      for (key in object) {
        if (Object.prototype.hasOwnProperty.call(object, key)) size++;
      }
      return size;
    };

    var objectKeyIndex = function (object, ix) {
      var size = 0,
        key;
      for (key in object) {
        if (Object.prototype.hasOwnProperty.call(object, key)) {
          if (size === ix) return key;
          size++;
        }
      }
      return false;
    };

    var isNumberValue = function (v) {
      return !isNaN(Number.parseFloat(v)) && isFinite(v);
    };

    var keySort = function (a, b, d) {
      d = d !== null ? d : 1;
      a = isNumberValue(a) ? a * 1 : String(a).toLowerCase(); // restore numbers
      b = isNumberValue(b) ? b * 1 : String(b).toLowerCase();
      if (a === b) return 0;
      return a > b ? 1 * d : -1 * d;
    };

    var keyLength = objectLength(keys);

    if (!keyLength) return this.sort(keySort);

    for (var k in keys) {
      // asc unless desc or skip
      keys[k] =
        keys[k] === "desc" || keys[k] === -1
          ? -1
          : keys[k] === "skip" || keys[k] === 0
          ? 0
          : 1;
    }

    this.sort(function (a, b) {
      var sorted = 0;
      var index = 0;
      while (sorted === 0 && index < keyLength) {
        var field = objectKeyIndex(keys, index);
        if (field) {
          var dir = keys[field];
          sorted = keySort(a[field], b[field], dir);
          index++;
        }
      }
      return sorted;
    });
    return this;
  },
});
