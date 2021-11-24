/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
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

    var objLength = function (obj) {
      var size = 0,
        key;
      for (key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) size++;
      }
      return size;
    };

    var objKeyIndex = function (obj, ix) {
      var size = 0,
        key;
      for (key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          if (size === ix) return key;
          size++;
        }
      }
      return false;
    };

    var isNumberValue = function (v) {
      return !isNaN(parseFloat(v)) && isFinite(v);
    };

    var keySort = function (a, b, d) {
      d = d !== null ? d : 1;
      a = isNumberValue(a) ? a * 1 : String(a).toLowerCase(); // restore numbers
      b = isNumberValue(b) ? b * 1 : String(b).toLowerCase();
      if (a === b) return 0;
      return a > b ? 1 * d : -1 * d;
    };

    var keyLength = objLength(keys);

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
      var i = 0;
      while (sorted === 0 && i < keyLength) {
        var field = objKeyIndex(keys, i);
        if (field) {
          var dir = keys[field];
          sorted = keySort(a[field], b[field], dir);
          i++;
        }
      }
      return sorted;
    });
    return this;
  },
});
