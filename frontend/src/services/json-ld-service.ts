/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import axios, { AxiosResponse } from "axios";
import { validateJson } from "@/utils/validateUtils";

export default {
  async getAndValidateJsonLd(
    jsonLdUrl: string
  ): Promise<AxiosResponse<any, any>> {
    const jsonLd = await axios.get(jsonLdUrl);
    const jsonLdString = JSON.stringify(jsonLd.data);

    if (validateJson(jsonLdString)) {
      return JSON.parse(jsonLdString);
    } else throw new Error("Source does not contain valid JSON");
  },
};
