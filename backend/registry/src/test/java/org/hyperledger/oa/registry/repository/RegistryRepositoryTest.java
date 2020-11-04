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
package org.hyperledger.oa.registry.repository;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.oa.registry.controller.api.Subject;
import org.hyperledger.oa.registry.pojo.RegistryBE;
import org.hyperledger.oa.registry.repository.RegistryRepository;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import io.micronaut.test.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MicronautTest
class RegistryRepositoryTest {

    @Inject
    private RegistryRepository repo;
    @Inject
    private ObjectMapper mapper;

    @Test
    void testFullTextSearch() throws Exception {
        repo.createTextIdx();

        Faker f = Faker.instance();
        String search = "something";
        for (int i = 0; i < 100; i++) {
            String name = f.company().name();
            String address = f.address().streetAddress();
            String did = "did:web:" + UUID.randomUUID().toString();
            repo.save(new RegistryBE(mapper.writeValueAsString(new Subject(did, name, address)), name));

            if (i == 50) {
                search = name;
                log.debug("Search string: {}", search);
            }
        }

        List<RegistryBE> res = repo.regexSearch(search, 0);

        assertTrue(res.size() > 0);
        assertTrue(res.get(0).getData().contains(search));
        res.forEach(r -> {
            log.debug("{}", r);
        });

        String prefix = search.substring(0, 5);
        log.debug("Search string: {}", prefix);
        res = repo.regexSearch(prefix, 0);

        assertTrue(res.size() > 0);
        assertTrue(res.get(0).getData().contains(prefix));
        res.forEach(r -> {
            log.debug("{}", r);
        });
    }
}
