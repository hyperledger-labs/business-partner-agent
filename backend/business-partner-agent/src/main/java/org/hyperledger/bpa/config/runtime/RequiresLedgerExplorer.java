package org.hyperledger.bpa.config.runtime;

import io.micronaut.context.annotation.Requires;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Requires(
        property = "bpa.ledger.browser",
        pattern = "^(http|https)://.*")
public @interface RequiresLedgerExplorer {
    //
}
