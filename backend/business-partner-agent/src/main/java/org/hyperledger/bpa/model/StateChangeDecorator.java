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
package org.hyperledger.bpa.model;

import io.micronaut.core.annotation.Nullable;
import lombok.*;
import org.hyperledger.bpa.impl.util.TimeUtil;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Decorator class for all database entities that deal with (connection, credential and proof) exchanges
 * to track state changes over time. As the BPA might receive events in particular order this class
 * also sets the top level state of the entity to the latest state which might not be the current state.
 *
 * @param <T> database entity
 * @param <S> state
 */
@Data
public abstract class StateChangeDecorator<T extends StateChangeDecorator<T, S>, S extends Enum<S>> {

    abstract public T setStateToTimestamp(StateToTimestamp<S> stateToTimestamp);

    abstract public StateToTimestamp<S> getStateToTimestamp();

    abstract public T setState(S state);

    /**
     * Records the timestamps of the different state changes, important in the
     * manual exchanges as they can take a while to happen.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class StateToTimestamp<S extends Enum<S>> {

        private Map<S, Instant> stateToTimestamp;

        public Map<S, Long> toApi() {
            return stateToTimestamp != null
                    ? stateToTimestamp.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, v -> v.getValue().toEpochMilli(), (v1, v2) -> v1,
                                    LinkedHashMap::new))
                    : Map.of();
        }

        public @Nullable Map.Entry<S, Instant> findLatestEntry() {
            return stateToTimestamp != null
                    ? stateToTimestamp.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparing(Instant::toEpochMilli)))
                            .min(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .orElse(null)
                    : null;
        }
    }

    public T pushStates(@NonNull S state) {
        return pushStates(state, Instant.now());
    }

    public T pushStates(@NonNull S state, @NonNull String ts) {
        return pushStates(state, TimeUtil.parseZonedTimestamp(ts));
    }

    @SuppressWarnings("unchecked")
    public T pushStates(@NonNull S state, @Nullable Instant ts) {
        if (ts == null) {
            ts = Instant.now();
        }
        if (getStateToTimestamp() == null || getStateToTimestamp().getStateToTimestamp() == null) {
            Map<S, Instant> states = new HashMap<>();
            states.put(state, ts);
            setStateToTimestamp(StateToTimestamp
                    .<S>builder()
                    .stateToTimestamp(states)
                    .build());
        } else {
            getStateToTimestamp().getStateToTimestamp().put(state, ts);
        }
        S latest = Objects.requireNonNull(getStateToTimestamp().findLatestEntry()).getKey();
        setState(Objects.requireNonNull(latest));
        return (T) this;
    }
}
