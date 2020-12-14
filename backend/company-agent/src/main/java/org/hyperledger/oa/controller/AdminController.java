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
package org.hyperledger.oa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.TAAInfo.TAARecord;
import org.hyperledger.oa.api.aries.SchemaAPI;
import org.hyperledger.oa.config.RuntimeConfig;
import org.hyperledger.oa.controller.api.admin.AddSchemaRequest;
import org.hyperledger.oa.controller.api.admin.TAADigestRequest;
import org.hyperledger.oa.controller.api.admin.UpdateSchemaRequest;
import org.hyperledger.oa.impl.indy.EndpointService;
import org.hyperledger.oa.impl.aries.SchemaService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/admin")
@Tag(name = "Configuration")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class AdminController {

    @Inject
    SchemaService schemaService;

    @Inject
    Optional<EndpointService> endpointService;

    @Inject
    RuntimeConfig config;

    @Inject
    AriesClient ac;

    /**
     * Aries: List configured schemas
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/schema")
    public HttpResponse<List<SchemaAPI>> listSchemas() {
        return HttpResponse.ok(schemaService.listSchemas());
    }

    /**
     * Aries: Get a schema configuration
     *
     * @param id the schema id
     * @return {@link HttpResponse}
     */
    @Get("/schema/{id}")
    public HttpResponse<SchemaAPI> getSchema(@PathVariable UUID id) {
        final Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            return HttpResponse.ok(schema.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Add a schema configuration
     *
     * @param req {@link AddSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> addSchema(@Body AddSchemaRequest req) {
        return HttpResponse.ok(schemaService.addSchema(req.getSchemaId(), req.getLabel(),
                req.getDefaultAttributeName()));
    }

    /**
     * Aries: Update a schema configuration
     *
     * @param id  the schema id
     * @param req {@link UpdateSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Put("/schema/{id}")
    public HttpResponse<SchemaAPI> updateSchema(@PathVariable UUID id, @Body UpdateSchemaRequest req) {
        Optional<SchemaAPI> schemaAPI = schemaService.updateSchema(id, req.getDefaultAttribute());
        if (schemaAPI.isPresent()) {
            return HttpResponse.ok(schemaAPI.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Removes a schema configuration. Doing so means the BPA will not
     * process requests containing this schema id any more.
     *
     * @param id the schema id
     * @return {@link HttpResponse}
     */
    @Delete("/schema/{id}")
    public HttpResponse<Void> removeSchema(@PathVariable UUID id) {
        Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            if (!schema.get().getIsReadOnly()) {
                schemaService.deleteSchema(id);
                return HttpResponse.ok();
            }
            return HttpResponse.notAllowed();
        }
        return HttpResponse.notFound();
    }

    /**
     * Get runtime configuration
     *
     * @return {@link RuntimeConfig}
     */
    @Get("/config")
    public HttpResponse<RuntimeConfig> getRuntimeConfig() {
        return HttpResponse.ok(config);
    }

    /**
     * Trigger the backend to write configured endpoints to the ledger. TAA digest
     * has to be passed to explicitly confirm prior TTA acceptance by the user for
     * this ledger interaction / session.
     * 
     * @param tAADigest {@link TAADigestRequest}
     * @return {@link HttpResponse}
     */
    @Post("/endpoints/register")
    public HttpResponse<Void> registerEndpoints(@Body TAADigestRequest tAADigest) {
        if (endpointService.isPresent()) {
            endpointService.get().registerEndpoints(tAADigest.getDigest());
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    /**
     * @return true if endpoint registration is required
     */
    @Get("/endpoints/registrationRequired")
    public HttpResponse<Boolean> isEndpointsWriteRequired() {
        if (endpointService.isPresent()) {
            return HttpResponse.ok(endpointService.get().getEndpointRegistrationRequired());
        }
        return HttpResponse.ok(Boolean.FALSE); // not writing to the ledger in this case
    }

    /**
     * Get TAA record (digest, text, version)
     * 
     * @return {@link TAARecord}
     */
    @Get("/taa/get")
    public HttpResponse<TAARecord> getTAARecord() {
        if (endpointService.isPresent()) {
            Optional<TAARecord> taa = endpointService.get().getTAA();
            if (taa.isPresent()) {
                return HttpResponse.ok(taa.get());
            }
        }
        return HttpResponse.notFound();
    }
}
