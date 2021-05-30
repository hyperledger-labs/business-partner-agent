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

import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

/**
 * Hack to make page refresh work on the VUE single page app, so that there are
 * no "page not found" results.
 *
 * @see <a href=
 *      "https://stackoverflow.com/questions/52630974/micronaut-angular-refresh-page/52655079">
 *      micronaut-angular-refresh-page</a>
 *
 */
@Secured(SecurityRule.IS_AUTHENTICATED)
@Hidden
@Controller
@ExecuteOn(TaskExecutors.IO)
@Slf4j
public class AppController {

    @Inject
    ResourceResolver res;

    @Get(value = "/app/{path:[^\\.]*}", produces = MediaType.TEXT_HTML)
    public HttpResponse<?> refresh(@PathVariable(name = "path") String path) {
        log.debug("Intercepting path: {}", path);
        String resource = "classpath:public/index.html";
        StreamedFile indexFile = new StreamedFile(res.getResource(resource).get());
        return HttpResponse.ok(indexFile);
    }
}
