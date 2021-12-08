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
package org.hyperledger.bpa.impl.aries;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialFreeOffer;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialFreeOfferHelper;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.controller.IssuerController;
import org.hyperledger.bpa.controller.api.issuer.IssueConnectionLessRequest;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.repository.IssuerCredExRepository;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class ConnectionLessCredential {

    @Value("${bpa.scheme}")
    String scheme;

    @Value("${bpa.host}")
    String host;

    @Inject
    AriesClient ac;

    @Inject
    Converter conv;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    private final V1CredentialFreeOfferHelper h;

    public ConnectionLessCredential(AriesClient ac) {
        this.ac = ac;
        this.h = new V1CredentialFreeOfferHelper(ac);
    }

    public URI issueConnectionLess(@NonNull IssueConnectionLessRequest request) {
        // TODO nice exception and attribute check
        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId()).orElseThrow();
        Map<String, String> document = conv.toStringMap(request.getDocument());
        V1CredentialFreeOffer freeOffer = h.buildFreeOffer(dbCredDef.getCredentialDefinitionId(), document);
        persistCredentialExchange(freeOffer, dbCredDef);
        removeTempConnectionRecord(freeOffer.getConnectionId());
        return createURI(IssuerController.ISSUER_CONTROLLER_BASE_URL + "/connection-less/"
                + freeOffer.getCredentialExchangeId());
    }

    public URI handleConnectionLess(@NonNull UUID credentialExchangeId) {
        BPACredentialExchange ex = credExRepo.findByCredentialExchangeId(credentialExchangeId.toString())
                .orElseThrow(EntityNotFoundException::new);
        if (ex.getFreeCredentialOffer() == null) {
            // TODO nice exception
            throw new IllegalStateException();
        }
        return createURI("?m_d=" + h.toBase64(ex.getFreeCredentialOffer()));
    }

    private void persistCredentialExchange(
            @NonNull V1CredentialFreeOffer offer, @NonNull BPACredentialDefinition dbCredDef) {
        BPACredentialExchange.BPACredentialExchangeBuilder b = BPACredentialExchange
                .builder()
                .schema(dbCredDef.getSchema())
                .credDef(dbCredDef)
                .role(CredentialExchangeRole.ISSUER)
                .state(CredentialExchangeState.OFFER_SENT)
                .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                .freeCredentialOffer(offer)
                .credentialExchangeId(offer.getCredentialExchangeId())
                .threadId(offer.getThreadId())
                .credentialOffer(offer.getCredentialPreview() != null
                        ? offer.getCredentialPreview()
                        : null);
        credExRepo.save(b.build());
    }

    private void removeTempConnectionRecord(@NonNull String connectionId) {
        try {
            ac.connectionsRemove(connectionId);
        } catch (IOException | AriesException e) {
            log.warn("Could not delete aries connection record.", e);
        }
    }

    private URI createURI(String path) {
        return URI.create(scheme + "://" + host + path);
    }
}
