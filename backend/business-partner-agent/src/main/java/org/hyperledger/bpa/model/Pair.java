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

import lombok.Data;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
public class Pair<L, R> {
    private final L left;

    private final R right;

    public <T> Pair<L, T> withRight(T newRight) {
        return new Pair<>(this.left, newRight);
    }

    public <T> Pair<T, R> withLeft(T newLeft) {
        return new Pair<>(newLeft, this.right);
    }

    public static <L, R, T> Function<Pair<L, R>, Pair<T, R>> mapLeft(@NonNull Function<L, T> mapper) {
        return p -> p.withLeft(mapper.apply(p.left));
    }

    public static <L, R, T> Function<Pair<L, R>, Pair<L, T>> mapRight(@NonNull Function<R, T> mapper) {
        return p -> p.withRight(mapper.apply(p.right));
    }

    public static <L, R, T> Function<Pair<L, R>, Optional<Pair<T, R>>> flatMapLeft(
            @NonNull Function<L, Optional<T>> mapper) {
        return p -> mapper.apply(p.left).map(p::withLeft);
    }

    public static <L, R, T> Function<Pair<L, R>, Optional<Pair<L, T>>> flatMapRight(
            @NonNull Function<R, Optional<T>> mapper) {
        return p -> mapper.apply(p.right).map(p::withRight);
    }

    public static <L, R> Predicate<Pair<L, R>> filterLeft(Predicate<L> predicate) {
        return p -> predicate.test(p.left);
    }

    public static <L, R> Predicate<Pair<L, R>> filterRight(Predicate<R> predicate) {
        return p -> predicate.test(p.right);
    }
}
