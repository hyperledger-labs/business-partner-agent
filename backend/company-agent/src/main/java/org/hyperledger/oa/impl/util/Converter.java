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
package org.hyperledger.oa.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.api.PartnerAPI.PartnerCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.model.Partner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class Converter {

    public static final TypeReference<Map<String, Object>> MAP_TYPEREF = new TypeReference<>() {
    };

    public static final TypeReference<VerifiablePresentation<VerifiableIndyCredential>> VP_TYPEREF = new TypeReference<>() {
    };

    @Inject
    @Setter
    private ObjectMapper mapper;

    public PartnerAPI toAPIObject(@NonNull Partner p) {
        PartnerAPI result;
        if (p.getVerifiablePresentation() == null) {
            result = new PartnerAPI();
        } else {
            result = toAPIObject(fromMap(p.getVerifiablePresentation(), VP_TYPEREF));
        }
        return result
                .setCreatedAt(Long.valueOf(p.getCreatedAt().toEpochMilli()))
                .setUpdatedAt(Long.valueOf(p.getUpdatedAt().toEpochMilli()))
                .setLastSeen(p.getLastSeen() != null ? Long.valueOf(p.getLastSeen().toEpochMilli()) : null)
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
            partner.getVerifiableCredential().forEach(c -> {
                JsonNode node = mapper.convertValue(c.getCredentialSubject(), JsonNode.class);
                boolean indyCredential = false;
                if (CollectionUtils.isNotEmpty(c.getType())) {
                    indyCredential = c.getType().stream().anyMatch(t -> "IndyCredential".equals(t));
                }
                final PartnerCredential pCred = PartnerCredential
                        .builder()
                        .type(CredentialType.fromType(c.getType()))
                        .issuer(indyCredential ? c.getIndyIssuer() : c.getIssuer())
                        .schemaId(c.getSchemaId())
                        .credentialData(node)
                        .indyCredential(Boolean.valueOf(indyCredential))
                        .build();
                pc.add(pCred);
            });
        }
        return PartnerAPI.builder()
                .verifiablePresentation(partner)
                .credential(pc).build();
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
                .setLabel(apiDoc.getLabel());
        return myDoc;
    }

    public MyDocumentAPI toApiObject(@NonNull MyDocument myDoc) {
        return MyDocumentAPI.builder()
                .id(myDoc.getId())
                .createdDate(Long.valueOf(myDoc.getCreatedAt().toEpochMilli()))
                .updatedDate(Long.valueOf(myDoc.getUpdatedAt().toEpochMilli()))
                .documentData(fromMap(myDoc.getDocument(), JsonNode.class))
                .isPublic(myDoc.getIsPublic())
                .type(myDoc.getType())
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

    public <T> T fromMapString(@NonNull Map<String, String> fromValue, @NotNull Class<T> type) {
        return mapper.convertValue(fromValue, type);
    }

    public Optional<String> writeValueAsString(Object value) {
        try {
            return Optional.of(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.error("Could not serialise to string: {}", e, value);
        }
        return Optional.empty();
    }

}
