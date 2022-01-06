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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.controller.api.issuer.*;
import org.hyperledger.bpa.impl.IssuerCredentialManager;
import org.hyperledger.bpa.impl.aries.ConnectionLessCredential;
import org.hyperledger.bpa.impl.aries.config.SchemaService;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hyperledger.bpa.controller.IssuerController.ISSUER_CONTROLLER_BASE_URL;

@Controller(ISSUER_CONTROLLER_BASE_URL)
@Tag(name = "Credential Issuance Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class IssuerController {

    public static final String ISSUER_CONTROLLER_BASE_URL = "/api/issuer";

    @Inject
    IssuerCredentialManager im;

    @Inject
    ConnectionLessCredential connectionLess;

    @Inject
    SchemaService schemaService;

    /**
     * List configured schemas
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/schema")
    public HttpResponse<List<SchemaAPI>> listSchemas() {
        return HttpResponse.ok(schemaService.listSchemas());
    }

    /**
     * Create a new schema configuration
     *
     * @param req {@link CreateSchemaRequest}
     * @return {@link SchemaAPI}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> createSchema(@Body CreateSchemaRequest req) {
        return HttpResponse.ok(schemaService.createSchema(req.getSchemaName(), req.getSchemaVersion(),
                req.getAttributes(), req.getSchemaLabel(), req.getDefaultAttributeName()));
    }

    /**
     * Get a configured schema by id
     *
     * @param id {@link UUID} the schema id
     * @return {@link SchemaAPI}
     */
    @Get("/schema/{id}")
    public HttpResponse<SchemaAPI> readSchema(@PathVariable UUID id) {
        final Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            return HttpResponse.ok(schema.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * List configured indy credential definitions
     *
     * @return list of {@link CredDef}
     */
    @Get("/creddef")
    public HttpResponse<List<CredDef>> listCredDefs() {
        return HttpResponse.ok(im.listCredDefs());
    }

    @Get("/templates")
    public HttpResponse<List<IssuanceTemplate>> listIssuanceTemplates() {
        return HttpResponse.ok(im.listIssuanceTemplates());
    }

    /**
     * Create a new indy credential definition
     *
     * @param req {@link CreateCredDefRequest}
     * @return {@link CredDef}
     */
    @Post("/creddef")
    public HttpResponse<CredDef> createCredDef(@Body CreateCredDefRequest req) {
        return HttpResponse.ok(im.createCredDef(req.getSchemaId(), req.getTag(), req.isSupportRevocation()));
    }

    /**
     * Delete a credential definition
     *
     * @param id {@link UUID} the cred def id
     * @return {@link HttpResponse}
     */
    @Delete("/creddef/{id}")
    public HttpResponse<Void> deleteCredDef(@PathVariable UUID id) {
        im.deleteCredDef(id);
        return HttpResponse.ok();
    }

    /**
     * Auto credential exchange: Issuer sends credential to holder
     *
     * @param req {@link IssueCredentialSendRequest}
     * @return {@link HttpResponse}
     */
    @Post("/issue-credential/send")
    public HttpResponse<String> issueCredentialSend(@Valid @Body IssueCredentialSendRequest req) {
        String exchange = im.issueCredential(
                IssuerCredentialManager.IssueCredentialRequest.from(req));
        // just return the id and not the full Aries Object.
        // Event handlers will create the db cred ex records
        return HttpResponse.ok(exchange);
    }

    /**
     * Issue connection-less credential step 1 - prepares credential offer and
     * returns URL for use within the barcode
     * 
     * @param req {@link IssueConnectionLessRequest}
     * @return {@link IssueConnectionLessResponse}
     */
    @Post("/issue-credential/connection-less")
    public HttpResponse<IssueConnectionLessResponse> issueCredentialConnectionLess(
            @Valid @Body IssueConnectionLessRequest req) {
        return HttpResponse.ok(new IssueConnectionLessResponse(connectionLess.issueConnectionLess(req).toString()));
    }

    /**
     * Issue connection-less credential step 2 - redirect with encoded offer
     * 
     * @param id {@link UUID}
     * @return Redirect with encoded credential offer in the location header
     */
    @ApiResponse(responseCode = "307", description = "Redirect with encoded credential offer in the location header")
    @Get("/issue-credential/connection-less/{id}")
    public HttpResponse<Object> handleConnectionLess(@PathVariable UUID id) {
        return HttpResponse.status(HttpStatus.TEMPORARY_REDIRECT).header("location",
                connectionLess.handleConnectionLess(id));
    }

    /**
     * List issued credentials
     *
     * @return list of {@link CredEx}
     */
    @Get("/exchanges")
    public HttpResponse<List<CredEx>> listCredentialExchanges(
            @Parameter(description = "issuer or holder") @Nullable @QueryValue CredentialExchangeRole role,
            @Parameter(description = "partner id") @Nullable @QueryValue String partnerId) {
        return HttpResponse.ok(im.listCredentialExchanges(role, partnerId != null ? UUID.fromString(partnerId) : null));
    }

    /**
     * Get credential exchange
     *
     * @return list of {@link CredEx}
     */
    @Get("/exchanges/{id}")
    public HttpResponse<CredEx> getCredentialExchange(@PathVariable UUID id) {
        return HttpResponse.ok(im.getCredEx(id));
    }

    /**
     * Revoke an issued credential
     *
     * @param id credential exchange id
     * @return {@link CredEx}
     */
    @Put("/exchanges/{id}/revoke")
    public HttpResponse<CredEx> revokeCredential(@PathVariable UUID id) {
        return HttpResponse.ok(im.revokeCredentialExchange(id));
    }

    /**
     * Send holder a new credential offer based on an existing (revoked) exchange
     * record
     * 
     * @param id credential exchange id
     * @return {@link HttpResponse}
     */
    @Post("/exchanges/{id}/re-issue")
    public HttpResponse<Void> reIssueCredential(@PathVariable UUID id) {
        im.reIssueCredential(id);
        return HttpResponse.ok();
    }

    /**
     * Manual credential exchange step two: Issuer sends credential counter offer to
     * holder (in reference to a proposal)
     *
     * @param id           credential exchange id
     * @param counterOffer {@link CredentialOfferRequest}
     * @return {@link CredEx}
     */
    @Put("/exchanges/{id}/send-offer")
    public HttpResponse<CredEx> sendCredentialOffer(@PathVariable UUID id, @Body CredentialOfferRequest counterOffer) {
        return HttpResponse.ok(im.sendCredentialOffer(id, counterOffer));
    }

    /**
     * Manual credential exchange: Issuer declines credential proposal received from
     * the holder
     *
     * @param id  credential exchange id
     * @param req {@link DeclineExchangeRequest}
     * @return HTTP status
     */
    @Put("/exchanges/{id}/decline-proposal")
    public HttpResponse<Void> declineCredentialExchange(@PathVariable UUID id,
            @Body @Nullable DeclineExchangeRequest req) {
        im.declineCredentialProposal(id, req != null ? req.getMessage() : null);
        return HttpResponse.ok();
    }

}
