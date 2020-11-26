/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

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
package org.hyperledger.oa.impl.activity;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.impl.aries.SchemaService;
import org.hyperledger.oa.model.BPASchema;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.repository.MyDocumentRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

/**
 * Does some validation on incoming {@link MyDocumentAPI} objects. Like this we
 * make sure that the system is in a defined state and we do not have to do
 * validation later.
 */
@Singleton
public class DocumentValidator {

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    @Setter
    Optional<SchemaService> schemaService;

    public void validateNew(MyDocumentAPI document) {
        verifyOnlyOneOrgProfile(document);
        validateInternal(document);
    }

    public void validateExisting(Optional<MyDocument> existing, MyDocumentAPI newDocument) {
        if (existing.isEmpty()) {
            throw new WrongApiUsageException("Document does not exist in database");
        }

        if (!existing.get().getType().equals(newDocument.getType())) {
            throw new WrongApiUsageException("Document type can not be changed after creation");
        }

        validateInternal(newDocument);
    }

    private void validateInternal(MyDocumentAPI document) {
        if (CredentialType.INDY_CREDENTIAL.equals(document.getType())) {
            if (StringUtils.isEmpty(document.getSchemaId())) {
                throw new WrongApiUsageException("A document of type indy_credential must have a schema id set.");
            }
            // validate document data against schema
            if (schemaService.isPresent()) {
                Optional<BPASchema> schema = schemaService.get().getSchemaFor(document.getSchemaId());
                if (schema.isPresent()) {
                    Set<String> attributeNames = schema.get().getSchemaAttributeNames();
                    // assuming flat structure
                    document.getDocumentData().fieldNames().forEachRemaining(fn -> {
                        if (!attributeNames.contains(fn)) {
                            throw new WrongApiUsageException("Attribute: " + fn + " is not a part of the schema");
                        }
                    });
                } else {
                    throw new WrongApiUsageException("Schema with id: " + document.getSchemaId() + " does not exist.");
                }
            }
        }
    }

    private void verifyOnlyOneOrgProfile(MyDocumentAPI doc) {
        if (doc.getType().equals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)) {
            docRepo.findAll().forEach(d -> {
                if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(d.getType())) {
                    throw new WrongApiUsageException("Organizational profile already exists, use update instead");
                }
            });
        }
    }
}
