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
package org.hyperledger.bpa.security.oauth2.endpoint.token.response;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.hyperledger.bpa.security.oauth2.client.RequiresKeycloak;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;

@Singleton
@RequiresKeycloak
@Named("keycloak")
public class KeycloakUserDetailsMapper implements OpenIdAuthenticationMapper {

    @Value("${micronaut.security.token.roles-name}")
    String rolesName;

    @Value("${micronaut.security.token.name-key}")
    String nameKey;

    @Override
    @NonNull
    public AuthenticationResponse createAuthenticationResponse(String providerName,
            OpenIdTokenResponse tokenResponse,
            OpenIdClaims openIdClaims,
            @Nullable State state) {
        return AuthenticationResponse.success((String) openIdClaims.get(nameKey),
                (Collection<String>) openIdClaims.get(rolesName),
                openIdClaims.getClaims());
    }
}
