/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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
package org.hyperledger.oa.impl;

import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.wallet.WalletDidResponse;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.client.CachingAriesClient;
import org.hyperledger.oa.controller.api.stats.BPAStats;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.MyDocumentRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
public class StatsService {

    @Value("${oagent.did.prefix}")
    String didPrefix;

    @Inject
    CachingAriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    MyDocumentRepository docRepo;

    public BPAStats collectStats() {
        return BPAStats
                .builder()
                .did(resolveMyPublicDid())
                .profile(docRepo
                        .existsByTypeEqualsAndIsPublicTrue(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL))
                .partners(partnerRepo.count())
                .credentials(credRepo.countByStateEquals("credential_acked"))
                .build();
    }

    private String resolveMyPublicDid() {
        String did = null;
        try {
            final Optional<WalletDidResponse> pubDid = ac.walletDidPublic();
            if (pubDid.isPresent()) {
                did = didPrefix + pubDid.get().getDid();
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return did;
    }

}
