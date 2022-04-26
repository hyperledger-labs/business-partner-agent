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

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

/**
 * Sets HTTP security headers on all frontend related calls.
 */
@Filter({ "/*", "classpath:public", "/js/**", "/css/**", "/fonts/**", "/img/**" })
public class FrontendSecurityHeaderFilter implements HttpServerFilter {

    @Property(name = "bpa.allowed.hosts")
    Optional<List<String>> allowedHosts;

    @Property(name = "bpa.ux.navigation.avatar.agent.default")
    Optional<Boolean> avatarImageDefault;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Flux.from(chain.proceed(request))
                .doOnNext(res -> {
                    // Uncritical headers, should always be set
                    res.getHeaders()
                            .add("Referrer-Policy", "same-origin")
                            .add("X-Content-Type-Options", "nosniff");
                    // These headers might break stuff
                    // TODO maybe only set if strict-security.yml is enabled
                    String frameSources;
                    if (allowedHosts.isEmpty() || CollectionUtils.isEmpty(allowedHosts.get())) {
                        res.getHeaders().add("X-Frame-Options", "deny");
                        frameSources = "frame-ancestors 'none'; ";
                    } else {
                        frameSources = "frame-src " + String.join(" ", allowedHosts.get()) + "; " +
                                "frame-ancestors " + String.join(" ", allowedHosts.get()) + "; ";
                    }
                    String imgSource = "img-src 'self'";
                    if (avatarImageDefault.isPresent() && !avatarImageDefault.get()) {
                        imgSource += " data:";
                    }
                    imgSource += "; ";
                    if (!StringUtils.contains(request.getPath(), "swagger")) { // Skipping swagger
                        res.getHeaders().add("Content-Security-Policy",
                                frameSources +
                                        imgSource +
                                        "default-src 'self'; script-src 'self'; connect-src *; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "font-src 'self' data:");
                    }
                });
    }
}
