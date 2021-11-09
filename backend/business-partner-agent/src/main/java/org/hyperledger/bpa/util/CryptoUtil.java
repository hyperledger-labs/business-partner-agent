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
package org.hyperledger.bpa.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.config.GsonConfig;
import org.springframework.security.crypto.codec.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class CryptoUtil {

    public static boolean hashCompare(@NonNull Object base, @NonNull Object other) {
        return hashCompare(GsonConfig.defaultConfig().toJson(base), GsonConfig.defaultConfig().toJson(other));
    }

    public static boolean hashCompare(@NonNull String base, @NonNull String other) {
        String b = toSHA256Hex(base);
        String o = toSHA256Hex(other);
        if (b == null || o == null) {
            return false;
        }
        return b.equals(o);
    }

    private static String toSHA256Hex(@NonNull String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final byte[] hashBytes = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            return String.valueOf(Hex.encode(hashBytes));
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA3-256 not available");
        }
        return null;
    }
}
