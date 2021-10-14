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
package org.hyperledger.bpa.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.model.BPAUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class DBOperations {

    @Inject
    private BPAUserRepository userRepo;

    @Inject
    BCryptPasswordEncoder enc;

    @Value("${bpa.bootstrap.username}")
    private String username;

    @Value("${bpa.bootstrap.password}")
    private String password;

    @Async
    @EventListener
    public void onServiceStartedEvent(StartupEvent startupEvent) {
        log.info("Running startup database operations. {}", startupEvent);

        createDefaultUser();

        log.debug("Done running database operations.");
    }

    private void createDefaultUser() {
        String pwEncoded = enc.encode(password);
        userRepo.findByUsername(username)
                .ifPresentOrElse(u -> {
                    log.info("Bootstrap user already exists, skipping creation");
                    if (!enc.matches(password, u.getPassword())) {
                        log.info("The password of the default user has changed, doing password reset.");
                        userRepo.updatePassword(u.getId(), pwEncoded);
                    }
                },
                        () -> userRepo.save(BPAUser.builder()
                                .username(username)
                                .password(pwEncoded)
                                .roles("ROLE_USER,ROLE_ADMIN")
                                .build()));
    }
}