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
package org.hyperledger.bpa.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Controller("/secured")
@Hidden
@Secured(SecurityRule.IS_AUTHENTICATED)
public class SecuredController {

    @Get("/admin")
    @Secured({ "ROLE_ADMIN" })
    public Map admin(@Nullable Authentication authentication) {
        return getDetails("ROLE_ADMIN", authentication);
    }

    @Get("/user")
    @Secured({ "ROLE_USER" })
    public Map view(@Nullable Authentication authentication) {
        return getDetails("ROLE_USER", authentication);
    }

    @Get("/anonymous")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public Map anonymous(@Nullable Authentication authentication) {
        return getDetails("<none>", authentication);
    }

    private Map getDetails(String role, @Nullable Authentication authentication) {
        if (authentication == null) {
            return Collections.singletonMap("isLoggedIn", false);
        }
        return CollectionUtils.mapOf("isLoggedIn", true,
                "role", role,
                "username", authentication.getName(),
                "roles", authentication.getAttributes().get("roles"));
    }
}
