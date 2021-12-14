/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential.VerifiableIndyCredentialBuilder;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.config.TimeUtil;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.DidDocWeb;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.DidDocWebRepository;
import org.hyperledger.bpa.repository.HolderCredExRepository;
import org.hyperledger.bpa.repository.MyDocumentRepository;

import java.util.*;

@Singleton
public class VPManager {

    @Inject
    Identity id;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    DidDocWebRepository didRepo;

    @Inject
    CryptoManager crypto;

    @Inject
    @Setter
    SchemaService schemaService;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    Converter converter;

    public void recreateVerifiablePresentation() {
        List<VerifiableIndyCredential> vcs = new ArrayList<>();

        String myDid = id.getMyDid();

        docRepo.findByIsPublicTrue().forEach(doc -> vcs.add(buildFromDocument(doc, myDid)));

        holderCredExRepo.findByRoleAndIsPublicTrue(CredentialExchangeRole.HOLDER)
                .forEach(cred -> vcs.add(buildFromCredential(cred)));

        // only split up into own method, because of a weird issue that the second
        // thread does
        // not see the newly created document otherwise.
        signVP(vcs);
    }

    @Async
    public void signVP(List<VerifiableIndyCredential> vcs) {
        final VerifiablePresentationBuilder<VerifiableIndyCredential> vpBuilder = VerifiablePresentation.builder();
        if (vcs.size() > 0) {
            vpBuilder.verifiableCredential(vcs);
        } else {
            vpBuilder.verifiableCredential(null);
        }
        crypto.sign(vpBuilder.build())
                .ifPresent(vp -> didRepo.findDidDocSingle().ifPresentOrElse(
                        didWeb -> didRepo.updateProfileJson(didWeb.getId(), converter.toMap(vp)),
                        () -> didRepo.save(DidDocWeb
                                .builder()
                                .profileJson(converter.toMap(vp))
                                .build())));
    }

    protected VerifiableIndyCredential buildFromDocument(@NonNull MyDocument doc, @NonNull String myDid) {
        final ObjectNode on = converter.fromMap(Objects.requireNonNull(doc.getDocument()), ObjectNode.class);
        on.remove("id");
        on.put("id", myDid);

        // this is needed because the java client serializes with GSON
        // and cannot handle Jackson ObjectNode
        JsonObject subj = GsonConfig.defaultConfig().fromJson(on.toString(), JsonObject.class);

        List<String> types = new ArrayList<>(doc.getType().getType());
        if (doc.typeIsJsonLd() && doc.getSchema() != null && doc.getSchema().getLdType() != null) {
            types.add(doc.getSchema().getLdType());
        }

        return VerifiableIndyCredential
                .builder()
                .id("urn:" + doc.getId().toString())
                .type(types)
                .context(resolveContext(doc.getType(), doc.getSchemaId()))
                .issuanceDate(TimeUtil.currentTimeFormatted())
                .issuer(myDid)
                .label(doc.getLabel())
                .credentialSubject(subj)
                .build();
    }

    protected VerifiableIndyCredential buildFromCredential(@NonNull BPACredentialExchange cred) {
        final ArrayList<String> type = new ArrayList<>(cred.getType().getType());
        type.add("IndyCredential");

        Credential ariesCred = cred.getCredential();
        final ArrayList<Object> context = new ArrayList<>(
                resolveContext(cred.getType(), Objects.requireNonNull(ariesCred).getSchemaId()));
        context.add(ApiConstants.INDY_CREDENTIAL_SCHEMA);

        @SuppressWarnings("rawtypes")
        VerifiableIndyCredentialBuilder builder = VerifiableIndyCredential.builder()
                .id("urn:" + cred.getId().toString())
                .type(type)
                .context(context)
                .issuanceDate(TimeUtil.currentTimeFormatted(cred.calculateIssuedAt()))
                .schemaId(ariesCred.getSchemaId())
                .credDefId(ariesCred.getCredentialDefinitionId())
                .label(cred.getLabel())
                .indyIssuer(id.getDidPrefix() + AriesStringUtil.credDefIdGetDid(ariesCred.getCredentialDefinitionId()))
                .credentialSubject(GsonConfig.defaultConfig().toJsonTree(ariesCred.getAttrs()).getAsJsonObject());
        return builder.build();
    }

    public Optional<VerifiablePresentation<VerifiableIndyCredential>> getVerifiablePresentation() {
        final Optional<DidDocWeb> dbVP = didRepo.findDidDocSingle();
        if (dbVP.isPresent() && dbVP.get().getProfileJson() != null) {
            return Optional
                    .of(converter.fromMap(Objects.requireNonNull(dbVP.get().getProfileJson()), Converter.VP_TYPEREF));
        }
        return Optional.empty();
    }

    protected List<Object> resolveContext(@NonNull CredentialType type, @Nullable String schemaId) {
        if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(type)) {
            return type.getContext();
        } else if (CredentialType.JSON_LD.equals(type)) {
            List<Object> res = new ArrayList<>(type.getContext());
            res.add(schemaId);
            return res;
        }

        final ArrayList<Object> context = new ArrayList<>(type.getContext());

        schemaService.getSchemaFor(schemaId).ifPresent(schema -> {
            Set<String> attributeNames = schema.getSchemaAttributeNames();

            JsonObject ctx = new JsonObject();
            JsonObject content = new JsonObject();
            ctx.add("@context", content);
            content.add("sc", new JsonPrimitive(id.getDidPrefix() + schema.getSchemaId()));

            // filter by did, otherwise there is a cyclic reference in the json-ld parser
            attributeNames.stream().filter(a -> !"did".equals(a)).forEach(name -> {
                JsonObject id = new JsonObject();
                id.addProperty("@id", "sc:" + name);
                content.add(name, id);
            });

            context.add(ctx);
        });

        return context;
    }

}
