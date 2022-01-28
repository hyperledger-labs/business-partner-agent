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
package org.hyperledger.bpa.controller;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.uri.UriBuilder;
import lombok.NonNull;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

public abstract class BaseControllerTest {

    private final HttpClient client;

    public BaseControllerTest(HttpClient client) {
        this.client = client;
    }

    public URI buildURI(@NonNull String template, Map<String, Object> values) {
        return UriBuilder.of(template).expand(values);
    }

    public <T> T getById(@NonNull UUID id, @NonNull Class<T> responseType) {
        return getById("/", id, responseType);
    }

    public <T> T getById(@NonNull String path, @NonNull UUID id, @NonNull Class<T> responseType) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return client.toBlocking()
                .retrieve(HttpRequest.GET(path + id), responseType);
    }

    public <T> T getAll(@NonNull String path, @NonNull Argument<T> responseType) {
        return client.toBlocking()
                .exchange(HttpRequest.GET(path), responseType).getBody().orElseThrow();
    }

    public <O, T> HttpResponse<T> post(@NonNull O body, @NonNull Class<T> responseType) {
        return post("", body, responseType);
    }

    public <O, T> HttpResponse<T> post(@NonNull String path, @NonNull O body, @NonNull Class<T> responseType) {
        return client.toBlocking()
                .exchange(HttpRequest.POST(path, body), responseType);
    }

    public <T> void put(@NonNull URI uri, @NonNull T body) {
        client.toBlocking().exchange(HttpRequest.PUT(uri, body));
    }

    public void delete(@NonNull String uri) {
        client.toBlocking().exchange(HttpRequest.DELETE(uri));
    }

    public void deleteById(@NonNull UUID id) {
        client.toBlocking().exchange(HttpRequest.DELETE("/" + id));
    }
}
