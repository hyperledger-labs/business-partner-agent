/*
 * Copyright (c) 2020 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.PartnerAPI.PartnerCredential;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.model.Partner;

import io.micronaut.core.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.*;

@Slf4j
@Singleton
@NoArgsConstructor
@AllArgsConstructor
// TODO this is more a conversion service
public class Converter {

    public static final TypeReference<Map<String, Object>> MAP_TYPEREF = new TypeReference<>() {
    };

    public static final TypeReference<VerifiablePresentation<VerifiableIndyCredential>> VP_TYPEREF = new TypeReference<>() {
    };

    @Value("${bpa.did.prefix}")
    private String ledgerPrefix;

    @Inject
    @Setter
    ObjectMapper mapper;

    @Inject
    SchemaService schemaService;

    public PartnerAPI toAPIObject(@NonNull Partner p) {
        PartnerAPI result;
        if (p.getVerifiablePresentation() == null) {
            result = new PartnerAPI();
        } else {
            result = toAPIObject(fromMap(p.getVerifiablePresentation(), VP_TYPEREF));
        }
        return result
                .setCreatedAt(p.getCreatedAt().toEpochMilli())
                .setUpdatedAt(p.getUpdatedAt().toEpochMilli())
                .setLastSeen(p.getLastSeen() != null ? p.getLastSeen().toEpochMilli() : null)
                .setId(p.getId().toString())
                .setValid(p.getValid())
                .setAriesSupport(p.getAriesSupport())
                .setState(p.getState())
                .setAlias(p.getAlias())
                .setDid(p.getDid())
                .setIncoming(p.getIncoming() != null ? p.getIncoming() : Boolean.FALSE);
    }

    public PartnerAPI toAPIObject(@NonNull VerifiablePresentation<VerifiableIndyCredential> partner) {
        List<PartnerCredential> pc = new ArrayList<>();
        if (partner.getVerifiableCredential() != null) {
            for (VerifiableIndyCredential c : partner.getVerifiableCredential()) {
                JsonNode node = mapper.convertValue(c.getCredentialSubject(), JsonNode.class);

                boolean indyCredential = false;
                if (CollectionUtils.isNotEmpty(c.getType())) {
                    indyCredential = c.getType().stream().anyMatch("IndyCredential"::equals);
                }

                CredentialType type = CredentialType.fromType(c.getType());

                String schemaId = null;
                if (indyCredential) {
                    schemaId = c.getSchemaId();
                } else if (CredentialType.SCHEMA_BASED.equals(type)) {
                    schemaId = getSchemaIdFromContext(c);
                }

                final PartnerCredential pCred = PartnerCredential
                        .builder()
                        .type(type)
                        .typeLabel(resolveTypeLabel(type, schemaId))
                        .issuer(indyCredential ? c.getIndyIssuer() : c.getIssuer())
                        .schemaId(schemaId)
                        .credentialData(node)
                        .indyCredential(indyCredential)
                        .build();
                pc.add(pCred);
            }
        }
        return PartnerAPI.builder()
                .verifiablePresentation(partner)
                .credential(pc)
                .build();
    }

    public Partner toModelObject(String did, PartnerAPI api) {
        return Partner
                .builder()
                .did(did)
                .valid(api.getValid())
                .verifiablePresentation(
                        api.getVerifiablePresentation() != null ? toMap(api.getVerifiablePresentation()) : null)
                .build();
    }

    /**
     * Converts the given credential to related model object.
     *
     * @param document credential payload
     * @return myCredential model object
     */
    public MyDocument toModelObject(@NonNull MyDocumentAPI document) {
        return updateMyCredential(document, new MyDocument());
    }

    /**
     * Updates the given myCredential with the data from the given credential
     *
     * @param apiDoc credential payload
     * @param myDoc  model object
     * @return myCredential model object, updated
     */
    public MyDocument updateMyCredential(@NonNull MyDocumentAPI apiDoc, @NonNull MyDocument myDoc) {
        Map<String, Object> data = toMap(apiDoc.getDocumentData());
        myDoc
                .setDocument(data)
                .setIsPublic(apiDoc.getIsPublic())
                .setType(apiDoc.getType())
                .setSchemaId(apiDoc.getSchemaId())
                .setLabel(apiDoc.getLabel());
        return myDoc;
    }

    public MyDocumentAPI toApiObject(@NonNull MyDocument myDoc) {
        return MyDocumentAPI.builder()
                .id(myDoc.getId())
                .createdDate(myDoc.getCreatedAt().toEpochMilli())
                .updatedDate(myDoc.getUpdatedAt().toEpochMilli())
                .documentData(fromMap(myDoc.getDocument(), JsonNode.class))
                .isPublic(myDoc.getIsPublic())
                .type(myDoc.getType())
                .typeLabel(resolveTypeLabel(myDoc.getType(), myDoc.getSchemaId()))
                .schemaId(myDoc.getSchemaId())
                .label(myDoc.getLabel())
                .build();
    }

    public Map<String, Object> toMap(@NonNull Object fromValue) {
        return mapper.convertValue(fromValue, MAP_TYPEREF);
    }

    public <T> T fromMap(@NonNull Map<String, Object> fromValue, @NotNull Class<T> type) {
        return mapper.convertValue(fromValue, type);
    }

    public <T> T fromMap(@NonNull Map<String, Object> fromValue, @NotNull TypeReference<T> type) {
        return mapper.convertValue(fromValue, type);
    }

    public Optional<String> writeValueAsString(Object value) {
        try {
            return Optional.of(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.error("Could not serialise to string: {}", value, e);
        }
        return Optional.empty();
    }

    private String resolveTypeLabel(@NonNull CredentialType type, @Nullable String schemaId) {
        String result = null;
        if (CredentialType.SCHEMA_BASED.equals(type)
                && StringUtils.isNotEmpty(schemaId)) {
            result = schemaService.getSchemaLabel(schemaId);
        } else if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(type)) {
            result = ApiConstants.ORG_PROFILE_NAME;
        }
        return result;
    }

    private String getSchemaIdFromContext(VerifiableIndyCredential c) {
        String schemaId = null;
        JsonArray ja = GsonConfig.defaultConfig().toJsonTree(c.getContext()).getAsJsonArray();
        for (JsonElement je : ja) {
            if (je.isJsonObject()) {
                JsonObject jo = je.getAsJsonObject();
                JsonElement ctx = jo.getAsJsonObject("@context");
                if (ctx != null) {
                    JsonElement e = ctx.getAsJsonObject().get("sc");
                    if (e != null) {
                        schemaId = e.getAsString().replace(ledgerPrefix, "");
                        break;
                    }
                }
            }
        }
        return schemaId;
    }
}
