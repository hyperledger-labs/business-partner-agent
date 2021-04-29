/*
 *
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.hyperledger.bpa.api.exception;

import java.io.Serial;

public class SchemaException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -7238940733903128511L;

    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable t) {
        super(message, t);
    }
}
