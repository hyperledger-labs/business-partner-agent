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
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.partner.*;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestVersion;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.activity.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.activity.PartnerLookup;
import org.hyperledger.bpa.impl.aries.chat.ChatMessageManager;
import org.hyperledger.bpa.impl.aries.chat.ChatMessageService;
import org.hyperledger.bpa.impl.aries.credential.HolderCredentialManager;
import org.hyperledger.bpa.impl.aries.proof.ProofManager;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateManager;
import org.hyperledger.bpa.persistence.model.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("/api/partners")
@Tag(name = "Partner Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class PartnerController {

    @Inject
    PartnerManager pm;

    @Inject
    PartnerLookup partnerLookup;

    @Inject
    HolderCredentialManager credM;

    @Inject
    ProofManager proofM;

    @Inject
    PartnerCredDefLookup credLookup;

    @Inject
    ChatMessageManager chatMessageManager;

    @Inject
    ChatMessageService chatMessageService;

    @Inject
    ProofTemplateManager proofTemplateManager;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

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
    public HttpResponse<PartnerAPI> getPartnerById(@PathVariable UUID id) {
        Optional<PartnerAPI> partner = pm.getPartnerById(id);
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
            @PathVariable UUID id,
            @Body UpdatePartnerRequest update) {
        Optional<PartnerAPI> partner = pm.updatePartner(id, update);
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
            @PathVariable UUID id,
            @Body UpdatePartnerDidRequest update) {
        Optional<PartnerAPI> partner = pm.updatePartnerDid(id, update.getDid());
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Remove partner
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status, no Body
     */
    @Delete("/{id}")
    public HttpResponse<Void> removePartner(@PathVariable UUID id) {
        pm.removePartnerById(id);
        return HttpResponse.ok();
    }

    /**
     * Add a new partner
     *
     * @param request {@link AddPartnerRequest}
     * @return {@link PartnerAPI}
     */
    @Post
    public HttpResponse<PartnerAPI> addPartner(@Body AddPartnerRequest request) {
        return HttpResponse.created(pm.addPartnerFlow(request));
    }

    /**
     * Manual connection flow. Accept partner connection request
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status, no Body
     */
    @Put("/{id}/accept")
    public HttpResponse<Void> acceptPartnerRequest(@PathVariable UUID id) {
        pm.acceptPartner(id);
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
    public HttpResponse<PartnerAPI> refreshPartner(@PathVariable UUID id) {
        final Optional<PartnerAPI> partner = pm.refreshPartner(id);
        if (partner.isPresent()) {
            return HttpResponse.ok(partner.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Request credential from partner
     *
     * @param id      {@link UUID} the partner id
     * @param credReq {@link RequestCredentialRequest}
     * @return HTTP status
     */
    @Post("/{id}/credential-request")
    public HttpResponse<Void> requestCredential(
            @PathVariable UUID id,
            @Body RequestCredentialRequest credReq) {
        credM.sendCredentialRequest(
                id,
                UUID.fromString(credReq.getDocumentId()),
                credReq.getExchangeVersion());
        return HttpResponse.ok();
    }

    /**
     * List proof exchange records
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status
     */
    @Get("/{id}/proof-exchanges")
    public HttpResponse<List<AriesProofExchange>> getPartnerProofs(@PathVariable UUID id) {
        return HttpResponse.ok(proofM.listPartnerProofs(id));
    }

    /**
     * Request proof from partner by proof template
     *
     * @param id         partner id
     * @param templateId proof template id
     * @param version    {@link PresentationRequestVersion}
     * @return Http Status
     */
    @Put("/{id}/proof-request/{templateId}")
    public HttpResponse<Void> invokeProofRequestByTemplate(
            @PathVariable UUID id,
            @PathVariable UUID templateId,
            @Body @Nullable PresentationRequestVersion version) {
        proofTemplateManager.invokeProofRequestByTemplate(templateId, id,
                version != null ? version.getExchangeVersion() : null);
        return HttpResponse.ok();
    }

    /**
     * Request proof from partner
     *
     * @deprecated use proof exchange controller
     *             {@link ProofExchangeController#requestProof}
     * @param id  {@link UUID} the partner id
     * @param req {@link RequestProofRequest}
     * @return HTTP status
     */
    @Post("/{id}/proof-request")
    @Deprecated
    public HttpResponse<Void> requestProof(
            @PathVariable UUID id,
            @RequestBody(description = "One of requestBySchema or requestRaw") @Body RequestProofRequest req) {
        if (req.getRequestBySchema() != null && req.getRequestRaw() != null) {
            throw new WrongApiUsageException(msg.getMessage("api.partner.proof.request.empty.body"));
        }
        if (req.isRequestBySchema() && StringUtils.isEmpty(req.getRequestBySchema().getSchemaId())) {
            throw new WrongApiUsageException(msg.getMessage("api.partner.proof.request.no.schema.id"));
        }
        proofM.sendPresentProofRequest(id, req);
        return HttpResponse.ok();
    }

    /**
     * Send proof to partner
     *
     * @deprecated use proof exchange controller
     *             {@link ProofExchangeController#sendProof}
     * @param id  {@link UUID} the partner id
     * @param req {@link SendProofRequest}
     * @return HTTP status
     */
    @Post("/{id}/proof-send")
    @Deprecated
    public HttpResponse<Void> sendProof(
            @PathVariable UUID id,
            @Body SendProofRequest req) {
        proofM.sendProofProposal(id, req.getMyCredentialId(), req.getExchangeVersion());
        return HttpResponse.ok();
    }

    /**
     * Send chat message to partner
     *
     * @param id  {@link UUID} the partner id
     * @param msg {@link SendMessageRequest}
     * @return HTTP status
     */
    @Post("/{id}/messages")
    public HttpResponse<Void> sendMessage(
            @PathVariable UUID id,
            @Body SendMessageRequest msg) {
        chatMessageManager.sendMessage(id, msg.getContent());
        return HttpResponse.ok();
    }

    /**
     * Get chat messages for partner
     *
     * @param id {@link UUID} the partner id
     * @return HTTP status
     */
    @Get("/{id}/messages")
    public HttpResponse<List<ChatMessage>> getMessagesForPartner(
            @PathVariable UUID id) {
        return HttpResponse.ok(chatMessageService.getMessagesForPartner(id));
    }
}
