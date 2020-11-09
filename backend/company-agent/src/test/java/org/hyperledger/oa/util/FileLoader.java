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
package org.hyperledger.oa.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
public class FileLoader {

    public String load(String filename) {
        String result = "";
        String fn;

        if (!filename.contains(".")) {
            fn = filename + ".json";
        } else {
            fn = filename;
        }

        InputStream is = getClass().getClassLoader().getResourceAsStream(fn);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            result = buffer.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Could not read from input stream.", e);
        }

        return result;
    }

    public static FileLoader newLoader() {
        return new FileLoader();
    }
}