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
package org.hyperledger.bpa.impl.aries.jsonld;

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
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential.VerifiableIndyCredentialBuilder;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.config.TimeUtil;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.aries.wallet.Identity;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.DidDocWeb;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.repository.DidDocWebRepository;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.MyDocumentRepository;

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
    SignVerifyLD crypto;

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
                .stream()
                .filter(credEx -> credEx.stateIsCredentialAcked() || credEx.stateIsDone()
                        || credEx.stateIsCredentialReceived())
                .forEach(credEx -> vcs.add(buildFromCredential(credEx)));

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
                        didWeb -> didRepo.updateProfileJson(didWeb.getId(), vp),
                        () -> didRepo.save(DidDocWeb
                                .builder()
                                .profileJson(vp)
                                .build())));
    }

    protected VerifiableIndyCredential buildFromDocument(@NonNull MyDocument doc, @NonNull String myDid) {
        var document = Objects.requireNonNull(doc.getDocument());

        // this is needed because the java client serializes with GSON
        // and cannot handle Jackson ObjectNode
        JsonObject subj = GsonConfig.defaultConfig().fromJson(document.toString(), JsonObject.class);

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
        if (cred.typeIsJsonLd()) {
            return buildFromLDCredential(cred);
        }
        return buildFromIndyCredential(cred);
    }

    private VerifiableIndyCredential buildFromIndyCredential(@NonNull BPACredentialExchange cred) {
        final ArrayList<String> type = new ArrayList<>(cred.getType().getType());
        type.add("IndyCredential");

        Credential ariesCred = cred.getIndyCredential();
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

    private VerifiableIndyCredential buildFromLDCredential(@NonNull BPACredentialExchange cred) {
        VerifiableCredential vc = Objects.requireNonNull(cred.getLdCredential()).getJsonLD().getCredential();

        List<Object> ctx = new ArrayList<>(vc.getContext());
        ctx.add(ApiConstants.LABELED_CREDENTIAL_SCHEMA);

        List<String> type = new ArrayList<>(vc.getType());
        type.add(ApiConstants.LABELED_CREDENTIAL_NAME);

        @SuppressWarnings("rawtypes")
        VerifiableIndyCredentialBuilder builder = VerifiableIndyCredential.builder()
                .context(ctx)
                .credentialSubject(vc.getCredentialSubject())
                .expirationDate(vc.getExpirationDate())
                .id(vc.getId())
                .issuanceDate(vc.getIssuanceDate())
                .issuer(vc.getIssuer())
                .proof(vc.getProof())
                .type(type)
                .label(cred.getLabel())
                .schemaId(null)
                .credDefId(null)
                .indyIssuer(null);
        return builder.build();
    }

    public Optional<VerifiablePresentation<VerifiableIndyCredential>> getVerifiablePresentation() {
        return didRepo.findDidDocSingle().map(DidDocWeb::getProfileJson);
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
