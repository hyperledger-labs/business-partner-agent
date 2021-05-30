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
package org.hyperledger.bpa.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.client.api.LedgerQueryResult;
import org.hyperledger.bpa.client.api.LedgerQueryResult.DomainTransaction;
import org.hyperledger.bpa.client.api.LedgerQueryResult.DomainTransaction.TxnMetadata;
import org.hyperledger.bpa.config.runtime.RequiresLedgerExplorer;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.bpa.impl.util.AriesStringUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Client that connects to a bcgov ledger explorer:
 * https://github.com/bcgov/von-network.git The client is not active if no
 * browser URL is configured. Installing a ledger explorer can be done in three
 * steps: 1. Clone the repository 2. Provide a genesis file 3. Create a systemd
 * service unit like
 *
 * [Unit] Description=IndyNode Requires=docker.service After=docker.service
 *
 * [Service] Type=oneshot RemainAfterExit=yes User=indy Group=docker
 * ExecStart=/opt/von-network/manage start-web
 * GENESIS_FILE=/home/indy/genesis/idu_genesis LEDGER_INSTANCE_NAME=MyName
 * ExecStop=/opt/von-network/manage stop
 *
 * [Install] WantedBy=multi-user.target
 */
@Slf4j
@Singleton
@RequiresLedgerExplorer
public class LedgerExplorerClient {

    @Setter(AccessLevel.PROTECTED)
    @Value("${bpa.ledger.browser}")
    String url;

    @Inject
    @Setter(value = AccessLevel.PROTECTED)
    ObjectMapper mapper;

    private final OkHttpClient ok = new OkHttpClient();

    /**
     * Query the ledger explorer for a list of credential definitions that are based
     * on a did or a TRX id.
     * 
     * @param query query can be either a did or a transaction id.
     * @return optional list of {@link PartnerCredentialType}
     */
    public Optional<List<PartnerCredentialType>> queryCredentialDefinitions(@NonNull String query) {
        Optional<List<PartnerCredentialType>> result = Optional.empty();

        if (StringUtils.isEmpty(url)) {
            log.error("The system property: 'bpa.ledger.browser' is not set");
            return result;
        }

        try {
            HttpUrl b = HttpUrl.parse(url + "/ledger/domain")
                    .newBuilder()
                    .addQueryParameter("query", AriesStringUtil.getLastSegment(query))
                    .addQueryParameter("type", "102") // 102 = credential definition
                    .build();
            Request request = new Request.Builder()
                    .url(b)
                    .build();
            try (Response response = ok.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    LedgerQueryResult md = mapper.readValue(body, LedgerQueryResult.class);
                    List<PartnerCredentialType> credDefIds = md.getResults()
                            .stream()
                            .map(DomainTransaction::getTxnMetadata)
                            .map(TxnMetadata::getTxnId)
                            .distinct()
                            .map(PartnerCredentialType::fromCredDefId)
                            .collect(Collectors.toList());
                    result = Optional.of(credDefIds);
                } else {
                    log.warn("Could not query ledger: {}, {}", response.code(), response.message());
                }
            }
        } catch (IOException e) {
            log.error("Ledger Explorer Call Failed", e);
        }
        return result;
    }
}
