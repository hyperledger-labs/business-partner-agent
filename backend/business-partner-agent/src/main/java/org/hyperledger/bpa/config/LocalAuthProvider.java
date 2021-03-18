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
package org.hyperledger.bpa.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.*;
import io.reactivex.Maybe;
import org.hyperledger.bpa.model.BPAUser;
import org.hyperledger.bpa.repository.BPAUserRepository;
import org.reactivestreams.Publisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Singleton
public class LocalAuthProvider implements AuthenticationProvider {

    @Inject
    private BPAUserRepository userRepo;

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
