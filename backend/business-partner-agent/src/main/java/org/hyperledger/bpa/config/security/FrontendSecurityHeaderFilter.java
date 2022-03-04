/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.config.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Sets security related HTTP headers on all frontend related calls.
 */
@Filter({ "/*", "classpath:public", "/js/**", "/css/**", "/fonts/**", "/img/**" })
public class FrontendSecurityHeaderFilter implements HttpServerFilter {
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Flux.from(chain.proceed(request))
                .doOnNext(res -> res.getHeaders()
                        .add("Referrer-Policy", "same-origin")
                        .add("X-Content-Type-Options", "nosniff")
                        .add("X-Frame-Options", "deny")
                        .add("Content-Security-Policy", "frame-ancestors 'none'; " +
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self'; font-src 'self' data:"));
    }
}
