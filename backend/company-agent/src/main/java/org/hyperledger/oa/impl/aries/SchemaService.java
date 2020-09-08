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
package org.hyperledger.oa.impl.aries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.SchemaAPI;
import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.model.Schema;
import org.hyperledger.oa.repository.SchemaRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
public class SchemaService {

    @Inject
    SchemaRepository schemaRepo;

    // CRUD Methods

    public SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label) {
        SchemaAPI result = null;
        String sId = StringUtils.strip(schemaId);
        if (schemaRepo.findBySchemaId(sId).isPresent()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " already exists.");
        }
        if (StringUtils.isNotEmpty(sId)) {
            Schema dbS = Schema
                .builder()
                .label(label)
                .type(CredentialType.fromSchemaId(sId))
                .schemaId(sId)
                .build();
            Schema saved = schemaRepo.save(dbS);
            result = SchemaAPI.from(saved);
        }
        return result;
    }

    public List<SchemaAPI> listSchemas() {
        List<SchemaAPI> result = new ArrayList<>();
        schemaRepo.findAll().forEach(dbS -> result.add(SchemaAPI.from(dbS)));
        return result;
    }

    public void deleteSchema(@NonNull UUID id) {
        schemaRepo.deleteById(id);
    }
}
