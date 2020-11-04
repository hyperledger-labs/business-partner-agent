/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.oa.model.BPAUser;
import org.hyperledger.oa.repository.UserRepository;
import org.reactivestreams.Publisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UserDetails;
import io.reactivex.Maybe;

@Singleton
public class LocalAuthProvider implements AuthenticationProvider {

    @Inject
    private UserRepository userRepo;

    @Inject
    private BCryptPasswordEncoder enc;

    @Override
    public Publisher<AuthenticationResponse> authenticate(
            HttpRequest<?> httpRequest,
            AuthenticationRequest<?, ?> authenticationRequest) {

        Optional<BPAUser> dbUser = userRepo.findByUsername(String.valueOf(authenticationRequest.getIdentity()));

        return Maybe.<AuthenticationResponse>create(emitter -> {
            if (dbUser.isPresent()
                    && enc.matches(String.valueOf(authenticationRequest.getSecret()), dbUser.get().getPassword())) {
                emitter.onSuccess(new UserDetails(
                        dbUser.get().getUsername(),
                        Arrays.asList(dbUser.get().getRoles().split(",")),
                        Map.of("userId", dbUser.get().getId())));
            } else {
                emitter.onError(new AuthenticationException(new AuthenticationFailed()));
            }
        }).toFlowable();
    }

}
