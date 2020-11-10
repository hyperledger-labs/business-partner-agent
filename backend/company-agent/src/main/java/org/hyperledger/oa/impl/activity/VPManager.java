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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.scheduling.annotation.Async;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential.VerifiableIndyCredentialBuilder;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.config.TimeUtil;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.BankAccount;
import org.hyperledger.oa.api.aries.BankAccountVC;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.DidDocWeb;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.repository.DidDocWebRepository;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Singleton
public class VPManager {

    @Inject
    Identity id;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    DidDocWebRepository didRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    CryptoManager crypto;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    Converter converter;

    public void recreateVerifiablePresentation() {
        List<VerifiableIndyCredential> vcs = new ArrayList<>();

        String myDid = id.getMyDid();

        docRepo.findByIsPublicTrue().forEach(doc -> vcs.add(buildFromDocument(doc, myDid)));

        credRepo.findByIsPublicTrue().forEach(cred -> {
            if (!CredentialType.OTHER.equals(cred.getType())) {
                vcs.add(buildFromCredential(cred, myDid));
            }
        });

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
        crypto.sign(vpBuilder.build()).ifPresent(vp -> {
            getVerifiablePresentationInternal().ifPresentOrElse(didWeb -> {
                didRepo.updateProfileJson(didWeb.getId(), converter.toMap(vp));
            }, () -> {
                didRepo.save(DidDocWeb
                        .builder()
                        .profileJson(converter.toMap(vp))
                        .build());
            });
        });
    }

    protected VerifiableIndyCredential buildFromDocument(@NonNull MyDocument doc, @NonNull String myDid) {
        Object subj;
        if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(doc.getType())) {
            BankAccount ba = converter.fromMap(doc.getDocument(), BankAccount.class);
            subj = new BankAccountVC(myDid, ba);
        } else {
            final ObjectNode on = converter.fromMap(doc.getDocument(), ObjectNode.class);
            on.remove("id");
            on.put("id", myDid);
            // this is needed because the java client serializes with GSON
            // and cannot handle Jackson ObjectNode
            subj = GsonConfig.defaultConfig().fromJson(on.toString(), Object.class);
        }
        return VerifiableIndyCredential
                .builder()
                .id("urn:" + doc.getId().toString())
                .type(doc.getType().getType())
                .context(doc.getType().getContext())
                .issuanceDate(TimeUtil.currentTimeFormatted())
                .issuer(myDid)
                .label(doc.getLabel())
                .credentialSubject(subj)
                .build();
    }

    private VerifiableIndyCredential buildFromCredential(@NonNull MyCredential cred, @Nullable String myDid) {
        final ArrayList<String> type = new ArrayList<>(cred.getType().getType());
        type.add("IndyCredential");

        final ArrayList<String> context = new ArrayList<>(cred.getType().getContext());
        context.add(ApiConstants.INDY_CREDENTIAL_SCHEMA);

        Credential ariesCred = converter.fromMap(cred.getCredential(), Credential.class);

        Object credSubj;
        if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(cred.getType())) {
            BankAccount ba = ariesCred.to(BankAccount.class);
            credSubj = new BankAccountVC(myDid, ba);
        } else {
            credSubj = converter.fromMap(cred.getCredential(), Object.class);
        }

        @SuppressWarnings("rawtypes")
        VerifiableIndyCredentialBuilder builder = VerifiableIndyCredential.builder()
                .id("urn:" + cred.getId().toString())
                .type(type)
                .context(context)
                .issuanceDate(TimeUtil.currentTimeFormatted(cred.getIssuedAt()))
                .schemaId(ariesCred.getSchemaId())
                .credDefId(ariesCred.getCredentialDefinitionId())
                .label(cred.getLabel())
                .credentialSubject(credSubj);
        partnerRepo.findByConnectionId(cred.getConnectionId()).ifPresent(p -> builder.indyIssuer(p.getDid()));
        return builder.build();
    }

    public Optional<VerifiablePresentation<VerifiableIndyCredential>> getVerifiablePresentation() {
        final Optional<DidDocWeb> dbVP = getVerifiablePresentationInternal();
        if (dbVP.isPresent() && dbVP.get().getProfileJson() != null) {
            return Optional.of(converter.fromMap(dbVP.get().getProfileJson(), Converter.VP_TYPEREF));
        }
        return Optional.empty();
    }

    private Optional<DidDocWeb> getVerifiablePresentationInternal() {
        Optional<DidDocWeb> result = Optional.empty();
        final Iterator<DidDocWeb> iterator = didRepo.findAll().iterator();
        if (iterator.hasNext()) {
            result = Optional.of(iterator.next());
            if (iterator.hasNext()) {
                throw new IllegalStateException("More than one did doc entity found");
            }
        }
        return result;
    }
}
