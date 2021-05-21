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

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint;

import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Singleton
@Named("keycloak")
public class KeycloakEndSessionEndpoint implements EndSessionEndpoint {

    @Value("${micronaut.security.oauth2.clients.keycloak.openid.end-session.url}")
    String endSessionUrl;

    @Value("${micronaut.security.oauth2.openid.end-session.redirect-uri}")
    String redirectUri;

    @Override
    public String getUrl(HttpRequest<?> originating, Authentication authentication) {
        return endSessionUrl + "?redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }
}
