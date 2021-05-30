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
package org.hyperledger.bpa.controller.api.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import org.hyperledger.bpa.api.exception.IssuerException;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = { IssuerException.class, ExceptionHandler.class })
public class IssuerExceptionHandler implements ExceptionHandler<IssuerException, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, IssuerException exception) {
        return HttpResponse.status(HttpStatus.PRECONDITION_FAILED)
                .body(new ErrorMessage(exception.getMessage()));
    }

}
