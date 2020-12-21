/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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
package org.hyperledger.bpa.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.model.DidDocWeb;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class DidDocWebRepositoryTest {

    @Inject
    DidDocWebRepository repo;

    @Test
    void testCreateAndLoad() {
        String json = "{\"bar\": {}}";
        repo.save(
                DidDocWeb.builder()
                        .didDoc(Map.of("key", json))
                        .build());

        final Iterator<DidDocWeb> it = repo.findAll().iterator();
        assertTrue(it.hasNext());
        DidDocWeb found = it.next();
        assertEquals(json, found.getDidDoc().get("key"));
    }

}
