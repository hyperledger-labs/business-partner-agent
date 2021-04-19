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
@Requires(classes = {IssuerException.class, ExceptionHandler.class})
public class IssuerExceptionHandler implements ExceptionHandler<IssuerException, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, IssuerException exception) {
        return HttpResponse.status(HttpStatus.PRECONDITION_FAILED)
                .body(new ErrorMessage(exception.getMessage()));
    }

}
