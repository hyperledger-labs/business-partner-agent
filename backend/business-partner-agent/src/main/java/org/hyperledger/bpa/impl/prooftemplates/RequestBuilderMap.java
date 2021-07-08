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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class RequestBuilderMap<T> {

    private final Map<String, T> createdBuilders = new HashMap<>();
    private final Supplier<T> builderCreator;

    public RequestBuilderMap(Supplier<T> builderCreator) {
        this.builderCreator = builderCreator;
    }

    public T getNewBuilder(String name) {
        T result = builderCreator.get();
        createdBuilders.putIfAbsent(name, result);
        return createdBuilders.get(name);
    }

    public Stream<Map.Entry<String, T>> getBuilders() {
        return createdBuilders.entrySet().stream();
    }
}
