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
package org.hyperledger.oa.registry.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.hyperledger.oa.registry.controller.api.Subject;
import org.hyperledger.oa.registry.impl.RegistryManager;
import org.hyperledger.oa.registry.pojo.RegistryBE;
import org.hyperledger.oa.registry.repository.RegistryRepository;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class RegistryManagerTest {

    @Inject
    private RegistryManager mgmt;

    @Inject
    private RegistryRepository repo;

    @Inject
    private ObjectMapper mapper;

    @Test
    void testRegisterAgent() throws Exception {
        String did1 = "did:web:1";
        Subject s1 = new Subject(did1, "comp1", "street1");
        String s1s = mapper.writeValueAsString(s1);

        mgmt.registerAgent(s1s, did1);
        mgmt.registerAgent(s1s, did1);

        List<RegistryBE> search = repo.regexSearch("comp1", 0);
        assertEquals(1, search.size());
    }

}
