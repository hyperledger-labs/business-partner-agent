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
package org.hyperledger.bpa.impl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.PartnerAPI.PartnerCredential;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.credential.CredentialInfoResolver;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextHelper;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateConversion;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@NoArgsConstructor
@AllArgsConstructor
// TODO this is more a conversion service
public class Converter {

    public static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP = new TypeReference<>() {
    };

    public static final TypeReference<Map<String, String>> STRING_STRING_MAP = new TypeReference<>() {
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

    @Inject
    ProofTemplateConversion templateConversion;

    @Inject
    CredentialInfoResolver credentialInfoResolver;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    public PartnerAPI toAPIObject(@NonNull Partner p) {
        PartnerAPI result = PartnerAPI.from(p);
        if (p.getVerifiablePresentation() != null) {
            result = toAPIObject(p.getVerifiablePresentation());
            PartnerAPI.copyFrom(result, p);
        }
        return result;
    }

    public PartnerAPI toAPIObject(@NonNull VerifiablePresentation<VerifiableIndyCredential> partner) {
        List<PartnerCredential> pc = new ArrayList<>();
        if (partner.getVerifiableCredential() != null) {
            for (VerifiableIndyCredential c : partner.getVerifiableCredential()) {
                JsonNode node;
                try {
                    node = mapper.readTree(c.getCredentialSubject().toString());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                boolean verifiedCredential = false;
                if (CollectionUtils.isNotEmpty(c.getType())) {
                    verifiedCredential = c.getType().stream().anyMatch("IndyCredential"::equals);
                }

                CredentialType type = CredentialType.fromCredential(c);

                String schemaId = null;
                if (verifiedCredential) { // indy credential signed by 3rd party
                    schemaId = c.getSchemaId();
                } else if (CredentialType.INDY.equals(type)) { // plain document, indy based and self-signed
                    schemaId = getSchemaIdFromContext(c);
                } else if (CredentialType.JSON_LD.equals(type)) {
                    List<Object> ctx = new ArrayList<>(c.getContext());
                    ctx.removeAll(CredentialType.JSON_LD.getContext());
                    if (CollectionUtils.isNotEmpty(ctx)) {
                        schemaId = String.valueOf(ctx.get(0));
                    }
                }

                String typeLabel = resolveTypeLabel(type, schemaId, c);

                final PartnerCredential pCred = PartnerCredential
                        .builder()
                        .type(type)
                        .typeLabel(typeLabel)
                        .issuer(verifiedCredential ? c.getIndyIssuer() : c.getIssuer())
                        .schemaId(schemaId)
                        .credentialData(node)
                        .indyCredential(verifiedCredential)
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
                        api.getVerifiablePresentation() != null ? api.getVerifiablePresentation() : null)
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
        schemaService.getSchemaFor(apiDoc.getSchemaId()).ifPresentOrElse(myDoc::setSchema,
                EntityNotFoundException::new);
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
                .createdAt(myDoc.getCreatedAt().toEpochMilli())
                .updatedAt(myDoc.getUpdatedAt().toEpochMilli())
                .documentData(myDoc.getDocument() != null ? fromMap(myDoc.getDocument(), JsonNode.class) : null)
                .isPublic(myDoc.getIsPublic())
                .type(myDoc.getType())
                .typeLabel(resolveTypeLabel(myDoc.getType(), myDoc.getSchemaId(), null))
                .schemaId(myDoc.getSchemaId())
                .label(myDoc.getLabel() != null ? myDoc.getLabel() : "")
                .build();
    }

    public JsonNode mapToNode(@NonNull Map<String, String> from) {
        return mapper.valueToTree(from);
    }

    public Map<String, Object> toMap(@NonNull Object fromValue) {
        return mapper.convertValue(fromValue, STRING_OBJECT_MAP);
    }

    public Map<String, String> toStringMap(@NonNull Object fromValue) {
        return mapper.convertValue(fromValue, STRING_STRING_MAP);
    }

    public <T> T fromMap(@NonNull Map<String, Object> fromValue, @NonNull Class<T> type) {
        return mapper.convertValue(fromValue, type);
    }

    public <T> T fromMap(@NonNull Map<String, Object> fromValue, @NonNull TypeReference<T> type) {
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

    public AriesProofExchange toAPIObject(@NonNull PartnerProof p) {
        AriesProofExchange proof = AriesProofExchange.from(p);

        proof.setTypeLabel(resolveTypeLabel(p));

        JsonNode proofData = null;
        try {
            if (p.getProof() != null) {
                if (p.typeIsIndy()) {
                    Map<String, PresentationExchangeRecord.RevealedAttributeGroup> groups = p.getProof().getIndy();
                    Map<String, AriesProofExchange.RevealedAttributeGroup> collect = groups.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> AriesProofExchange.RevealedAttributeGroup
                                    .builder()
                                    .revealedAttributes(e.getValue().getRevealedAttributes())
                                    .identifier(credentialInfoResolver.populateIdentifier(e.getValue().getIdentifier()))
                                    .build()));
                    proofData = mapper.convertValue(collect, JsonNode.class);
                } else if (p.typeIsJsonLd()) {
                    VerifiablePresentation<VerifiableCredential> vp = p.getProof().getLdProof();
                    Map<String, AriesProofExchange.RevealedAttributeGroup> collect = vp.getVerifiableCredential()
                            .stream().map(vc -> {
                                String type = LDContextHelper.findSchemaId(vc) + "_" + UUID.randomUUID();
                                AriesProofExchange.RevealedAttributeGroup ag = AriesProofExchange.RevealedAttributeGroup
                                        .builder()
                                        .revealedAttributes(vc.subjectToFlatMap())
                                        .identifier(credentialInfoResolver
                                                .populateIdentifier(PresentationExchangeRecord.Identifier
                                                        .builder()
                                                        .schemaId(type)
                                                        .build()))
                                        .build();
                                return new AbstractMap.SimpleEntry<>(type, ag);
                            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    // TODO duplicate key
                    proofData = mapper.convertValue(collect, JsonNode.class);
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Not an attribute group");
            proofData = p.getProof() != null ? mapper.convertValue(p.getProof(), JsonNode.class) : null;
        }

        if (p.typeIsJsonLd() && p.getProofRequest() != null && p.getProofRequest().getLdProof() != null) {
            V2DIFProofRequest proofRequest = p.getProofRequest().getLdProof();
            Map<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> requestedAttributes = proofRequest
                    .getPresentationDefinition().getInputDescriptors().stream().map(id -> {
                        PresentProofRequest.ProofRequest.ProofRequestedAttributes ra = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                                .builder()
                                .names(id.getConstraints().getFields().stream()
                                        .map(f -> f.getPath().stream()
                                                .map(path -> path.replace("$.credentialSubject.", ""))
                                                .collect(Collectors.toList()))
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toList()))
                                .build();
                        String type = LDContextHelper.findSchemaId(id.getSchema().stream()
                                .map(V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri::getUri)
                                .collect(Collectors.toList()));
                        return new AbstractMap.SimpleEntry<>(type, ra);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            proof.setProofRequest(PresentProofRequest.ProofRequest
                    .builder()
                    .requestedAttributes(requestedAttributes)
                    .build());
        }

        proof.setProofData(proofData);
        return proof;
    }

    public Map<String, PresentationExchangeRecord.RevealedAttributeGroup> revealedAttrsToGroup(
            Map<String, PresentationExchangeRecord.RevealedAttribute> attrs,
            List<PresentationExchangeRecord.Identifier> identifier) {
        Map<String, PresentationExchangeRecord.RevealedAttributeGroup> attrToGroup = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(attrs)) {
            attrs.forEach((k, v) -> attrToGroup.put(k, PresentationExchangeRecord.RevealedAttributeGroup
                    .builder()
                    .revealedAttributes(Map.of(k, v.getRaw()))
                    .identifier(CollectionUtils.isNotEmpty(identifier) ? identifier.get(v.getSubProofIndex()) : null)
                    .build()));
        }
        return attrToGroup;
    }

    /**
     * In V1 proof proposal there is no way to name the proof request so aca-py
     * always falls back to 'proof-request' for the name. In most cases this is only
     * relevant in bpa to bpa communication, so we know how the proposal looks like,
     * and we can fall back to the label of the credential definition.
     *
     * @param p {@link PartnerProof}
     * @return name, credential definition tag, or default label
     */
    private String resolveTypeLabel(@NonNull PartnerProof p) {
        String defaultLabel = msg.getMessage("api.proof.exchange.default.name");
        PresentProofRequest.ProofRequest indy = Objects
                .requireNonNullElseGet(p.getProofRequest(),
                        ExchangePayload<PresentProofRequest.ProofRequest, V2DIFProofRequest>::new)
                .getIndy();
        if (indy != null && !"proof-request".equals(indy.getName())) {
            return indy.getName();
        }
        if (indy != null
                && indy.getRequestedAttributes() != null
                && indy.getRequestedAttributes().size() == 1) {
            return indy.getRequestedAttributes().entrySet().stream().findFirst().map(attr -> {
                if (attr.getValue().getRestrictions() != null && attr.getValue().getRestrictions().size() == 1) {
                    JsonObject jo = attr.getValue().getRestrictions().get(0);
                    String credDefId = jo.get("cred_def_id") != null ? jo.get("cred_def_id").getAsString() : null;
                    if (credDefId != null) {
                        return StringUtils.replace(AriesStringUtil.credDefIdGetTag(credDefId), "-", " ");
                    }
                }
                return defaultLabel;
            }).orElse(defaultLabel);
        }
        return defaultLabel;
    }

    private String resolveTypeLabel(@NonNull CredentialType type, @Nullable String schemaId,
            @Nullable VerifiableIndyCredential c) {
        String result = null;
        if (CredentialType.INDY.equals(type) && StringUtils.isNotEmpty(schemaId)) {
            result = schemaService.getSchemaLabel(schemaId);
        } else if (CredentialType.JSON_LD.equals(type)) {
            if (StringUtils.isNotEmpty(schemaId)) {
                result = schemaService.getSchemaLabel(schemaId);
            }
            if (result == null && c != null) {
                List<String> types = new ArrayList<>(c.getType());
                types.removeAll(CredentialType.JSON_LD.getType());
                if (CollectionUtils.isNotEmpty(types)) {
                    result = types.get(0);
                }
            }
        } else if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(type)) {
            result = msg.getMessage("api.org.profile.name");
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
