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
package org.hyperledger.bpa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.context.event.ApplicationEventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.StartupTasks;

import java.io.IOException;

@Getter
@Singleton
@NoArgsConstructor
@Slf4j
public class AcaPyConfig implements ApplicationEventListener<StartupTasks.AcaPyReady> {

    @JsonIgnore
    @Inject
    transient AriesClient ac;

    private Boolean autoRespondCredentialOffer = false;
    private Boolean autoRespondCredentialProposal = false;
    private Boolean autoRespondCredentialRequest = false;
    private Boolean autoRespondPresentationProposal = false;
    private Boolean autoRespondPresentationRequest = false;
    private Boolean autoVerifyPresentation = false;
    private Boolean autoStoreCredential = false;
    private Boolean autoAcceptInvites = false;
    private Boolean autoAcceptRequests = false;
    private Boolean autoRespondMessages = false;
    private Boolean preserveExchangeRecords = false;

    @Override
    public void onApplicationEvent(StartupTasks.AcaPyReady event) {
        try {
            ac.statusConfigTyped().ifPresent(c -> {
                autoAcceptInvites = c.isAutoAcceptInvites();
                autoAcceptRequests = c.isAutoAcceptRequests();
                autoRespondMessages = c.isAutoRespondMessages();
                autoRespondCredentialOffer = c.isAutoRespondCredentialOffer();
                autoRespondCredentialProposal = c.isAutoRespondCredentialProposal();
                autoRespondCredentialRequest = c.isAutoRespondCredentialRequest();
                autoRespondPresentationProposal = c.isAutoRespondPresentationProposal();
                autoRespondPresentationRequest = c.isAutoRespondPresentationRequest();
                autoStoreCredential = c.isAutoStoreCredential();
                autoVerifyPresentation = c.isAutoVerifyPresentation();
                preserveExchangeRecords = c.isPreserveExchangeRecords();
            });
        } catch (IOException e) {
            log.warn("aca-py not reachable");
        }
    }
}
