/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.aries.jsonld;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hyperledger.bpa.client.DidDocClient;

import java.util.Optional;

@Singleton
@Requires(notEnv = Environment.TEST)
public class SchemaContextResolver implements LDContextResolver {

    @Inject
    DidDocClient http;

    @Override
    public String resolve(@NonNull String uri, @NonNull String type) {
        Optional<LDContext> ld = http.call(uri, LDContext.class);
        LDContext ldContext = ld.orElse(new LDContext());
        if (ldContext.hasType(type)) {
            JsonObject typeEl = ldContext.getContext().get(type).getAsJsonObject();
            if (typeEl.has("@id")) {
                return typeEl.get("@id").getAsString();
            }
        }
        throw new IllegalStateException("Could not normalise schema url");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class LDContext {
        @SerializedName("@context")
        private JsonObject context;

        public boolean hasType(@NonNull String type) {
            return context != null && context.has(type);
        }
    }
}
