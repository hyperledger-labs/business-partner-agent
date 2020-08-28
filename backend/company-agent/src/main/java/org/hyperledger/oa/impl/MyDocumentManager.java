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
package org.hyperledger.oa.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.MyDocumentAPI;
import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.repository.MyDocumentRepository;

import lombok.NonNull;

@Singleton
public class MyDocumentManager {

    @Inject
    private MyDocumentRepository docRepo;

    @Inject
    private VPManager vp;

    @Inject
    private Converter converter;

    @SuppressWarnings("boxing")
    public MyDocumentAPI saveNewDocument(@NonNull MyDocumentAPI document) {
        // there should be only one Masterdata credential
        verifyOnlyOneMasterdata(document);

        final MyDocument vc = docRepo.save(converter.toModelObject(document));

        if (document.getIsPublic()) { // new credential, so no need to change the VP when it's private
            vp.recreateVerifiablePresentation();
        }
        return converter.toApiObject(vc);
    }

    public MyDocumentAPI updateDocument(@NonNull UUID id, @NonNull MyDocumentAPI document) {
        final Optional<MyDocument> dbCred = docRepo.findById(id);

        if (dbCred.isEmpty()) {
            throw new WrongApiUsageException("Credential does not exist in database");
        }

        MyDocument dbCredUpdated = converter.updateMyCredential(document, dbCred.get());
        docRepo.update(dbCredUpdated);

        vp.recreateVerifiablePresentation(); // update, so we always need to check, only exception private stays private

        return converter.toApiObject(dbCredUpdated);
    }

    public List<MyDocumentAPI> getMyDocuments() {
        List<MyDocumentAPI> result = new ArrayList<>();
        docRepo.findAll().forEach(c -> {
            result.add(converter.toApiObject(c));
        });

        return result;
    }

    public Optional<MyDocumentAPI> getMyDocumentById(UUID id) {
        Optional<MyDocument> myDoc = docRepo.findById(id);
        if (myDoc.isPresent()) {
            return Optional.of(converter.toApiObject(myDoc.get()));
        }
        return Optional.empty();
    }

    public void deleteMyDocumentById(UUID id) {
        docRepo.deleteById(id);
        vp.recreateVerifiablePresentation();
    }

    private void verifyOnlyOneMasterdata(MyDocumentAPI doc) {
        if (doc.getType().equals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)) {
            docRepo.findAll().forEach(d -> {
                if (d.getType().equals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)) {
                    throw new WrongApiUsageException("Use updateCredential() instead");
                }
            });
        }
    }
}
