/*
 * Copyright (c) 2020 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.activity;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.jsonld.SignRequest;
import org.hyperledger.aries.api.jsonld.SignRequest.SignDocument.Options;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifyResponse;
import org.hyperledger.bpa.api.exception.NetworkException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
public class CryptoManager {

    @Inject
    @Setter
    AriesClient acaPy;

    @Inject
    Identity id;

    /**
     * Self sign a {@link VerifiablePresentation}
     *
     * @param inputVp {@link VerifiablePresentation}
     * @return the signed {@link VerifiablePresentation} including the proof
     */
    public Optional<VerifiablePresentation<VerifiableIndyCredential>> sign(
            @NonNull VerifiablePresentation<VerifiableIndyCredential> inputVp) {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> result = Optional.empty();
        try {
            final Optional<String> verkey = id.getVerkey();
            String myDid = id.getMyDid();
            String myVerkey = id.getMyKeyId(myDid);
            if (verkey.isPresent()) {
                SignRequest sr = SignRequest.from(
                        verkey.get(),
                        inputVp,
                        Options.builderWithDefaults()
                                .verificationMethod(myVerkey)
                                .build());
                result = acaPy.jsonldSign(
                        sr, VerifiablePresentation.INDY_CREDENTIAL_TYPE);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new NetworkException(e.getMessage());
        }
        return result;
    }

    /**
     * Verify a signed {@link VerifiablePresentation}
     *
     * @param verkey  the own or the partners verkey
     * @param inputVp {@link VerifiablePresentation}
     * @return verification success or failure
     */
    public Boolean verify(String verkey, VerifiablePresentation<VerifiableIndyCredential> inputVp) {
        Boolean result = Boolean.FALSE;
        try {
            Optional<VerifyResponse> state = acaPy.jsonldVerify(verkey, inputVp);
            if (state.isPresent()) {
                result = state.get().isValid();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new NetworkException(e.getMessage());
        }
        return result;
    }
}
