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
package org.hyperledger.oa.impl.aries;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.aries.AriesCredential;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.BPASchema;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * Tries to set a human readable label on document and credential data. Labels
 * set by a user always take precedence, if none is set the strategy tries to
 * set a default value based on a schema attribute that has been configured in
 * the {@link org.hyperledger.oa.config.SchemaConfig}.
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

    public @Nullable String apply(@Nullable Credential credential) {
        if (credential != null) {
            Optional<String> attr = findDefaultAttribute(credential.getSchemaId());
            if (attr.isPresent() && credential.getAttrs() != null) {
                Map<String, String> attrs = credential.getAttrs();
                return attrs.get(attr.get());
            }
        }
        return null;
    }

    public @Nullable String apply(@Nullable String newLabel, @NonNull AriesCredential credential) {
        String mergedLabel = null;
        if (StringUtils.isNotBlank(newLabel)) {
            mergedLabel = newLabel;
        } else {
            Optional<String> attr = findDefaultAttribute(credential.getSchemaId());
            if (attr.isPresent() && credential.getCredentialData() != null) {
                Map<String, String> attrs = credential.getCredentialData();
                mergedLabel = attrs.get(attr.get());
            }
        }
        return mergedLabel;
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
