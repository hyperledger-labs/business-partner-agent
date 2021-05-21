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
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.oauth2.client.OpenIdProviderMetadata;
import io.micronaut.security.oauth2.configuration.OauthConfigurationProperties;
import io.micronaut.security.oauth2.configuration.endpoints.EndSessionConfiguration;
import io.micronaut.security.oauth2.endpoint.endsession.request.EndSessionEndpoint;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Requires(property = OauthConfigurationProperties.PREFIX + ".end-session.keycloak.enabled",
        notEquals = StringUtils.FALSE)
@Requires(beans = {
        EndSessionConfiguration.class,
        OpenIdProviderMetadata.class
})
@Slf4j
@Singleton
@Named("keycloak")
public class KeycloakEndSessionEndpoint implements EndSessionEndpoint {
    @Inject
    EndSessionConfiguration endSessionConfiguration;
    @Inject
    OpenIdProviderMetadata providerMetadata;

    public KeycloakEndSessionEndpoint() {
    }

    @Override
    public String getUrl(HttpRequest<?> originating, Authentication authentication) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(providerMetadata.getEndSessionEndpoint())) {
            sb.append(providerMetadata.getEndSessionEndpoint());
        }
        if (StringUtils.isNotEmpty(endSessionConfiguration.getRedirectUri())) {
            sb.append("?redirect_uri=");
            sb.append(endSessionConfiguration.getRedirectUri());
        }
        return sb.toString();
    }
}
