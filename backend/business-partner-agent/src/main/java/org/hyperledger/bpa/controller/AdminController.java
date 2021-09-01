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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.TAAInfo.TAARecord;
import org.hyperledger.bpa.api.TagAPI;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.admin.*;
import org.hyperledger.bpa.impl.TagService;
import org.hyperledger.bpa.impl.aries.config.RestrictionsManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.mode.indy.EndpointService;
import org.hyperledger.bpa.model.BPARestrictions;

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
    TagService tagService;

    @Inject
    Optional<EndpointService> endpointService;

    @Inject
    RestrictionsManager restrictionsManager;

    @Inject
    RuntimeConfig config;

    @Inject
    AriesClient ac;

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
     * Get a configured schema
     *
     * @param id {@link UUID} the schema id
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
     * Add a schema configuration
     *
     * @param req {@link AddSchemaRequest}
     * @return {@link HttpResponse}
     */
    @Post("/schema")
    public HttpResponse<SchemaAPI> addSchema(@Body AddSchemaRequest req) {
        return HttpResponse.ok(schemaService.addSchema(req.getSchemaId(), req.getLabel(),
                req.getDefaultAttributeName(), req.getTrustedIssuer()));
    }

    /**
     * Update a schema configuration
     *
     * @param id  {@link UUID} the schema id
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
     * Removes a schema configuration. Doing so means the BPA will not process
     * requests containing this schema id anymore.
     *
     * @param id {@link UUID} the schema id
     * @return {@link HttpResponse}
     */
    @Delete("/schema/{id}")
    @ApiResponse(responseCode = "404", description = "If the schema does not exist")
    @ApiResponse(responseCode = "405", description = "If the schema is read only")
    public HttpResponse<Void> removeSchema(@PathVariable UUID id) {
        Optional<SchemaAPI> schema = schemaService.getSchema(id);
        if (schema.isPresent()) {
            schemaService.deleteSchema(id);
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    /**
     * Add a trusted issuer to a schema
     *
     * @param id      {@link UUID} the schema id
     * @param request {@link AddTrustedIssuerRequest}
     * @return {@link TrustedIssuer}
     */
    @Post("/schema/{id}/trustedIssuer")
    public HttpResponse<TrustedIssuer> addTrustedIssuer(
            @PathVariable UUID id,
            @Body AddTrustedIssuerRequest request) {
        Optional<TrustedIssuer> res = restrictionsManager.addRestriction(
                id, request.getIssuerDid(), request.getLabel());
        if (res.isPresent()) {
            return HttpResponse.ok(res.get());
        }
        throw new WrongApiUsageException("Trusted issuer could not be added. Check the logs");
    }

    /**
     * Update a trusted issuer
     *
     * @param id              {@link UUID} the schema id
     * @param trustedIssuerId {@link UUID} the trusted issuer id
     * @param request         {@link UpdateTrustedIssuerRequest}
     * @return {@link TrustedIssuer}
     */
    @Put("/schema/{id}/trustedIssuer/{trustedIssuerId}")
    public HttpResponse<TrustedIssuer> updateTrustedIssuer(
            @SuppressWarnings("unused") @PathVariable UUID id,
            @PathVariable UUID trustedIssuerId,
            @Body UpdateTrustedIssuerRequest request) {
        restrictionsManager.updateLabel(trustedIssuerId, request.getLabel());
        return HttpResponse.ok();
    }

    /**
     * Delete a trusted issuer
     *
     * @param id              {@link UUID} the schema id
     * @param trustedIssuerId {@link UUID} the trusted issuer id
     * @return {@link HttpResponse}
     */
    @Delete("/schema/{id}/trustedIssuer/{trustedIssuerId}")
    @ApiResponse(responseCode = "404", description = "If the trusted issuer does not exist")
    @ApiResponse(responseCode = "405", description = "If the trusted issuer is read only")
    public HttpResponse<Void> deleteTrustedIssuer(
            @SuppressWarnings("unused") @PathVariable UUID id,
            @PathVariable UUID trustedIssuerId) {
        Optional<BPARestrictions> config = restrictionsManager.findById(trustedIssuerId);
        if (config.isPresent()) {
            restrictionsManager.deleteById(trustedIssuerId);
            return HttpResponse.ok();
        }
        return HttpResponse.notFound();
    }

    /**
     * List configured tags
     *
     * @return list of {@link TagAPI}
     */
    @Get("/tag")
    public HttpResponse<List<TagAPI>> listTags() {
        return HttpResponse.ok(tagService.listTags());
    }

    /**
     * Get a configured tag
     *
     * @param id {@link UUID} the tag id
     * @return {@link HttpResponse}
     */
    @Get("/tag/{id}")
    public HttpResponse<TagAPI> getTag(@PathVariable UUID id) {
        final Optional<TagAPI> tag = tagService.getTag(id);
        if (tag.isPresent()) {
            return HttpResponse.ok(tag.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Add a tag
     *
     * @param req {@link AddTagRequest}
     * @return {@link HttpResponse}
     */
    @Post("/tag")
    public HttpResponse<TagAPI> addTag(@Body AddTagRequest req) {
        return HttpResponse.ok(tagService.addTag(req.getName()));
    }

    /**
     * Update a tag
     *
     * @param id  {@link UUID} the tag id
     * @param req {@link UpdateTagRequest}
     * @return {@link HttpResponse}
     */
    @Put("/tag/{id}")
    public HttpResponse<TagAPI> updateTag(@PathVariable UUID id, @Body UpdateTagRequest req) {
        Optional<TagAPI> tagAPI = tagService.updateTag(id, req.getName());
        if (tagAPI.isPresent()) {
            return HttpResponse.ok(tagAPI.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Removes a tag.
     *
     * @param id {@link UUID} the tag id
     * @return {@link HttpResponse}
     */
    @Delete("/tag/{id}")
    @ApiResponse(responseCode = "404", description = "If the tag does not exist")
    @ApiResponse(responseCode = "405", description = "If the tag is read only")
    public HttpResponse<Void> removeTag(@PathVariable UUID id, @Nullable @QueryValue Boolean force) {
        Optional<TagAPI> tag = tagService.getTag(id);
        if (tag.isPresent()) {
            if (!tag.get().getIsReadOnly()) {
                tagService.deleteTag(id, force != null && force);
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