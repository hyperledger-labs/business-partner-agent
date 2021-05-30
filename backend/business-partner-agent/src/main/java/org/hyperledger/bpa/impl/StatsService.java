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

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.stats.BPAStats;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.MyDocumentRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class StatsService {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    Identity identity;

    public BPAStats collectStats() {
        return BPAStats
                .builder()
                .did(identity.getMyDid())
                .profile(docRepo
                        .existsByTypeEqualsAndIsPublicTrue(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL))
                .partners(partnerRepo.count())
                .credentials(credRepo.countByStateEquals(CredentialExchangeState.CREDENTIAL_ACKED))
                .build();
    }
}
