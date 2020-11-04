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
package org.hyperledger.oa.registry.controller;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.bson.Document;
import org.hyperledger.oa.registry.controller.api.RegistrationResult;
import org.hyperledger.oa.registry.controller.api.RegistryError;
import org.hyperledger.oa.registry.controller.api.RegistryStatistics;
import org.hyperledger.oa.registry.controller.api.Subject;
import org.hyperledger.oa.registry.impl.RegistryManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/api")
@Tag(name = "Registry")
@ExecuteOn(TaskExecutors.IO)
public class RegistryController {

    @Inject
    private RegistryManager mgmt;

    @Inject
    private ObjectMapper mapper;

    /**
     * Register agent
     */
    @Post(value = "/register", consumes = MediaType.TEXT_PLAIN)
    public HttpResponse<?> registerAgent(@Body String raw) {
        Subject subj;
        try {
            subj = mapper.readValue(raw, Subject.class);
            if (StringUtils.isEmpty(subj.getDid())) {
                return HttpResponse.badRequest(new RegistryError(
                        "Missing did, expecting: {\"did\": \"did:web:something\"}"));
            }
        } catch (JsonProcessingException e) {
            return HttpResponse.badRequest(new RegistryError(e.getMessage()));
        }
        return HttpResponse.ok(new RegistrationResult(mgmt.registerAgent(raw, subj.getDid())));
    }

    /**
     * Search registered agents
     */
    @Get("/search")
    public HttpResponse<List<Document>> searchAgents(
            @Parameter(description = "Query string", example = "Acme") @Nullable @QueryValue String q,
            @Parameter(description = "Result size", example = "5") @Nullable @QueryValue Integer s) {
        int limit = 5;
        if (s != null) {
            limit = s.intValue();
        }
        return HttpResponse.ok(mgmt.searchAgents(q, limit));
    }

    /**
     * Get registry statistics
     */
    @Get("/stats")
    public HttpResponse<?> readStatistics() {
        return HttpResponse.ok(new RegistryStatistics(mgmt.getPartnerCount()));
    }
}
