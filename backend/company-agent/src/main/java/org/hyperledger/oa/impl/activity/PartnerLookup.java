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
package org.hyperledger.oa.impl.activity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.DidDocAPI.PublicKey;
import org.hyperledger.oa.api.DidDocAPI.Service;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.URClient;
import org.hyperledger.oa.impl.util.Converter;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PartnerLookup {

    @Inject
    private Converter converter;

    @Inject
    private URClient ur;

    @Inject
    private CryptoManager crypto;

    @Cacheable(cacheNames = { "partner-lookup-cache" })
    public PartnerAPI lookupPartner(@NonNull String did) {
        Optional<DidDocAPI> didDocument = ur.getDidDocument(did);
        if (didDocument.isPresent()) {
            Optional<Map<String, String>> services = filterServices(didDocument.get());
            if (services.isPresent()) {
                PartnerAPI partner = new PartnerAPI();
                if (services.get().containsKey(EndpointType.Profile.getLedgerName())) {
                    partner = lookupPartner(
                            services.get().get(EndpointType.Profile.getLedgerName()),
                            didDocument.get().getPublicKey());
                }
                if (services.get().containsKey(EndpointType.Endpoint.getLedgerName())) {
                    partner.setAriesSupport(Boolean.TRUE);
                } else {
                    partner.setAriesSupport(Boolean.FALSE);
                }
                return partner;
            }
            throw new PartnerException("Could not resolve profile and/or aries endpoint from did document");
        }
        throw new PartnerException("Could not retrieve did document from universal resolver");
    }

    static Optional<Map<String, String>> filterServices(@NonNull DidDocAPI doc) {
        Map<String, String> result = null;
        if (doc.getService() != null) {
            result = doc.getService().stream()
                    .filter(s -> EndpointType.Profile.getLedgerName().equals(s.getType())
                            || EndpointType.Endpoint.getLedgerName().equals(s.getType()))
                    .collect(Collectors.toMap(Service::getType, Service::getServiceEndpoint));
        }
        return Optional.ofNullable(result);
    }

    PartnerAPI lookupPartner(@NonNull String endpoint, List<PublicKey> publicKey) {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> profile = ur.getPublicProfile(endpoint);
        if (profile.isPresent()) {
            String verificationMethod = profile.get().getProof() != null
                    ? profile.get().getProof().getVerificationMethod()
                    : "";
            Optional<String> pk = matchKey(verificationMethod, publicKey);
            final PartnerAPI partner = converter.toAPIObject(profile.get());
            partner.setVerifiablePresentation(profile.get());
            if (pk.isPresent()) {
                final Boolean valid = crypto.verify(pk.get(), profile.get());
                partner.setValid(valid);
            }
            return partner;
        }
        throw new PartnerException("Could not retrieve public profile from endpoint: " + endpoint);
    }

    /**
     * Tries to find the the public key in the did document that matches the proofs
     * verification method
     *
     * @param verificationMethod the proof verification method
     * @param publicKey          list of {@link PublicKey} from the did document
     * @return matching public key in Base58
     */
    static Optional<String> matchKey(String verificationMethod, List<PublicKey> publicKeys) {
        Optional<PublicKey> key = Optional.empty();
        String result = null;
        if (StringUtils.isNotEmpty(verificationMethod) && CollectionUtils.isNotEmpty(publicKeys)) {
            key = publicKeys.stream().filter(k -> verificationMethod.equals(k.getId())).findFirst();
        }
        if (key.isEmpty()) { // falling back to key list from did doc
            key = resolvePublicKey(publicKeys);
        }
        if (key.isPresent()) {
            result = key.get().getPublicKeyBase58();
        }
        return Optional.ofNullable(result);
    }

    /**
     * Fallback method to find at least one matching default key in the did document
     *
     * @param publicKey list of {@link PublicKey} from the did document
     * @return matching {@link PublicKey}
     */
    static Optional<PublicKey> resolvePublicKey(List<PublicKey> publicKey) {
        Optional<PublicKey> key = Optional.empty();
        if (CollectionUtils.isEmpty(publicKey)) {
            log.warn("No public key found in the did document");
        } else {
            Optional<PublicKey> vk = publicKey.stream()
                    .filter(k -> ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE.equals(k.getType())).findFirst();
            if (vk.isPresent()) {
                key = Optional.of(vk.get());
            } else {
                log.warn("Expected at least one " + ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE
                        + " in the did document, but found none");
            }
        }
        return key;
    }
}
