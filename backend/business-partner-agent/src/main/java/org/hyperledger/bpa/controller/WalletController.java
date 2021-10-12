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
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.controller.api.wallet.WalletCredentialRequest;
import org.hyperledger.bpa.controller.api.wallet.WalletDocumentRequest;
import org.hyperledger.bpa.impl.MyDocumentManager;
import org.hyperledger.bpa.impl.aries.HolderCredentialManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/wallet")
@Tag(name = "Wallet Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class WalletController {

    @Inject
    MyDocumentManager docMgmt;

    @Inject
    HolderCredentialManager credMgmt;

    // -------------------------------------
    // Document Management
    // -------------------------------------

    /**
     * List wallet documents
     *
     * @return list of {@link MyDocumentAPI}
     */
    @Get("/document")
    public HttpResponse<List<MyDocumentAPI>> getDocuments() {
        final List<MyDocumentAPI> myCreds = docMgmt.getMyDocuments();
        return HttpResponse.ok(myCreds);
    }

    /**
     * Get wallet document by id
     *
     * @param id the document id
     * @return {@link MyDocumentAPI}
     */
    @Get("/document/{id}")
    public HttpResponse<MyDocumentAPI> getDocumentById(@PathVariable String id) {
        final Optional<MyDocumentAPI> myCred = docMgmt.getMyDocumentById(UUID.fromString(id));
        if (myCred.isPresent()) {
            return HttpResponse.ok(myCred.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Add a document to the wallet
     *
     * @param req {@link WalletDocumentRequest}
     * @return {@link HttpResponse}
     */
    @Post("/document")
    public HttpResponse<MyDocumentAPI> addDocument(@Body WalletDocumentRequest req) {
        return HttpResponse.ok(docMgmt.saveNewDocument(MyDocumentAPI.fromRequest(req)));
    }

    /**
     * Update a wallet document by id
     *
     * @param id  the document id
     * @param req {@link WalletDocumentRequest}
     * @return {@link HttpResponse}
     */
    @Put("/document/{id}")
    public HttpResponse<MyDocumentAPI> updateDocument(
            @PathVariable String id,
            @Body WalletDocumentRequest req) {
        return HttpResponse.ok(docMgmt.updateDocument(UUID.fromString(id), MyDocumentAPI.fromRequest(req)));
    }

    /**
     * Delete a wallet document by id
     *
     * @param id the document id
     * @return HTTP status
     */
    @Delete("/document/{id}")
    public HttpResponse<Void> deleteDocument(
            @PathVariable String id) {
        docMgmt.deleteMyDocumentById(UUID.fromString(id));
        return HttpResponse.ok();
    }

    // -------------------------------------
    // Credential Management
    // -------------------------------------

    /**
     * Aries: List aries wallet credentials
     *
     * @return list of {@link AriesCredential}
     */
    @Get("/credential")
    public HttpResponse<List<AriesCredential>> getCredentials() {
        return HttpResponse.ok(credMgmt.listCredentials());
    }

    /**
     * Aries: Get aries wallet credential by id
     *
     * @param id the credential id
     * @return {@link AriesCredential}
     */
    @Get("/credential/{id}")
    public HttpResponse<AriesCredential> getCredentialById(@PathVariable String id) {
        return HttpResponse.ok(credMgmt.getAriesCredentialById(UUID.fromString(id)));
    }

    /**
     * Aries: Set/update a credentials label
     *
     * @param id  the credential id
     * @param req {@link WalletCredentialRequest}
     * @return HTTP status
     */
    @Put("/credential/{id}")
    public HttpResponse<Void> updateCredential(
            @PathVariable String id,
            @Body WalletCredentialRequest req) {
        credMgmt.updateCredentialById(UUID.fromString(id), req.getLabel());
        return HttpResponse.ok();
    }

    /**
     * Aries: Delete an aries wallet credential by id
     *
     * @param id the credential id
     * @return HTTP status
     */
    @Delete("/credential/{id}")
    public HttpResponse<Void> deleteCredential(@PathVariable String id) {
        credMgmt.deleteCredentialById(UUID.fromString(id));
        return HttpResponse.ok();
    }

    /**
     * Toggles the credentials visibility
     *
     * @param id the credential id
     * @return {@link HttpResponse}
     */
    @Put("/credential/{id}/toggle-visibility")
    public HttpResponse<Void> toggleCredentialVisibility(
            @PathVariable String id) {
        credMgmt.toggleVisibility(UUID.fromString(id));
        return HttpResponse.ok();
    }
}
