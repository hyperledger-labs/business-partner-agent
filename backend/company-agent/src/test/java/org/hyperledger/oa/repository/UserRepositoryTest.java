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
package org.hyperledger.oa.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import javax.inject.Inject;

import org.hyperledger.oa.model.BPAUser;
import org.junit.jupiter.api.Test;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class UserRepositoryTest {

    @Inject
    private UserRepository userRepo;

    @Test
    void test() {
        userRepo.save(BPAUser.builder()
                .username("testuser123")
                .password("dummy")
                .roles("ROLE_USER,ROLE_ADMIN")
                .build());

        Optional<BPAUser> user = userRepo.findByUsername("testuser123");
        assertTrue(user.isPresent());
    }

}
