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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.hyperledger.oa.registry.pojo.RegistryBE;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.TextSearchOptions;
import com.mongodb.client.model.Updates;

import io.micronaut.context.annotation.Value;
import lombok.NonNull;

@Singleton
public class RegistryRepository {

    private MongoCollection<RegistryBE> registryDB;

    public RegistryRepository(MongoClient mc, @Value("${mongodb.database}") String database) {
        super();
        MongoDatabase md = mc.getDatabase(database);
        registryDB = md.getCollection(RegistryBE.COLLECTION_NAME, RegistryBE.class);
    }

    public Optional<ObjectId> save(@NonNull RegistryBE user) {
        BsonValue result = registryDB.insertOne(user).getInsertedId();
        if (result != null) {
            return Optional.of(result.asObjectId().getValue());
        }
        return Optional.empty();
    }

    public void updateData(@NonNull ObjectId id, String data) {
        registryDB.updateOne(Filters.eq("_id", id), Updates.set("data", data));
    }

    public Optional<RegistryBE> findByDid(@NonNull String did) {
        return Optional.ofNullable(registryDB.find(Filters.eq("did", did)).first());
    }

    public List<RegistryBE> textSearch(@NonNull String query) {
        List<RegistryBE> result = new ArrayList<>();
        registryDB.find(Filters.text(query,
                new TextSearchOptions()
                        .caseSensitive(Boolean.FALSE)))
                .forEach(t -> result.add(t));
        return result;
    }

    public List<RegistryBE> regexSearch(@NonNull String query, int limit) {
        List<RegistryBE> result = new ArrayList<>();
        Pattern p = Pattern.compile(".*" + Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        registryDB.find(Filters.regex("data", p))
                .limit(limit)
                .forEach(t -> result.add(t));
        return result;
    }

    public long getDocumentCount() {
        return registryDB.estimatedDocumentCount();
    }

    public void createTextIdx() {
        registryDB.createIndex(Indexes.text("data"), new IndexOptions()
                .background(true)
                .name("data_text_idx"));
    }

    public void createDidIdx() {
        registryDB.createIndex(Indexes.ascending("did"), new IndexOptions()
                .background(true)
                .name("unique_did_idx")
                .unique(true));
    }
}
