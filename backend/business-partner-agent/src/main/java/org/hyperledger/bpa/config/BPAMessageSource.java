/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.config;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.server.util.locale.CompositeHttpLocaleResolver;
import jakarta.inject.Singleton;
import lombok.NonNull;

import java.util.Locale;
import java.util.Map;

@Factory
public class BPAMessageSource {

    @Singleton
    public DefaultMessageSource messageSource(CompositeHttpLocaleResolver resolver) {
        return new DefaultMessageSource(resolver);
    }

    public static class DefaultMessageSource {

        private final CompositeHttpLocaleResolver resolver;

        private final ResourceBundleMessageSource ms;

        public DefaultMessageSource(CompositeHttpLocaleResolver resolver) {
            this.resolver = resolver;
            this.ms = new ResourceBundleMessageSource("org.hyperledger.bpa.i18n.messages");
        }

        public String getMessage(@NonNull String key) {
            return ms.getMessage(key, MessageSource.MessageContext.of(resolveLocale()), "");
        }

        public String getMessage(@NonNull String key, Map<String, Object> variables) {
            return ms.getMessage(key, MessageSource.MessageContext.of(resolveLocale(), variables), "");
        }

        private Locale resolveLocale() {
            return ServerRequestContext.currentRequest().map(resolver::resolveOrDefault).orElse(Locale.ENGLISH);
        }
    }
}
