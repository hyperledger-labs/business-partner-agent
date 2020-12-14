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
package org.hyperledger.oa.repository.migration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.SchemaAPI;
import org.hyperledger.oa.impl.aries.SchemaService;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.repository.MyDocumentRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 * Temp fix class, delete once all services have been upgraded to
 * https://github.com/hyperledger-labs/business-partner-agent/issues/238
 */
@Slf4j
@Singleton
public class V1_9_1__RepairSchemaIds {

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    @Setter
    Converter conv;

    @Inject
    SchemaService schema;

    public void setSchemaIdsWhereNull() {
        log.info("Running database migrations.");
        docRepo.findAll().forEach(d -> {
            if (CredentialType.SCHEMA_BASED.equals(d.getType())
                    && StringUtils.isEmpty(d.getSchemaId())) {
                JsonNode node = conv.fromMap(d.getDocument(), JsonNode.class);
                if (hasProperty(node, "iban")) {
                    String schemaId = findSchemaId("Bank");
                    docRepo.updateSchemaId(d.getId(), schemaId);
                } else if (hasProperty(node, "did")) {
                    String schemaId = findSchemaId("Commercial");
                    docRepo.updateSchemaId(d.getId(), schemaId);
                }
            }
        });
    }

    private boolean hasProperty(@NonNull JsonNode node, @NonNull String property) {
        List<JsonNode> values = node.findValues(property);
        return values.size() > 0;
    }

    private String findSchemaId(@NonNull String label) {
        Optional<SchemaAPI> schemaId = schema
                .listSchemas().stream().filter(s -> s.getLabel().startsWith(label)).findFirst();
        return schemaId.map(SchemaAPI::getSchemaId).orElse(null);
    }
}
