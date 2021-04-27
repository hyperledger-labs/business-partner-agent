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
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.controller.api.issuer.CreateCredDefRequest;
import org.hyperledger.bpa.controller.api.issuer.CreateSchemaRequest;
import org.hyperledger.bpa.impl.IssuerManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("/api/issuer")
@Tag(name = "Credential Issuance Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class IssuerController {

    @Inject
    IssuerManager im;

    /**
     * List configured schemas
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/schema")
    public HttpResponse<List<SchemaAPI>> listSchemas() {
        return HttpResponse.ok(im.listSchemas());
    }

    /**
     * Create a new schema configuration
     *
     * @param req {@link CreateSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> createSchema(@Body CreateSchemaRequest req) {
        return HttpResponse.ok(im.createSchema(req.getSchemaName(), req.getSchemaVersion(),
                req.getAttributes(), req.getSchemaLabel(), req.getDefaultAttributeName()));
    }

    /**
     * Get a configured schema
     *
     * @param id {@link UUID} the schema id
     * @return {@link HttpResponse}
     */
    @Get("/schema/{id}")
    public HttpResponse<SchemaAPI> readSchema(@PathVariable UUID id) {
        final Optional<SchemaAPI> schema = im.readSchema(id);
        if (schema.isPresent()) {
            return HttpResponse.ok(schema.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Create a new credential definition
     *
     * @param req {@link CreateCredDefRequest}
     * @return {@link HttpResponse}
     */
    @Post("/creddef")
    public HttpResponse<Object> createCredDef(@Body CreateCredDefRequest req) {
        return HttpResponse.ok(im.createCredDef(req.getSchemaId(), req.getTag(), req.isSupportRevocation()));
    }

}
