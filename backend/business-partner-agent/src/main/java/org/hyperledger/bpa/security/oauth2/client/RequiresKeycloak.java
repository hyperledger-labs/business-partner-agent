package org.hyperledger.bpa.security.oauth2.client;

import io.micronaut.context.annotation.Requires;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Requires(
        property = "micronaut.security.oauth2.clients.keycloak"
)
public @interface RequiresKeycloak {
    //
}
