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
    HolderCredentialManager holderCredMgmt;

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
    public HttpResponse<MyDocumentAPI> getDocumentById(@PathVariable UUID id) {
        final Optional<MyDocumentAPI> myCred = docMgmt.getMyDocumentById(id);
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
            @PathVariable UUID id,
            @Body WalletDocumentRequest req) {
        return HttpResponse.ok(docMgmt.updateDocument(id, MyDocumentAPI.fromRequest(req)));
    }

    /**
     * Delete a wallet document by id
     *
     * @param id the document id
     * @return HTTP status
     */
    @Delete("/document/{id}")
    public HttpResponse<Void> deleteDocument(
            @PathVariable UUID id) {
        docMgmt.deleteMyDocumentById(id);
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
        return HttpResponse.ok(holderCredMgmt.listCredentials());
    }

    /**
     * Aries: Get aries wallet credential by id
     *
     * @param id the credential id
     * @return {@link AriesCredential}
     */
    @Get("/credential/{id}")
    public HttpResponse<AriesCredential> getCredentialById(@PathVariable UUID id) {
        return HttpResponse.ok(holderCredMgmt.getCredentialById(id));
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
            @PathVariable UUID id,
            @Body WalletCredentialRequest req) {
        holderCredMgmt.updateCredentialById(id, req.getLabel());
        return HttpResponse.ok();
    }

    /**
     * Aries: Delete an aries wallet credential by id
     *
     * @param id the credential id
     * @return HTTP status
     */
    @Delete("/credential/{id}")
    public HttpResponse<Void> deleteCredential(@PathVariable UUID id) {
        holderCredMgmt.deleteCredentialById(id);
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
            @PathVariable UUID id) {
        holderCredMgmt.toggleVisibility(id);
        return HttpResponse.ok();
    }

    /**
     * Manual credential exchange step four: Holder accepts credential offer from
     * issuer
     *
     * @param id the credential id
     * @return HTTP status
     */
    @Put("/credential/{id}/accept-offer")
    public HttpResponse<Void> acceptCredentialOffer(
            @PathVariable UUID id) {
        holderCredMgmt.sendCredentialRequest(id);
        return HttpResponse.ok();
    }

    /**
     * Manual credential exchange: Holder declines credential offer from
     * issuer
     *
     * @param id the credential id
     * @return HTTP status
     */
    @Put("/credential/{id}/decline-offer")
    public HttpResponse<Void> declineCredentialOffer(
            @PathVariable UUID id) {
        holderCredMgmt.declineCredentialOffer(id);
        return HttpResponse.ok();
    }

}
