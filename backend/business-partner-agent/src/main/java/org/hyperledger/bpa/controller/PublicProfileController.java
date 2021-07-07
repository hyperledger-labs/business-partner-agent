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
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.impl.DidDocManager;
import org.hyperledger.bpa.impl.activity.VPManager;

import javax.inject.Inject;
import java.util.Optional;

@Controller
@Tag(name = "Public Web Profile")
@Validated
@Secured(SecurityRule.IS_ANONYMOUS)
@ExecuteOn(TaskExecutors.IO)
public class PublicProfileController {

    @Inject
    VPManager vpMgmt;

    @Inject
    DidDocManager didDocManager;

    @Get("/profile.jsonld")
    public HttpResponse<VerifiablePresentation<VerifiableIndyCredential>> getMasterdata() {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> vp = vpMgmt.getVerifiablePresentation();
        if (vp.isPresent()) {
            return HttpResponse.ok(vp.get());
        }
        return HttpResponse.notFound();
    }

    @Get("/.well-known/did.json")
    public HttpResponse<DIDDocument> getDid() {

        Optional<DIDDocument> api = didDocManager.getDidDocument();
        if (api.isPresent()) {
            return HttpResponse.ok(api.get());
        }
        return HttpResponse.notFound();
    }
}
