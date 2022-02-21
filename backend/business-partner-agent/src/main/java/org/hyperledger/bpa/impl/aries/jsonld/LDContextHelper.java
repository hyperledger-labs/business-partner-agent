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
package org.hyperledger.bpa.impl.aries.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.activity.DocumentValidator;
import org.hyperledger.bpa.impl.aries.wallet.Identity;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.BPARestrictions;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.BPARestrictionsRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
public class LDContextHelper {

    @Inject
    Identity identity;

    @Inject
    Converter conv;

    @Inject
    DocumentValidator documentValidator;

    @Inject
    BPARestrictionsRepository trustedIssuer;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public static String findSchemaId(@Nullable V20CredExRecordByFormat.LdProof ldProof) {
        if (ldProof == null) {
            return null;
        }
        // TODO this does not consider all use cases like complex schemas
        List<Object> context = ldProof.getCredential().getContext();
        List<Object> contextCopy = new ArrayList<>(context);
        contextCopy.removeAll(CredentialType.JSON_LD.getContext());
        return (String) contextCopy.get(0);
    }

    public V2CredentialExchangeFree.V20CredFilter buildVC(
            @NonNull BPASchema bpaSchema, @NonNull JsonNode document, @NonNull Boolean issuer) {
        return buildVC(bpaSchema, conv.toStringMap(document), issuer);
    }

    public V2CredentialExchangeFree.V20CredFilter buildVC(
            @NonNull BPASchema bpaSchema, @NonNull Map<String, String> document, @NonNull Boolean issuer) {
        documentValidator.validateAttributesAgainstLDSchema(bpaSchema, document);
        return V2CredentialExchangeFree.V20CredFilter.builder()
                .ldProof(V2CredentialExchangeFree.LDProofVCDetail.builder()
                        .credential(VerifiableCredential.builder()
                                .context(List.of(CredentialType.JSON_LD.getContext().get(0), bpaSchema.getSchemaId()))
                                .credentialSubject(GsonConfig.defaultConfig().toJsonTree(document).getAsJsonObject())
                                .issuanceDate(TimeUtil.toISOInstantTruncated(Instant.now()))
                                .issuer(issuer ? identity.getDidKey() : findIssuerDidOrFallback(bpaSchema))
                                .type(List.of(CredentialType.JSON_LD.getType().get(0),
                                        Objects.requireNonNull(bpaSchema.getLdType())))
                                .build())
                        .options(V2CredentialExchangeFree.LDProofVCDetailOptions.builder()
                                .proofType(V2CredentialExchangeFree.ProofType.BbsBlsSignature2020)
                                .build())
                        .build())
                .build();
    }

    /**
     * Returns the first configured did:key from the trusted issuer list, or falls
     * back to the holders local did:key which will then be replaced by the issuer
     * with the issuers key. Note: This will work, but in this case the issuer is
     * not verified.
     * 
     * @param bpaSchema {@link BPASchema}
     * @return did:key
     */
    private String findIssuerDidOrFallback(@NonNull BPASchema bpaSchema) {
        return trustedIssuer.findBySchema(bpaSchema)
                .stream()
                .map(BPARestrictions::getIssuerDid)
                .findFirst()
                .orElse(identity.getDidKey());
    }
}
