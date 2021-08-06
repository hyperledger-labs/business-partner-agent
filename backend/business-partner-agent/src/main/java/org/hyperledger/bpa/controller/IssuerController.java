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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.controller.api.issuer.*;
import org.hyperledger.bpa.impl.IssuerManager;
import org.hyperledger.bpa.impl.util.Converter;

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

    @Inject
    Converter conv;

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
     * List cred defs, items that i can issue
     *
     * @return list of {@link SchemaAPI}
     */
    @Get("/creddef")
    public HttpResponse<List<CredDef>> listCredDefs() {
        return HttpResponse.ok(im.listCredDefs());
    }

    /**
     * Create a new credential definition
     *
     * @param req {@link CreateCredDefRequest}
     * @return {@link HttpResponse}
     */
    @Post("/creddef")
    public HttpResponse<CredDef> createCredDef(@Body CreateCredDefRequest req) {
        return HttpResponse.ok(im.createCredDef(req.getSchemaId(), req.getTag(), req.isSupportRevocation()));
    }

    /**
     * Create a new credential definition
     *
     * @param id {@link UUID} the cred def id
     * @return {@link HttpResponse}
     */
    @Delete("/creddef/{id}")
    public HttpResponse<Void> deleteCredDef(@PathVariable UUID id) {
        im.deleteCredDef(id);
        return HttpResponse.ok();
    }

    /**
     * Issue a credential
     *
     * @param req {@link IssueCredentialSendRequest}
     * @return {@link HttpResponse}
     */
    @Post("/issue-credential/send")
    public HttpResponse<String> issueCredentialSend(@Body IssueCredentialSendRequest req) {
        Optional<V1CredentialExchange> exchange = im.issueCredentialSend(UUID.fromString(req.getCredDefId()),
                UUID.fromString(req.getPartnerId()),
                conv.toMap(req.getDocument()));
        if (exchange.isPresent()) {
            // just return the id and not the full Aries Object.
            // Event handlers will create the db cred ex records
            return HttpResponse.ok(exchange.get().getCredentialExchangeId());
        }
        return HttpResponse.badRequest();
    }

    /**
     * Issue a credential
     *
     * @return {@link HttpResponse}
     */
    @Get("/exchanges")
    public HttpResponse<List<CredEx>> listCredentialExchanges(
            @Parameter(description = "issuer or holder") @Nullable @QueryValue CredentialExchangeRole role,
            @Parameter(description = "partner id") @Nullable @QueryValue String partnerId) {
        return HttpResponse.ok(im.listCredentialExchanges(role, partnerId != null ? UUID.fromString(partnerId) : null));
    }

    /**
     * Revoke an issued credential
     * 
     * @param id credential exchange id
     * @return {@link HttpResponse}
     */
    @Post("/exchanges/{id}/revoke")
    public HttpResponse<CredEx> revokeCredential(@PathVariable UUID id) {
        Optional<CredEx> credEx = im.revokeCredentialExchange(id);
        if (credEx.isPresent()) {
            return HttpResponse.ok(credEx.get());
        }
        return HttpResponse.notFound();
    }

}
