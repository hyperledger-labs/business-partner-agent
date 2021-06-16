package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Data;

import java.util.List;
import java.util.Map;

@EachProperty("bpa.tags")
@Data
public class TagConfig {

    /**
     * Bean name
     */
    private String name;

    public TagConfig(@Parameter String name) {
        this.name = name;
    }
}
