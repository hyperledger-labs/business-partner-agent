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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.hyperledger.oa.registry.pojo.RegistryBE;
import org.hyperledger.oa.registry.repository.RegistryRepository;

import io.micronaut.core.util.StringUtils;

@Singleton
public class RegistryManager {

    @Inject
    private RegistryRepository repo;

    public Optional<ObjectId> registerAgent(String raw, String did) {
        Optional<RegistryBE> dbEntry = repo.findByDid(did);
        if (dbEntry.isPresent()) {
            repo.updateData(dbEntry.get().getId(), raw);
            return Optional.of(dbEntry.get().getId());
        }
        return repo.save(new RegistryBE(raw, did));
    }

    public List<Document> searchAgents(String query, int limit) {
        List<Document> result = new ArrayList<>();
        if (StringUtils.isNotEmpty(query)) {
            repo.regexSearch(query, limit).forEach(be -> result.add(Document.parse(be.getData())));
        }
        return result;
    }

    public long getPartnerCount() {
        return repo.getDocumentCount();
    }
}
