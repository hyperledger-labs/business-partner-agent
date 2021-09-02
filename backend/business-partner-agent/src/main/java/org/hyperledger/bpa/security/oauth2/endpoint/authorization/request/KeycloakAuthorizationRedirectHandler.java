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
package org.hyperledger.bpa.security.oauth2.endpoint.authorization.request;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.oauth2.endpoint.authorization.request.AuthorizationRequest;
import io.micronaut.security.oauth2.endpoint.authorization.request.DefaultAuthorizationRedirectHandler;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.security.oauth2.client.RequiresKeycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Slf4j
@NoArgsConstructor
@Singleton
@RequiresKeycloak
@Replaces(DefaultAuthorizationRedirectHandler.class)
public class KeycloakAuthorizationRedirectHandler extends DefaultAuthorizationRedirectHandler {

    @Value("${micronaut.security.oauth2.clients.keycloak.vcauthn.pres_req_conf_id}")
    String presentationRequestConfigurationId;

    /**
     * @param authorizationRequest Authentication Request
     * @param response             Authorization Redirect Response
     * @return A parameter map which contains the URL variables used to construct
     *         the authorization redirect url.
     */
    @Override
    protected Map<String, Object> instantiateParameters(AuthorizationRequest authorizationRequest,
            MutableHttpResponse response) {
        Map<String, Object> parameters = super.instantiateParameters(authorizationRequest, response);

        parameters.put("pres_req_conf_id", this.presentationRequestConfigurationId);
        log.info("keycloak vcauthn pres_req_conf_id = " + this.presentationRequestConfigurationId);

        return parameters;
    }

}