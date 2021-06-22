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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.partner.*;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.activity.PartnerLookup;
import org.hyperledger.bpa.impl.aries.CredentialManager;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.impl.aries.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.aries.ProofManager;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/proof-exchanges")
@Tag(name = "Partner (Connection) Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ProofExchangeController {

    @Inject
    PartnerManager pm;

    @Inject
    PartnerLookup partnerLookup;

    @Inject
    CredentialManager credM;

    @Inject
    ProofManager proofM;

    @Inject
    PartnerProofRepository ppRepo;

    @Inject
    ConnectionManager cm;

    @Inject
    PartnerCredDefLookup credLookup;

    /**
     * Get known partners
     *
     * @return list of partners
     */
    @Get
    public HttpResponse<List<AriesProofExchange>> getProofExchanges() {
        return HttpResponse.ok(proofM.getAllPartnerProof());        
    }

    /**
     * Get partner by id
     *
     * @param id {@link UUID} the partner id
     * @return partner
     */
    @Get("/{id}")
    public HttpResponse<AriesProofExchange> getProofExchangebyId(@PathVariable String id) {
        Optional<AriesProofExchange> pProof = proofM.getPartnerProofById(UUID.fromString(id));
        if (pProof.isPresent()) {
            return HttpResponse.ok(pProof.get());
        }
        return HttpResponse.notFound();
    }

}
