package org.hyperledger.oa.impl.aries;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Data;

@EachProperty("oagent.schemas")
@Data
public class Schema {
    private String name;
    private String label;
    private String id;

    public Schema(@Parameter String name) {
        this.name = name;
    }
}