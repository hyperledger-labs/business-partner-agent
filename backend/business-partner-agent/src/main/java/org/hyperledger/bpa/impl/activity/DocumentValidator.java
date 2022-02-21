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
package org.hyperledger.bpa.impl.activity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.repository.MyDocumentRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

/**
 * Does some validation on incoming {@link MyDocumentAPI} objects. Like this we
 * make sure that the system is in a defined state, and we do not have to do
 * validation later.
 */
@Singleton
public class DocumentValidator {

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    @Setter
    SchemaService schemaService;

    @Inject
    Converter conv;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public void validateNew(@NonNull MyDocumentAPI document) {
        verifyOnlyOneOrgProfile(document);
        validateInternal(document);
    }

    public void validateExisting(@NonNull Optional<MyDocument> existing, @NonNull MyDocumentAPI newDocument) {
        if (existing.isEmpty()) {
            throw new WrongApiUsageException(ms.getMessage("api.document.validation.empty"));
        }

        if (!existing.get().getType().equals(newDocument.getType())) {
            throw new WrongApiUsageException(ms.getMessage("api.document.validation.type.changed"));
        }

        validateInternal(newDocument);
    }

    public void validateAttributesAgainstIndySchema(@NonNull JsonNode attributes, @NonNull String schemaId) {
        // validate document data against schema
        BPASchema schema = findSchema(schemaId);
        Set<String> attributeNames = schema.getSchemaAttributeNames();
        // assuming flat structure
        attributes.fieldNames().forEachRemaining(fn -> {
            if (!attributeNames.contains(fn)) {
                throw new WrongApiUsageException(
                        ms.getMessage("api.document.validation.attribute.not.in.schema",
                                Map.of("attr", fn)));
            }
        });
    }

    public void validateAttributesAgainstLDSchema(@NonNull BPASchema bpaSchema, @NonNull Map<String, String> document) {
        SortedSet<String> attributeNames = bpaSchema.getSchemaAttributeNames();
        document.keySet().forEach(k -> {
            if (!"id".equals(k) && !attributeNames.contains(k)) {
                throw new WrongApiUsageException(
                        ms.getMessage("api.document.validation.attribute.not.in.schema",
                                Map.of("attr", k)));
            }
        });
    }

    private void validateInternal(@NonNull MyDocumentAPI document) {
        if (CredentialType.INDY.equals(document.getType())) {
            mustHaveSchemaId(document);
            validateAttributesAgainstIndySchema(document.getDocumentData(), document.getSchemaId());
        } else if (CredentialType.JSON_LD.equals(document.getType())) {
            mustHaveSchemaId(document);
            validateAttributesAgainstLDSchema(findSchema(document.getSchemaId()),
                    conv.toStringMap(document.getDocumentData()));
        }
    }

    private void verifyOnlyOneOrgProfile(@NonNull MyDocumentAPI doc) {
        if (doc.getType().equals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)) {
            docRepo.findAll().forEach(d -> {
                if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(d.getType())) {
                    throw new WrongApiUsageException(ms.getMessage("api.document.validation.profile.already.exists"));
                }
            });
        }
    }

    private @io.micronaut.core.annotation.NonNull BPASchema findSchema(@NotNull String schemaId) {
        return schemaService.getSchemaFor(schemaId)
                .orElseThrow(() -> new WrongApiUsageException(
                        ms.getMessage("api.schema.not.found", Map.of("id", schemaId))));
    }

    private void mustHaveSchemaId(@NotNull MyDocumentAPI document) {
        if (StringUtils.isEmpty(document.getSchemaId())) {
            throw new WrongApiUsageException(ms.getMessage("api.document.validation.schema.id.missing"));
        }
    }
}
