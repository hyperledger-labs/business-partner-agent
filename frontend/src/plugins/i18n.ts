/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { createI18n } from "vue-i18n";

function loadLocaleMessages() {
  const locales = require.context("../locales", true, /[\s\w,-]+\.json$/i);
  const messages: any = {};
  for (const key of locales.keys()) {
    const matched = key.match(/([\w-]+)\./i);
    if (matched && matched.length > 1) {
      const locale = matched[1];
      messages[locale] = locales(key);
    }
  }
  return messages;
}

const i18n = createI18n({
  allowComposition: true,
  legacy: false,
  globalInjection: true,
  locale: process.env.VUE_APP_I18N_LOCALE || "en",
  fallbackLocale: process.env.VUE_APP_I18N_FALLBACK_LOCALE || "en",
  messages: loadLocaleMessages(),
});

export default i18n;
