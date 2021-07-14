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
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Data
public class Pair<L, R> implements Map.Entry<L, R> {
    private final L left;

    private final R right;

    public Pair(Map.Entry<L, R> entry) {
        this.left = entry.getKey();
        this.right = entry.getValue();
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    public <T> Pair<L, T> withRight(T newRight) {
        return new Pair<>(this.left, newRight);
    }

    public <T> Pair<T, R> withLeft(T newLeft) {
        return new Pair<>(newLeft, this.right);
    }

    public <T> Pair<L, T> withRight(@NonNull Function<R, T> mapper) {
        return withRight(mapper.apply(right));
    }

    public <T> Pair<T, R> withLeft(@NonNull Function<L, T> mapper) {
        return withLeft(mapper.apply(left));
    }

    public static <L, R, T> Function<Pair<L, R>, Pair<L, T>> mapRight(@NonNull Function<R, T> mapper) {
        return p -> p.withRight(mapper.apply(p.right));
    }

    public static <L, R, T> Function<Pair<L, R>, Pair<T, R>> mapLeft(@NonNull Function<L, T> mapper) {
        return p -> p.withLeft(mapper.apply(p.left));
    }

    public static <L, R, T> Function<Pair<L, R>, Optional<Pair<T, R>>> optionalMapLeft(
            @NonNull Function<L, Optional<T>> mapper) {
        return p -> mapper.apply(p.left).map(p::withLeft);
    }

    public static <T, L, R> Function<T, Pair<L, R>> with(Function<T, L> getLeft, Function<T, R> getRight) {
        return o -> new Pair<>(getLeft.apply(o), getRight.apply(o));
    }

    public static <L, R, T> Function<Pair<L, R>, Stream<Pair<T, R>>> streamMapLeft(
            @NonNull Function<L, Stream<T>> mapper) {
        return p -> mapper.apply(p.left).map(p::withLeft);
    }

    public static <L, R, T> Function<Pair<L, R>, Optional<Pair<L, T>>> optionalMapRight(
            @NonNull Function<R, Optional<T>> mapper) {
        return p -> mapper.apply(p.right).map(p::withRight);
    }

    public static <L, R, T> Function<Pair<L, R>, Stream<Pair<L, T>>> streamMapRight(
            @NonNull Function<R, Stream<T>> mapper) {
        return p -> mapper.apply(p.right).map(p::withRight);
    }

    public static <L, R> Predicate<Pair<L, R>> filterLeft(Predicate<L> predicate) {
        return p -> predicate.test(p.left);
    }

    public static <L, R> Predicate<Pair<L, R>> filterRight(Predicate<R> predicate) {
        return p -> predicate.test(p.right);
    }

    public static <L, R, T> UnaryOperator<Pair<L, R>> lookUpAndSetOnRight(BiFunction<L, R, T> valueLookupFunction,
            Function<R, Function<T, R>> setter) {
        return lookUpAndSetOnRight(pair -> valueLookupFunction.apply(pair.getLeft(), pair.getRight()), setter);
    }

    public static <L, R, T> UnaryOperator<Pair<L, R>> lookUpAndSetOnRight(Function<Pair<L, R>, T> valueLookupFunction,
            Function<R, Function<T, R>> setter) {
        return target -> set(valueLookupFunction, setter.apply(target.getRight()).andThen(target::withRight))
                .apply(target);
    }

    public static <L, R, T> UnaryOperator<Pair<L, R>> lookUpAndSetOnLeft(BiFunction<L, R, T> valueLookupFunction,
            Function<L, Function<T, L>> setter) {
        return lookUpAndSetOnLeft(pair -> valueLookupFunction.apply(pair.getLeft(), pair.getRight()), setter);
    }

    public static <L, R, T> UnaryOperator<Pair<L, R>> lookUpAndSetOnLeft(Function<Pair<L, R>, T> valueLookupFunction,
            Function<L, Function<T, L>> setter) {
        return target -> set(valueLookupFunction, setter.apply(target.getLeft()).andThen(target::withLeft))
                .apply(target);
    }

    private static <T, L, R, NL, NR> Function<Pair<L, R>, Pair<NL, NR>> set(Function<Pair<L, R>, T> valueSupplier,
            Function<T, Pair<NL, NR>> valueSink) {
        return pair -> valueSupplier.andThen(valueSink).apply(pair);
    }

}
