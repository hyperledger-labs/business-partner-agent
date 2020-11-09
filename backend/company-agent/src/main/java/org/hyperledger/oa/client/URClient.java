/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.api.DidDocument;
import org.hyperledger.oa.impl.util.Converter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpRequest.GET;

/**
 * Universal Resolver Client
 *
 */
@Slf4j
@Singleton
// TODO probably better to use:
// https://github.com/decentralized-identity/universal-resolver/tree/master/resolver/java/uni-resolver-client
public class URClient {

    @Client("${oagent.resolver.url}")
    @Inject
    private RxHttpClient client;

    @Inject
    private ObjectMapper mapper;

    private final OkHttpClient okClient = new OkHttpClient();

    private final Map<CharSequence, CharSequence> headers;

    public URClient() {
        super();
        this.headers = Map.of("Accept", "application/ld+json");
    }

    @Cacheable(cacheNames = { "ur-cache" })
    public Optional<DidDocAPI> getDidDocument(String did) {
        DidDocAPI doc = null;
        try (BlockingHttpClient bc = client.toBlocking()) {
            doc = bc.retrieve(
                    GET("/1.0/identifiers/" + did)
                            .headers(headers),
                    DidDocument.class)
                    .getDidDocument();
        } catch (HttpClientResponseException e) {
            String msg = "Call to universal resolver failed - msg: " + e.getMessage() + ", status: " + e.getStatus();
            log.error(msg);
            throw new PartnerException(msg);
        } catch (IOException e) {
            log.warn("Could not close connection", e);
        }
        return Optional.ofNullable(doc);
    }

    public Optional<VerifiablePresentation<VerifiableIndyCredential>> getPublicProfile(String url) {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> result = Optional.empty();
        try {
            URL url2 = new URL(url);
            Request request = new Request.Builder()
                    .url(url2.toString())
                    .build();
            try (Response response = okClient.newCall(request).execute()) {
                String body = response.body().string();
                VerifiablePresentation<VerifiableIndyCredential> md = mapper.readValue(body, Converter.VP_TYPEREF);
                result = Optional.of(md);
            }
        } catch (MalformedURLException e) {
            String msg = "Malformed endpoint URL: " + url;
            log.error(msg, e);
            throw new PartnerException(msg);
        } catch (IOException e) {
            String msg = "Call to partner web endpoint failed - msg: " + e.getMessage();
            log.error(msg);
            throw new PartnerException(msg);
        }
        return result;
    }

}
