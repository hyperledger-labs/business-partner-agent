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
package org.hyperledger.bpa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.context.event.ApplicationEventListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.StartupTasks;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Getter
@Singleton
@NoArgsConstructor
@Slf4j
public class AcaPyConfig implements ApplicationEventListener<StartupTasks.AcaPyReady> {

    @JsonIgnore
    @Inject
    transient AriesClient ac;

    private Boolean autoRespondCredentialOffer;
    private Boolean autoRespondCredentialProposal;
    private Boolean autoRespondCredentialRequest;
    private Boolean autoRespondPresentationProposal;
    private Boolean autoRespondPresentationRequest;
    private Boolean autoVerifyPresentation;
    private Boolean autoStoreCredential;
    private Boolean autoAcceptInvites;
    private Boolean autoAcceptRequests;
    private Boolean autoRespondMessages;

    @Override
    public void onApplicationEvent(StartupTasks.AcaPyReady event) {
        try {
            ac.statusConfig().ifPresent(c -> {
                autoAcceptInvites = c.getUnwrapped("debug.auto_accept_invites", Boolean.class);
                autoAcceptRequests = c.getUnwrapped("debug.auto_accept_requests", Boolean.class);
                autoRespondMessages = c.getUnwrapped("debug.auto_respond_messages", Boolean.class);
                autoRespondCredentialOffer = c.getUnwrapped("debug.auto_respond_credential_offer", Boolean.class);
                autoRespondCredentialProposal = c.getUnwrapped("debug.auto_respond_credential_proposal",
                        Boolean.class);
                autoRespondCredentialRequest = c.getUnwrapped("debug.auto_respond_credential_request",
                        Boolean.class);
                autoRespondPresentationProposal = c.getUnwrapped("debug.auto_respond_presentation_proposal",
                        Boolean.class);
                autoRespondPresentationRequest = c.getUnwrapped("debug.auto_respond_presentation_request",
                        Boolean.class);
                autoStoreCredential = c.getUnwrapped("debug.auto_store_credential", Boolean.class);
                autoVerifyPresentation = c.getUnwrapped("debug.auto_verify_presentation", Boolean.class);
            });
        } catch (IOException e) {
            log.warn("aca-py not reachable");
        }
    }
}
