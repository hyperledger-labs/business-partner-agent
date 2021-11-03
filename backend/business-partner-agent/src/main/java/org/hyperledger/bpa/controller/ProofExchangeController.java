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
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.issuer.DeclineExchangeRequest;
import org.hyperledger.bpa.controller.api.partner.ApproveProofRequest;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.controller.api.partner.SendProofRequest;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestCredentials;
import org.hyperledger.bpa.impl.aries.ProofManager;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("/api/proof-exchanges")
@Tag(name = "Proof Exchange Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ProofExchangeController {

    @Inject
    ProofManager proofM;

    /**
     * Manual proof exchange flow. Get matching wallet credentials before sending or
     * declining the proof request.
     *
     * @param id {@link UUID} the presentationExchangeId
     * @return list of {@link PresentationRequestCredentials}
     */
    @Get("/{id}/matching-credentials")
    public HttpResponse<List<PresentationRequestCredentials>> getMatchingCredentials(@PathVariable UUID id) {
        List<PresentationRequestCredentials> mc = proofM.getMatchingCredentials(id);
        return HttpResponse.ok(mc);
    }

    /**
     * Manual proof exchange flow. Answer ProofRequest with matching attributes
     *
     * @param id  {@link UUID} the presentationExchangeId
     * @param req {@link ApproveProofRequest}
     * @return HTTP status
     */
    @Post("/{id}/prove")
    public HttpResponse<Void> responseToProofRequest(@PathVariable UUID id, @Body @Nullable ApproveProofRequest req) {
        proofM.presentProof(id, req);
        return HttpResponse.ok();
    }

    /**
     * Manual proof exchange flow. Reject ProofRequest received from a partner
     *
     * @param id {@link UUID} the presentationExchangeId
     * @param req {@link DeclineExchangeRequest}
     * @return HTTP status
     */
    @Post("/{id}/decline")
    public HttpResponse<Void> declinePresentProofRequest(
            @PathVariable UUID id,
            @Body @Nullable DeclineExchangeRequest req) {
        proofM.declinePresentProofRequest(id, req != null ? req.getMessage() : null);
        return HttpResponse.ok();
    }

    /**
     * Request proof from partner
     *
     * @param req {@link RequestProofRequest}
     * @return HTTP status
     */
    @Post("/proof-request")
    public HttpResponse<Void> requestProof(
            @RequestBody(description = "One of requestBySchema or requestRaw") @Valid @Body RequestProofRequest req) {
        if (req.getRequestBySchema() != null && req.getRequestRaw() != null) {
            throw new WrongApiUsageException("One of requestBySchema or requestRaw must be set.");
        }
        if (req.isRequestBySchema() && StringUtils.isEmpty(req.getRequestBySchema().getSchemaId())) {
            throw new WrongApiUsageException("Schema id must not be empty");
        }
        proofM.sendPresentProofRequest(req.getPartnerId(), req);
        return HttpResponse.ok();
    }

    /**
     * Send proof to partner
     *
     * @param req {@link SendProofRequest}
     * @return HTTP status
     */
    @Post("/proof-send")
    public HttpResponse<Void> sendProof(
            @Body SendProofRequest req) {
        proofM.sendProofProposal(req.getPartnerId(), req.getMyCredentialId(), req.getExchangeVersion());
        return HttpResponse.ok();
    }

    /**
     * Get proof exchange by id
     *
     * @param id {@link UUID} presentation exchange id
     * @return {@link AriesProofExchange}
     */
    @Get("/{id}")
    public HttpResponse<AriesProofExchange> getProofExchangeById(@PathVariable UUID id) {
        AriesProofExchange pProof = proofM.getPartnerProofById(id);
        return HttpResponse.ok(pProof);
    }

    /**
     * Aries: Deletes a proof exchange by id
     *
     * @param id the proof exchange id
     * @return HTTP status
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteProofExchangeById(
            @PathVariable UUID id) {
        proofM.deletePartnerProof(id);
        return HttpResponse.ok();
    }
}
