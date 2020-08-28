package org.hyperledger.oa.impl;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

class FooTest {

    @Test
    void test() throws Exception {
        ObjectMapper m = new ObjectMapper();
        String s = m.writeValueAsString(new Attic(UUID.randomUUID()));
        System.err.println(s);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attic {
        private UUID uuid;
    }

}
