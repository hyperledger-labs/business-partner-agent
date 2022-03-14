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
package org.hyperledger.bpa.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
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
import org.hyperledger.bpa.controller.api.PaginationCommand;
import org.hyperledger.bpa.controller.api.invitation.APICreateInvitationResponse;
import org.hyperledger.bpa.controller.api.issuer.*;
import org.hyperledger.bpa.impl.aries.creddef.CredDefManager;
import org.hyperledger.bpa.impl.aries.credential.IssuerManager;
import org.hyperledger.bpa.impl.aries.credential.OOBCredentialOffer;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;

import javax.validation.Valid;
import java.util.List;
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
    IssuerManager im;

    @Inject
    CredDefManager credDef;

    @Inject
    OOBCredentialOffer connectionLess;

    @Inject
    SchemaService schemaService;

    /**
     * Create a new schema on the indy ledger and import it
     *
     * @param req {@link CreateSchemaRequest}
     * @return {@link SchemaAPI}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> createSchema(@Body @Valid CreateSchemaRequest req) {
        return HttpResponse.ok(schemaService.createSchema(req.getSchemaName(), req.getSchemaVersion(),
                req.getAttributes(), req.getSchemaLabel(), req.getDefaultAttributeName()));
    }

    /**
     * List credential definitions, items that I can issue
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/creddef")
    public HttpResponse<List<CredDef>> listCredDefs() {
        return HttpResponse.ok(credDef.listCredDefs());
    }

    /**
     * Create a new indy credential definition, and send it to the ledger
     *
     * @param req {@link CreateCredDefRequest}
     * @return {@link CredDef}
     */
    @Post("/creddef")
    public HttpResponse<CredDef> createCredDef(@Body CreateCredDefRequest req) {
        return HttpResponse.ok(credDef.createCredDef(req.getSchemaId(), req.getTag(), req.isSupportRevocation()));
    }

    /**
     * Delete a indy credential definition (will not delete it from the ledger)
     *
     * @param id {@link UUID} the cred def id
     * @return {@link HttpResponse}
     */
    @Delete("/creddef/{id}")
    public HttpResponse<Void> deleteCredDef(@PathVariable UUID id) {
        credDef.deleteCredDef(id);
        return HttpResponse.ok();
    }

    /**
     * Auto credential exchange: Issuer sends credential to holder
     *
     * @param req {@link IssueCredentialRequest}
     * @return {@link HttpResponse}
     */
    @Post("/issue-credential/send")
    public HttpResponse<String> issueCredential(@Valid @Body IssueCredentialRequest req) {
        String exchangeId = im.issueCredential(req);
        // just return the id and not the full Aries Object.
        // Event handlers will create the db cred ex records
        return HttpResponse.ok(exchangeId);
    }

    /**
     * Issue OOB credential step 1 - prepares credential offer and returns URL for
     * use within the barcode
     *
     * @param req {@link IssueOOBCredentialRequest}
     * @return {@link APICreateInvitationResponse}
     */
    @Post("/issue-credential/oob-attachment")
    public HttpResponse<APICreateInvitationResponse> issueCredentialConnectionLess(
            @Valid @Body IssueOOBCredentialRequest req) {
        return HttpResponse.ok(connectionLess.issueConnectionLess(req));
    }

    /**
     * Issue OOB credential step 2 - redirect with encoded offer
     *
     * @param id {@link UUID}
     * @return Redirect with encoded credential offer in the location header
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @ApiResponse(responseCode = "301", description = "Redirect with encoded credential offer in the location header")
    @Get("/issue-credential/oob-attachment/{id}")
    public HttpResponse<Object> handleConnectionLess(@PathVariable UUID id) {
        return HttpResponse.status(HttpStatus.MOVED_PERMANENTLY).header("location",
                connectionLess.handleConnectionLess(id));
    }

    /**
     * List issued or received credentials
     *
     * @param pc        {@link PaginationCommand}
     * @param partnerId partner id
     * @param role      {@link CredentialExchangeRole}
     * @return list of {@link CredEx}
     */
    @Get("/exchanges{?pc*}")
    public HttpResponse<Page<CredEx>> listCredentialExchanges(
            @Valid @Nullable PaginationCommand pc,
            @Parameter(description = "issuer or holder") @Nullable @QueryValue CredentialExchangeRole role,
            @Parameter(description = "partner id") @Nullable @QueryValue UUID partnerId) {
        return HttpResponse.ok(im.foo(role, partnerId, pc.toPageable()));
    }

    /**
     * Get credential exchange
     *
     * @return list of {@link CredEx}
     */
    @Get("/exchanges/{id}")
    public HttpResponse<CredEx> getCredentialExchange(@PathVariable UUID id) {
        return HttpResponse.ok(im.findCredentialExchangeById(id));
    }

    /**
     * Revoke an issued credential
     *
     * @param id credential exchange id
     * @return {@link CredEx}
     */
    @Put("/exchanges/{id}/revoke")
    public HttpResponse<CredEx> revokeCredential(@PathVariable UUID id) {
        return HttpResponse.ok(im.revokeCredential(id));
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
    public HttpResponse<CredEx> sendCredentialOffer(@PathVariable UUID id,
            @Body @Valid CredentialOfferRequest counterOffer) {
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
