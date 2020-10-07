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
package org.hyperledger.oa.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.oa.api.aries.SchemaAPI;
import org.hyperledger.oa.config.RuntimeConfig;
import org.hyperledger.oa.controller.api.admin.AddSchemaRequest;
import org.hyperledger.oa.impl.aries.SchemaService;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/api/admin")
@Tag(name = "Configuration")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class AdminController {

    @Inject
    Optional<SchemaService> schemaService;

    @Inject
    RuntimeConfig config;

    /**
     * Aries: List configured schemas
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/schema")
    public HttpResponse<List<SchemaAPI>> listSchemas() {
        if (schemaService.isPresent()) {
            return HttpResponse.ok(schemaService.get().listSchemas());
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
        if (schemaService.isPresent()) {
            return HttpResponse.ok(schemaService.get().addSchema(req.getSchemaId(), req.getLabel()));
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
    public HttpResponse<Void> removeSchema(@PathVariable String id) {
        if (schemaService.isPresent()) {
            schemaService.get().deleteSchema(UUID.fromString(id));
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    /**
     * Aries: Get a schema configuration
     *
     * @param id the schema id
     * @return {@link HttpResponse}
     */
    @Get("/schema/{id}")
    public HttpResponse<SchemaAPI> getSchema(@PathVariable UUID id) {
        if (schemaService.isPresent()) {
            return HttpResponse.ok(schemaService.get().getSchema(id));
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

}
