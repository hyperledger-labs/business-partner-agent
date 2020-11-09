/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.client.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LedgerQueryResult {

    @JsonAlias("first_index")
    private Integer firstIndex;

    @JsonAlias("last_index")
    private Integer lastIndex;

    private Integer page;

    @JsonAlias("page_size")
    private Integer pageSize;

    private List<DomainTransaction> results;

    private Integer total;

    @Data
    @NoArgsConstructor
    public static final class DomainTransaction {
        private Integer ledgerSize;
        private TxnMetadata txnMetadata;

        @Data
        @NoArgsConstructor
        public static final class TxnMetadata {
            private Integer seqNo;
            private String txnId;
            private Long txnTime;
        }
    }
}
