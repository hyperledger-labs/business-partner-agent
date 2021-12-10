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
package org.hyperledger.bpa.impl;

import com.google.gson.Gson;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.endorser.EndorseTransactionRecord;
import org.hyperledger.aries.api.endorser.TransactionState;
import org.hyperledger.aries.api.endorser.TransactionType;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.aries.TransactionRole;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.endorser.Transaction;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPATransaction;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPATransactionRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TransactionManager {

    @Inject
    AriesClient ac;

    @Inject
    BPATransactionRepository transactionRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    Converter conv;

    @Inject
    RuntimeConfig config;

    public void handleEndorserTransactionEvent(@NonNull EndorseTransactionRecord transaction) {
        // first check if we already have this transaction in our DB
        Optional<BPATransaction> result = Optional.empty();
        result = transactionRepo.findByTransactionId(transaction.getTransactionId());

        result.ifPresentOrElse(
            txn -> {
                // if yes the update the state
                transactionRepo.updateState(txn.getId(), transaction.getState());
            },
            () -> {
                // if no then add it
                Optional<Partner> partner = Optional.empty();
                partner = partnerRepo.findByConnectionId(transaction.getConnectionId());
                partner.ifPresentOrElse(
                    p -> {
                        BPATransaction txn = BPATransaction
                            .builder()
                            .createdAt(TimeUtil.fromISOInstant(transaction.getCreatedAt()))
                            .partner(p)
                            .role(config.isAuthor() ? TransactionRole.AUTHOR : TransactionRole.ENDORSER)
                            .threadId(transaction.getThreadId())
                            .transactionId(transaction.getTransactionId())
                            .state(transaction.getState())
                            .endorserWriteTransaction(transaction.getEndorserWriteTxn() != null ? transaction.getEndorserWriteTxn() : Boolean.FALSE)
                            .updatedAt(TimeUtil.fromISOInstant(transaction.getUpdatedAt()))
                            .build();
                        transactionRepo.save(txn);
                    },
                    () -> {
                        // TODO raise exception if partner is not found

                    }
                );
            }
        );

    }

    public void listTransactions() {
    }

    public void readTransaction(@NonNull UUID transactionId) {
    }
}
