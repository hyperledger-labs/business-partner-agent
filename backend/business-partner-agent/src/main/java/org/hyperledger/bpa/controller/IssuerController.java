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
            @Parameter(description = "issuer or holder") @Nullable @QueryValue String role) {
        return HttpResponse.ok(im.listCredentialExchanges(role));
    }

}
