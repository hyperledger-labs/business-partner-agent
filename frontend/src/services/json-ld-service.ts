/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ContextParser,
  IDocumentLoader,
  IJsonLdContext,
} from "jsonld-context-parser";

// Accept json-ld files without checking for application/ld+json media type
class NoMediaTypeFetchDocumentLoader implements IDocumentLoader {
  private readonly fetcher?: (
    url: string,
    init: RequestInit
  ) => Promise<Response>;

  constructor(fetcher?: (url: string, init: RequestInit) => Promise<Response>) {
    this.fetcher = fetcher;
  }

  public async load(url: string): Promise<IJsonLdContext> {
    const response: Response = await (this.fetcher || fetch)(url, {
      headers: new Headers({
        accept: "*/*",
      }),
    });
    if (response.ok) {
      return await response.json();
    } else {
      throw new Error(response.statusText || `Status code: ${response.status}`);
    }
  }
}

const jsonLdContextParser = new ContextParser({
  documentLoader: new NoMediaTypeFetchDocumentLoader(),
});

export default {
  contextParser() {
    return jsonLdContextParser;
  },
};
