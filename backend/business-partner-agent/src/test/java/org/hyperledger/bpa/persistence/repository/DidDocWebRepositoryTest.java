/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.persistence.model.DidDocWeb;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class DidDocWebRepositoryTest {

    @Inject
    DidDocWebRepository repo;

    @Test
    void testCreateAndLoad() {
        DIDDocument didDoc = DIDDocument.builder()
                .controller("contr")
                .build();
        repo.save(
                DidDocWeb.builder()
                        .didDoc(didDoc)
                        .build());

        final Iterator<DidDocWeb> it = repo.findAll().iterator();
        assertTrue(it.hasNext());
        DidDocWeb found = it.next();
        assertNotNull(found.getDidDoc());
        assertEquals("contr", found.getDidDoc().getController());
    }

}
