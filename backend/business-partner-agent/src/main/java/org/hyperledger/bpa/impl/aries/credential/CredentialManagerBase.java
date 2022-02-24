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
package org.hyperledger.bpa.impl.aries.credential;

import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.hyperledger.acy_py.generated.model.V10CredentialProblemReportRequest;
import org.hyperledger.acy_py.generated.model.V20CredIssueProblemReportRequest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wraps all credential exchange logic that is common for indy and json-ld
 * credentials, and both holder and issuer flows.
 */
@Singleton
public abstract class CredentialManagerBase {

    @Inject
    AriesClient ac;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    @Inject
    IssuerCredExRepository issuerCredExRepo;

    @Inject
    Converter conv;

    /**
     * If there is a problem during the credential exchange and aca-py is started
     * without the option to preserve exchange records, the record is deleted
     * immediately. Hence, we need to check if the record exists in both systems.
     * 
     * @param id credential exchange id
     * @return {@link BPACredentialExchange}
     */
    public BPACredentialExchange getCredentialExchange(@NonNull UUID id) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        try {
            if (ExchangeVersion.V1.equals(credEx.getExchangeVersion())) {
                ac.issueCredentialRecordsGetById(credEx.getCredentialExchangeId());
            } else {
                ac.issueCredentialV2RecordsGetById(credEx.getCredentialExchangeId());
            }
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        } catch (AriesException e) {
            if (e.getCode() == 404) {
                credEx.pushStates(CredentialExchangeState.PROBLEM, Instant.now());
                issuerCredExRepo.updateAfterEventNoRevocationInfo(credEx.getId(), credEx.getState(),
                        credEx.getStateToTimestamp(), msg.getMessage("api.credential.no.match"));
                throw new EntityNotFoundException();
            }
            throw e;
        }
        return credEx;
    }

    public List<CredEx> listCredentialExchanges(@Nullable CredentialExchangeRole role, @Nullable UUID partnerId) {
        List<BPACredentialExchange> exchanges = issuerCredExRepo.listOrderByUpdatedAtDesc();
        // now, lets get credentials...
        return exchanges.stream()
                .filter(x -> {
                    if (role != null) {
                        return role.equals(x.getRole());
                    }
                    return true;
                })
                .filter(x -> x.getPartner() != null)
                .filter(x -> {
                    if (partnerId != null) {
                        return x.getPartner().getId().equals(partnerId);
                    }
                    return true;
                })
                .map(ex -> CredEx.from(ex, conv.toAPIObject(ex.getPartner())))
                .collect(Collectors.toList());
    }

    public CredEx findCredentialExchangeById(@NonNull UUID id) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        return CredEx.from(credEx, conv.toAPIObject(Objects.requireNonNull(credEx.getPartner())));
    }

    /**
     * The only way to stop or decline a credential exchange is for any side (issuer
     * or holder) to send a problem report. If a problem report is sent aca-py will
     * set the state of the exchange to null and hence it becomes unusable and con
     * not be restarted again.
     *
     * @param credEx  {@link BPACredentialExchange}
     * @param message to sent to the other party
     */
    public void declineCredentialExchange(@NonNull BPACredentialExchange credEx, @Nullable String message) {
        try {
            if (ExchangeVersion.V1.equals(credEx.getExchangeVersion())) {
                ac.issueCredentialRecordsProblemReport(credEx.getCredentialExchangeId(),
                        V10CredentialProblemReportRequest.builder().description(message).build());
            } else {
                ac.issueCredentialV2RecordsProblemReport(credEx.getCredentialExchangeId(),
                        V20CredIssueProblemReportRequest.builder().description(message).build());
            }
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        } catch (AriesException e) {
            if (e.getCode() == 404) {
                throw new EntityNotFoundException();
            }
            throw e;
        }
    }
}
