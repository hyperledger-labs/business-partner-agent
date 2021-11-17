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
package org.hyperledger.bpa.impl.util;

import io.micronaut.core.util.StringUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

@Slf4j
public class TimeUtil {

    private static final DateTimeFormatter ISO_INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static String toISOInstant(@NonNull Instant instant) {
        return ISO_INSTANT_FORMATTER.format(instant);
    }

    public static String toISOInstantTruncated(@NonNull Instant instant) {
        return ISO_INSTANT_FORMATTER.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    public static Instant fromISOInstant(String ts) {
        if (StringUtils.isEmpty(ts)) {
            return Instant.ofEpochMilli(0);
        }
        final TemporalAccessor parsed = ISO_INSTANT_FORMATTER.parse(ts);
        return Instant.from(parsed);
    }
}
