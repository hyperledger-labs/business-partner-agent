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

package org.hyperledger.bpa.impl.prooftemplates;

import io.micronaut.core.annotation.NonNull;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ProofTemplateRequestBuilder<Predicate, Attribute, Restrictions> {

    private final Map<String, Predicate> predicates = new HashMap<>();
    private final Map<String, Attribute> attributes = new HashMap<>();
    private final Map<String, Restrictions> restrictions = new HashMap<>();
    private final Function<String, Predicate> predicateCreator;
    private final Function<String, Attribute> attributeCreator;
    private final Supplier<Restrictions> restrictionCreator;

    public ProofTemplateRequestBuilder(
            @NonNull Function<String, Predicate> predicateCreator,
            @NonNull Function<String, Attribute> attributeCreator,
            @NonNull Supplier<Restrictions> restrictionCreator) {
        this.predicateCreator = predicateCreator;
        this.attributeCreator = attributeCreator;
        this.restrictionCreator = restrictionCreator;
    }

    public void addPredicate(String name) {
        predicates.computeIfAbsent(name, predicateCreator);
    }

    public void addAttribute(String name) {
        attributes.computeIfAbsent(name, attributeCreator);
    }

    public void onAttribute(String name, @NonNull UnaryOperator<Attribute> modifier) {
        attributes.computeIfPresent(name, (key, attr) -> modifier.apply(attr));
    }

    public void onPredicate(String name, @NonNull UnaryOperator<Predicate> modifier) {
        predicates.computeIfPresent(name, (key, attr) -> modifier.apply(attr));
    }

    public void putRestriction(String name, @NonNull UnaryOperator<Restrictions> modifier) {
        restrictions.compute(name,
                (key, oldAttr) -> modifier.apply(Optional.ofNullable(oldAttr).orElseGet(restrictionCreator)));
    }

    public Stream<Predicate> predicateStream() {
        return predicates.entrySet()
                .stream()
                .map(entry -> restrictionsFor(entry.getKey())
                        .map(r -> this.joinPredicateWithRestrictions(entry.getValue(), r))
                        .orElseGet(entry::getValue));
    }

    public Stream<Attribute> attributeStream() {
        return attributes.entrySet()
                .stream()
                .map(entry -> restrictionsFor(entry.getKey())
                        .map(r -> this.joinAttributeWithRestrictions(entry.getValue(), r))
                        .orElseGet(entry::getValue));
    }

    public Optional<Restrictions> restrictionsFor(@NotEmpty String name) {
        return Optional.ofNullable(restrictions.get(name));
    }

    public Attribute joinAttributeWithRestrictions(Attribute attribute, Restrictions restrictions) {
        return attribute;
    }

    public Predicate joinPredicateWithRestrictions(Predicate predicate, Restrictions restrictions) {
        return predicate;
    }
}
