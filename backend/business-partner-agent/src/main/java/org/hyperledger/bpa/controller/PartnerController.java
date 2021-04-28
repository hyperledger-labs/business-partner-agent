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
package org.hyperledger.bpa.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.AriesProof;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.partner.*;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.activity.PartnerLookup;
import org.hyperledger.bpa.impl.aries.CredentialManager;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.impl.aries.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.aries.ProofManager;

import org.hyperledger.aries.api.connection.CreateInvitationResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/partners")
@Tag(name = "Partner (Connection) Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class PartnerController {

    @Inject
    PartnerManager pm;

    @Inject
    PartnerLookup partnerLookup;

    @Inject
    CredentialManager credM;

    @Inject
    ProofManager proofM;

    @Inject
    ConnectionManager cm;

    @Inject
    PartnerCredDefLookup credLookup;

    /**
     * Get known partners
     *
     * @param schemaId Filter Partners by schema id
     * @return list of partners
     */
    @Get
    public HttpResponse<List<PartnerAPI>> getPartners(
            @Parameter(description = "schema id") @Nullable @QueryValue String schemaId) {
        if (StringUtils.isNotBlank(schemaId)) {
            return HttpResponse.ok(credLookup.getIssuersFor(schemaId));
        }
        return HttpResponse.ok(pm.getPartners());
    }

    /**
     * Get partner by id
     *
     * @param id {@link UUID} the partner id
     * @return partner
     */
    @Get("/{id}")
    public HttpResponse<PartnerAPI> getPartnerById(@PathVariable String id) {
        Optional<PartnerAPI> partner = pm.getPartnerById(UUID.fromString(id));
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Update partner
     *
     * @param id     {@link UUID} the partner id
     * @param update {@link UpdatePartnerRequest}
     * @return {@link PartnerAPI}
     */
    @Put("/{id}")
    public HttpResponse<PartnerAPI> updatePartner(
            @PathVariable String id,
            @Body UpdatePartnerRequest update) {
        Optional<PartnerAPI> partner = pm.updatePartner(UUID.fromString(id), update.getAlias());
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Update partner's did
     *
     * @param id     {@link UUID} the partner id
     * @param update {@link UpdatePartnerRequest}
     * @return {@link PartnerAPI}
     */
    @Put("/{id}/did")
    public HttpResponse<PartnerAPI> updatePartnerDid(
            @PathVariable String id,
            @Body UpdatePartnerDidRequest update) {
        Optional<PartnerAPI> partner = pm.updatePartnerDid(UUID.fromString(id), update.getDid());
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Remove partner
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status, no body
     */
    @Delete("/{id}")
    public HttpResponse<Void> removePartner(@PathVariable String id) {
        pm.removePartnerById(UUID.fromString(id));
        return HttpResponse.ok();
    }

    /**
     * Add a new partner
     *
     * @param partner {@link AddPartnerRequest}
     * @return {@link PartnerAPI}
     */
    @Post
    public HttpResponse<PartnerAPI> addPartner(@Body AddPartnerRequest partner) {
        return HttpResponse.created(pm.addPartnerFlow(partner.getDid(), partner.getAlias()));
    }

    /**
     * Accept partner connection request
     * 
     * @param id {@link UUID} the partner id
     * @return HTTP status, no body
     */
    @Put("/{id}/accept")
    public HttpResponse<Void> acceptPartnerRequest(@PathVariable String id) {
        pm.acceptPartner(UUID.fromString(id));
        return HttpResponse.ok();
    }

    /**
     * Lookup/Preview a partners public profile before adding
     *
     * @param did the partners did
     * @return {@link PartnerAPI}
     */
    @Get("/lookup/{did}")
    public HttpResponse<PartnerAPI> lookupPartner(@PathVariable String did) {
        return HttpResponse.ok(partnerLookup.lookupPartner(did));
    }

    /**
     * Reload/Re- lookup a partners public profile
     *
     * @param id {@link UUID} the partner id
     * @return {@link PartnerAPI}
     */
    @Get("/{id}/refresh")
    public HttpResponse<PartnerAPI> refreshPartner(@PathVariable String id) {
        final Optional<PartnerAPI> partner = pm.refreshPartner(UUID.fromString(id));
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Request credential from partner
     *
     * @param id      {@link UUID} the partner id
     * @param credReq {@link RequestCredentialRequest}
     * @return HTTP status
     */
    @Post("/{id}/credential-request")
    public HttpResponse<Void> requestCredential(
            @PathVariable String id,
            @Body RequestCredentialRequest credReq) {
        credM.sendCredentialRequest(
                UUID.fromString(id),
                UUID.fromString(credReq.getDocumentId()));
        return HttpResponse.ok();
    }

    /**
     * Aries: Request proof from partner
     *
     * @param id  {@link UUID} the partner id
     * @param req {@link RequestProofRequest}
     * @return HTTP status
     */
    @Post("/{id}/proof-request")
    public HttpResponse<Void> requestProof(
            @PathVariable String id,
            @RequestBody(description = "One of requestBySchema or requestRaw") @Body RequestProofRequest req) {
        if (req.getRequestBySchema() != null && req.getRequestRaw() != null) {
            throw new WrongApiUsageException("One of requestBySchema or requestRaw must be set.");
        }
        if (req.isRequestBySchema() && StringUtils.isEmpty(req.getRequestBySchema().getSchemaId())) {
            throw new WrongApiUsageException("Schema id must not be empty");
        }
        proofM.sendPresentProofRequest(UUID.fromString(id), req);
        return HttpResponse.ok();
    }

    /**
     * Aries: Send proof to partner
     *
     * @param id  {@link UUID} the partner id
     * @param req {@link SendProofRequest}
     * @return HTTP status
     */
    @Post("/{id}/proof-send")
    public HttpResponse<Void> sendProof(
            @PathVariable String id,
            @Body SendProofRequest req) {
        proofM.sendProofProposal(UUID.fromString(id), req.getMyCredentialId());
        return HttpResponse.ok();
    }

    /**
     * Aries: List proof exchange records
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status
     */
    @Get("/{id}/proof")
    public HttpResponse<List<AriesProof>> getPartnerProofs(
            @PathVariable String id) {
        return HttpResponse.ok(proofM.listPartnerProofs(UUID.fromString(id)));
    }

    /**
     * Aries: Get a proof exchange by id
     *
     * @param id      {@link UUID} the partner id
     * @param proofId the proof id
     * @return HTTP status
     */
    @Get("/{id}/proof/{proofId}")
    public HttpResponse<AriesProof> getPartnerProofById(
            @SuppressWarnings("unused ") @PathVariable String id,
            @PathVariable String proofId) {
        final Optional<AriesProof> proof = proofM.getPartnerProofById(UUID.fromString(proofId));
        if (proof.isPresent()) {
            return HttpResponse.ok(proof.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Deletes a partners proof by id
     *
     * @param id      {@link UUID} the partner id
     * @param proofId the proof id
     * @return HTTP status
     */
    @Delete("/{id}/proof/{proofId}")
    public HttpResponse<Void> deletePartnerProofById(
            @SuppressWarnings("unused ") @PathVariable String id,
            @PathVariable String proofId) {
        proofM.deletePartnerProof(UUID.fromString(proofId));
        return HttpResponse.ok();
    }

    /**
     * Aries: Create a connection-invitation
     *
     * @param req {@link CreatePartnerInvitationRequest}
     * @return {@link PartnerAPI}
     */
    @Post("/invitation")
    public HttpResponse<CreateInvitationResponse> requestConnectionInvitation(
            @Body CreatePartnerInvitationRequest req) {
        final Optional<CreateInvitationResponse> invitation = cm.createConnectionInvitation(req.alias);
        return HttpResponse.ok(invitation.get());
    }
}
