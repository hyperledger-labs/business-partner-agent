/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

// import { apiBaseUrl } from "../main";
import axios from "axios";

let apiBaseUrl = "/api";

if (process.env.NODE_ENV === "development") {
  apiBaseUrl = "http://localhost:8080/api";
}

const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 1000,
  headers: {
    "Content-Type": "application/json",
  },
});

export default httpClient;
