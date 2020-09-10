/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.aries.api.credential.CredentialAttributes;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.credential.CredentialProposalRequest;
import org.hyperledger.aries.api.credential.CredentialProposalRequest.CredentialPreview;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.AriesCredential;
import org.hyperledger.oa.api.aries.AriesCredential.AriesCredentialBuilder;
import org.hyperledger.oa.api.aries.BankAccount;
import org.hyperledger.oa.api.exception.NetworkException;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.LedgerClient;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.model.MyDocument;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
public class AriesCredentialManager {

    @Inject
    private AriesClient ac;

    @Inject
    private PartnerRepository partnerRepo;

    @Inject
    private MyDocumentRepository docRepo;

    @Inject
    private MyCredentialRepository credRepo;

    @Inject
    private VPManager vpMgmt;

    @Inject
    SchemaService schemaService;

    @Inject
    private Converter conv;

    @Inject
    private LedgerClient ledger;

    public Optional<List<PartnerCredentialType>> getPartnerCredDefs(@NonNull UUID partnerId) {
        Optional<List<PartnerCredentialType>> result = Optional.empty();
        final Optional<Partner> p = partnerRepo.findById(partnerId);
        if (p.isPresent() && StringUtils.isNotEmpty(p.get().getDid())) {
            result = ledger.getCredentialDefinitionIdsForDid(AriesStringUtil.didGetLastSegment(p.get().getDid()));
        }
        return result;
    }

    private Optional<String> findBACredentialDefinitionId(@NonNull UUID partnerId, @NonNull Integer seqNo) {
        Optional<String> result = Optional.empty();

        final Optional<List<PartnerCredentialType>> pct = getPartnerCredDefs(partnerId);
        if (pct.isPresent()) {
            final List<PartnerCredentialType> baCreds = pct.get().stream()
                    .filter(cred -> AriesStringUtil.credDefIdGetSquenceNo(
                            cred.getCredentialDefinitionId()).equals(seqNo.toString()))
                    .collect(Collectors.toList());
            if (baCreds.size() > 0) {
                result = Optional.of(baCreds.get(baCreds.size() - 1).getCredentialDefinitionId());
            }
        }
        return result;
    }

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId) {
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (dbPartner.isPresent()) {
            final Optional<MyDocument> dbDoc = docRepo.findById(myDocId);
            if (dbDoc.isPresent()) {
                if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(dbDoc.get().getType())) {
                    final BankAccount bankAccount = conv.fromMap(dbDoc.get().getDocument(), BankAccount.class);
                    try {
                        final org.hyperledger.oa.model.Schema s = schemaService
                                .getSchemaFor(CredentialType.BANK_ACCOUNT_CREDENTIAL);
                        final Optional<String> baCredDefId = findBACredentialDefinitionId(partnerId, s.getSeqNo());
                        if (baCredDefId.isPresent()) {
                            ac.issueCredentialSendProposal(
                                    CredentialProposalRequest
                                            .builder()
                                            .connectionId(dbPartner.get().getConnectionId())
                                            .schemaId(s.getSchemaId())
                                            .credentialProposal(
                                                    new CredentialPreview(
                                                            CredentialAttributes.from(bankAccount)))
                                            .credentialDefinitionId(baCredDefId.get())
                                            .build());
                        } else
                            throw new PartnerException("Found no matching credential definition id. "
                                    + "Partner can not issue bank account credentials");
                    } catch (IOException e) {
                        throw new NetworkException("No aries connection", e);
                    }
                } else {
                    throw new PartnerException("Currently only documents of type BANK_ACCOUNT are supported");
                }
            } else {
                throw new PartnerException("No document found for id: " + myDocId.toString());
            }
        } else {
            throw new PartnerException("No partner found for id: " + partnerId.toString());
        }
    }

    // all other state changes that are not store or acked
    public void handleCredentialEvent(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    credRepo.updateState(cred.getId(), credEx.getState());
                }, () -> {
                    MyCredential dbCred = MyCredential
                            .builder()
                            .isPublic(Boolean.FALSE)
                            .connectionId(credEx.getConnectionId())
                            .state(credEx.getState())
                            .threadId(credEx.getThreadId())
                            .build();
                    credRepo.save(dbCred);
                });
    }

    // credential signed, but not in wallet yet
    public void handleStroreCredential(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    try {
                        credRepo.updateState(cred.getId(), credEx.getState());
                        // TODO should not be necessary with --auto-store-credential set
                        ac.issueCredentialRecordsStore(credEx.getCredentialExchangeId());
                    } catch (IOException e) {
                        log.error("aca-py not reachable", e);
                    }
                }, () -> log.error("Received store credential event without matching therad id"));
    }

    // credential, signed and stored in wallet
    public void handleCredentialAcked(CredentialExchange credEx) {
        credRepo.findByThreadId(credEx.getThreadId())
                .ifPresentOrElse(cred -> {
                    cred
                            .setReferent(credEx.getCredential().getReferent())
                            .setCredential(conv.toMap(credEx.getCredential()))
                            .setType(CredentialType.fromSchemaId(credEx.getSchemaId()))
                            .setState(credEx.getState())
                            .setIssuedAt(Instant.now());
                    credRepo.update(cred);
                }, () -> log.error("Received credential without matching thread id, credential is not stored."));
    }

    @SuppressWarnings("boxing")
    public Optional<MyCredential> toggleVisibility(UUID id) {
        final Optional<MyCredential> cred = credRepo.findById(id);
        if (cred.isPresent()) {
            credRepo.updateIsPublic(id, !cred.get().getIsPublic());
            vpMgmt.recreateVerifiablePresentation();
        }
        return cred;
    }

    // credential CRUD operations

    public List<AriesCredential> listCredentials() {
        List<AriesCredential> result = new ArrayList<>();
        credRepo.findAll().forEach(c -> result.add(AriesCredential.fromMyCredential(c).build()));
        return result;
    }

    public Optional<AriesCredential> getAriesCredentialById(@NonNull UUID id) {
        final Optional<MyCredential> dbCred = credRepo.findById(id);
        if (dbCred.isPresent()) {
            final AriesCredentialBuilder myCred = AriesCredential.fromMyCredential(dbCred.get());
            final Credential ariesCred = conv.fromMap(dbCred.get().getCredential(), Credential.class);
            myCred
                    .schemaId(ariesCred.getSchemaId())
                    .issuer(ariesCred.getCredentialDefinitionId())
                    .credentialData(ariesCred.getAttrs());
            return Optional.of(myCred.build());
        }
        return Optional.empty();
    }

    public Optional<AriesCredential> updateCredentialById(@NonNull UUID id, @NonNull String label) {
        final Optional<AriesCredential> cred = getAriesCredentialById(id);
        if (cred.isPresent()) {
            credRepo.updateLabel(id, label);
            cred.get().setLabel(label);
        }
        return cred;
    }

    public void deleteCredentialById(@NonNull UUID id) {
        credRepo.findById(id).ifPresent(c -> {
            try {
                if (c.getReferent() != null) {
                    ac.credentialRemove(c.getReferent());
                }
            } catch (IOException e) {
                throw new NetworkException("aca-py not reachable", e);
            }
            credRepo.deleteById(id);
        });
    }
}
