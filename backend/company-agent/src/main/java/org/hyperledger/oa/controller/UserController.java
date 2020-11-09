/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.controller;

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.session.SessionLoginHandler;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.oa.model.BPAUser;
import org.hyperledger.oa.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/user")
@Hidden
@Tag(name = "User")
@ExecuteOn(TaskExecutors.IO)
public class UserController {

    @Inject
    private UserRepository userRepo;

    @Inject
    private BCryptPasswordEncoder enc;

    @Inject
    private SessionLoginHandler session;

    @View("signin")
    @Get("/signin")
    public HttpResponse<?> loginView() {
        return HttpResponse.ok(CollectionUtils.mapOf("register", Boolean.FALSE));
    }

    @View("signin")
    @Get("/authFailed")
    public Map<String, Object> authFailed() {
        return Collections.singletonMap("errors", Boolean.TRUE);
    }

    @Secured({ "ROLE_ADMIN" })
    @Post(value = "/register", consumes = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<?> registerUser(@Body BPAUser user, HttpRequest<?> request) {
        Optional<BPAUser> existing = userRepo.findByUsername(user.getUsername());
        if (existing.isPresent()) {
            log.warn("User with name: {} already exists.", user.getUsername());
            return HttpResponse
                    .status(HttpStatus.SEE_OTHER, "{\"message\": \"user already exists\"}")
                    .header("Location", "/user/registerFailed");
        }

        BPAUser dbUser = BPAUser
                .builder()
                .username(user.getUsername())
                .password(enc.encode(user.getPassword()))
                .roles("ROLE_USER")
                .build();
        userRepo.save(dbUser);

        return session.loginSuccess(new UserDetails(
                dbUser.getUsername(),
                Arrays.asList(dbUser.getRoles().split(",")),
                Map.of("userId", dbUser.getId())), request);
    }

    @Put(value = "/authenticate", consumes = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
    public HttpResponse<String> authenticateUser(@Body BPAUser user) {
        Optional<BPAUser> dbUser = userRepo.findByUsername(user.getUsername());
        if (dbUser.isPresent()) {
            boolean match = enc.matches(user.getPassword(), dbUser.get().getPassword());
            if (match) {
                return HttpResponse.ok();
            }
        }
        return HttpResponse.unauthorized();
    }
}
