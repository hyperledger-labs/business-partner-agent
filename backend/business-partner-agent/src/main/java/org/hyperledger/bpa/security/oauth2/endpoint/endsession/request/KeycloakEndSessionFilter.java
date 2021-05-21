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
package org.hyperledger.bpa.security.oauth2.endpoint.endsession.request;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.netty.NettyMutableHttpResponse;
import io.micronaut.security.filters.SecurityFilter;
import io.micronaut.security.oauth2.configuration.OauthConfigurationProperties;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

@Requires(property = OauthConfigurationProperties.PREFIX + ".end-session.keycloak.enabled",
        notEquals = StringUtils.FALSE)
@Filter("/oauth/logout")
public class KeycloakEndSessionFilter implements HttpFilter {

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(HttpRequest<?> request, FilterChain chain) {
        if (request.getAttribute(SecurityFilter.REJECTION).isPresent()) {
            return chain.proceed(request);
        }
        return Flowable.fromPublisher(chain.proceed(request)).flatMap(response -> {
            // ok, our client cannot handle redirecting back to keycloak.
            // so change from a 302 found, to a success (ok) with location header.
            if (response.getStatus() == HttpStatus.FOUND) {
                ((NettyMutableHttpResponse) response).getNativeResponse().setStatus(HttpResponseStatus.OK);
            }
            return Flowable.just(response);
        });
    }
}
