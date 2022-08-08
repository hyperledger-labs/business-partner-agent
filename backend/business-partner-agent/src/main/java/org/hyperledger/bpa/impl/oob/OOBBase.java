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
package org.hyperledger.bpa.impl.oob;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.connection.ConnectionManager;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.Tag;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Singleton
public abstract class OOBBase {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Value("${bpa.scheme}")
    String scheme;

    @Value("${bpa.host}")
    String host;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    /**
     * Step 2: Return the base64 encoded invitation plus attachment
     *
     * @param invMessageId invitation message id
     * @return base64 encoded invitation URL
     */
    public String handleConnectionLess(@NonNull UUID invMessageId) {
        log.debug("Handling connectionless credential request: {}", invMessageId);
        Partner ex = partnerRepo.findByInvitationMsgId(invMessageId.toString())
                .orElseThrow(EntityNotFoundException::new);
        if (ex.getInvitationRecord() == null) {
            throw new EntityNotFoundException(ms.getMessage("api.issuer.connectionless.invitation.not.found",
                    Map.of("id", invMessageId)));
        }
        // getInvitationUrl() has an encoding issue
        byte[] envelopeBase64 = Base64.getEncoder().encode(
                GsonConfig.defaultNoEscaping().toJson(
                        ex.getInvitationRecord().getInvitation()).getBytes(StandardCharsets.UTF_8));
        return "didcomm://" + host + "?oob=" + new String(envelopeBase64, StandardCharsets.UTF_8);
    }

    Partner persistPartner(InvitationRecord r, String alias, Boolean trustPing, List<Tag> tag) {
        return partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .invitationMsgId(r.getInviMsgId())
                .did(didPrefix + ConnectionManager.UNKNOWN_DID)
                .state(ConnectionState.INVITATION)
                .pushStateChange(ConnectionState.INVITATION, Instant.now())
                .invitationRecord(r)
                .incoming(Boolean.TRUE)
                .alias(StringUtils.trimToNull(alias))
                .tags(tag != null ? new HashSet<>(tag) : null)
                .trustPing(trustPing != null ? trustPing : Boolean.FALSE)
                .build());
    }

    URI createURI(String path) {
        return URI.create(scheme + "://" + host + path);
    }
}
