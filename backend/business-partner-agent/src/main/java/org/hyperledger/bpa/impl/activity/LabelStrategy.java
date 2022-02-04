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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextHelper;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;

import java.util.Map;
import java.util.Optional;

/**
 * Tries to set a human-readable label on document and credential data. Labels
 * set by a user always take precedence, if none is set the strategy tries to
 * set a default value based on a schema attribute that has been configured in
 * the {@link org.hyperledger.bpa.config.SchemaConfig}.
 */
@Singleton
public class LabelStrategy {

    @Setter
    @Inject
    Converter converter;

    @Inject
    SchemaService schemaService;

    public @Nullable String apply(@NonNull MyDocumentAPI document) {
        if (StringUtils.isBlank(document.getLabel())) {
            Optional<String> attr = findDefaultAttribute(document.getSchemaId());
            if (attr.isPresent()) {
                JsonNode documentData = document.getDocumentData();
                JsonNode value = documentData.findValue(attr.get());
                if (value != null) {
                    String label = value.asText();
                    document.setLabel(label);
                    return label;
                }
            }
            document.setLabel(null);
        }
        return null;
    }

    public @Nullable String apply(@Nullable Credential ariesCredential) {
        if (ariesCredential != null) {
            Optional<String> attr = findDefaultAttribute(ariesCredential.getSchemaId());
            if (attr.isPresent() && ariesCredential.getAttrs() != null) {
                Map<String, String> attrs = ariesCredential.getAttrs();
                return attrs.get(attr.get());
            }
        }
        return null;
    }

    public @Nullable String apply(@Nullable String newLabel, @NonNull AriesCredential ariesCredential) {
        String mergedLabel = null;
        if (StringUtils.isNotBlank(newLabel)) {
            mergedLabel = newLabel;
        } else {
            Optional<String> attr = findDefaultAttribute(ariesCredential.getSchemaId());
            if (attr.isPresent() && ariesCredential.getCredentialData() != null) {
                Map<String, String> attrs = ariesCredential.getCredentialData();
                mergedLabel = attrs.get(attr.get());
            }
        }
        return mergedLabel;
    }

    public String apply(@Nullable BPACredentialExchange.ExchangePayload ldCredential) {
        String result = null;
        if (ldCredential != null && ldCredential.typeIsJsonLd()) {
            V20CredExRecordByFormat.LdProof ldProof = ldCredential.getLdProof();
            String schemaId = LDContextHelper.findSchemaId(ldProof);
            if (StringUtils.isNotEmpty(schemaId)) {
                Optional<String> defaultAttribute = findDefaultAttribute(schemaId);
                if (defaultAttribute.isPresent()) {
                    JsonObject credentialSubject = ldProof.getCredential().getCredentialSubject();
                    JsonElement je = credentialSubject.get(defaultAttribute.get());
                    if (je != null) {
                        result = je.getAsString();
                    }
                }
            }
        }
        return result;
    }

    private Optional<String> findDefaultAttribute(@Nullable String schemaId) {
        if (StringUtils.isNotEmpty(schemaId)) {
            Optional<BPASchema> schema = schemaService.getSchemaFor(schemaId);
            if (schema.isPresent()) {
                String defaultAttributeName = schema.get().getDefaultAttributeName();
                if (StringUtils.isNotEmpty(defaultAttributeName)) {
                    return Optional.of(defaultAttributeName);
                }
            }
        }
        return Optional.empty();
    }
}
