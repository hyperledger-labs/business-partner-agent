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
package org.hyperledger.bpa.impl;

import io.micronaut.core.util.ArrayUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.Setter;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.activity.DocumentValidator;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.MyDocumentRepository;

import java.util.*;

@Singleton
public class MyDocumentManager {

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    VPManager vp;

    @Inject
    Converter converter;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    @Setter
    DocumentValidator validator;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public MyDocumentAPI saveNewDocument(@NonNull MyDocumentAPI apiDoc) {
        validator.validateNew(apiDoc);

        labelStrategy.apply(apiDoc);
        final MyDocument dbDoc = docRepo.save(converter.toModelObject(apiDoc));

        if (apiDoc.getIsPublic()) { // new credential, so no need to change the VP when it's private
            vp.recreateVerifiablePresentation();
        }
        return converter.toApiObject(dbDoc);
    }

    public MyDocumentAPI updateDocument(@NonNull UUID id, @NonNull MyDocumentAPI apiDoc) {
        final Optional<MyDocument> dbCred = docRepo.findById(id);
        if (dbCred.isPresent()) {
            validator.validateExisting(dbCred, apiDoc);

            labelStrategy.apply(apiDoc);
            MyDocument dbDoc = converter.updateMyCredential(apiDoc, dbCred.get());
            docRepo.update(dbDoc);

            // update, so we always need to check, only exception private stays private
            vp.recreateVerifiablePresentation();

            return converter.toApiObject(dbDoc);
        }
        throw new WrongApiUsageException(ms.getMessage("api.document.not.found", Map.of("id", id)));
    }

    public List<MyDocumentAPI> getMyDocuments(CredentialType... types) {
        List<MyDocumentAPI> result = new ArrayList<>();
        List<CredentialType> search = Arrays.asList(Objects.requireNonNullElseGet(
                ArrayUtils.isEmpty(types) ? null : types,
                CredentialType::values));
        docRepo.findByTypeIn(search).forEach(dbDoc -> result.add(converter.toApiObject(dbDoc)));
        return result;
    }

    public Optional<MyDocumentAPI> getMyDocumentById(@NonNull UUID id) {
        Optional<MyDocument> myDoc = docRepo.findById(id);
        return myDoc.map(converter::toApiObject);
    }

    public void deleteMyDocumentById(@NonNull UUID id) {
        docRepo.deleteById(id);
        vp.recreateVerifiablePresentation();
    }
}
