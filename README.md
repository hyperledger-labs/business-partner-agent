# Business Partner Agent [![join the chat][rocketchat-image]][rocketchat-url]

[rocketchat-url]: https://chat.hyperledger.org/channel/business-partner-agent
[rocketchat-image]: https://open.rocket.chat/images/join-chat.svg

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI/CD](https://github.com/hyperledger-labs/business-partner-agent/workflows/CI/CD/badge.svg)](https://github.com/hyperledger-labs/business-partner-agent/actions?query=workflow%3ACI%2FCD+branch%3Amain)

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/hyperledger-labs/business-partner-agent)

# Short Description
The Business Partner Agent allows to manage and exchange master data between organizations. Exchange of master data should not happen via telephone, excel, e-mail or various supplier portals. Organizations should be able to publish documents like addresses, locations, contacts, bank accounts and certifications publicly, or exchange them privately with their business partners in a machine-readable and tamper-proof format. Furthemore, verified documents, issued by trusted institutions, are able streamline the process of onboarding new business partners.

The Business Partner Agent is built on top of the Hyperledger Self-Sovereign Identity Stack, in particular [Hyperledger Indy](https://www.hyperledger.org/use/hyperledger-indy) and [Hyperledger Cloud Agent Python](https://github.com/hyperledger/aries-cloudagent-python).

![](https://i.imgur.com/kz4s0gQ.png)

## Current Features

- Attach a public organizational profile to your public DID (either did:indy/sov or did:web)
- Add business partners by their public DID and view their public profile.
- Add documents based on Indy schemas and request verifications from business partners
- Share and request verified documents with/from your business partners

## Project Status

A first alpha version of Business Partner Agent is available, see
[Helm Chart](https://github.com/hyperledger-labs/business-partner-agent-chart) and [Docker images](https://github.com/orgs/hyperledger-labs/packages/container/package/business-partner-agent)
It is not ready for production use.  Releases are in general considered "alpha", which means API may change at any time and we do not have explicit / planned system tests (See also [Publishing](PUBLISHING.md)). 

# Getting Started

The Business Partner Agent supports two modes
1. Web mode: Serves a did:web identity and allows to publish a public organizational profile.
2. Indy mode: Utilizes an identity on an Hyperledger Indy ledger (default: https://indy-test.bosch-digital.de/)

Both modes are currently coupled with a specific instance of an Indy network in order to read schemas and credential definitions.
The agent is started in Indy mode per default and tries to connect with our test network. Please refer to the [.env-example file](scripts/.env-example) to start the agent in web mode or connect to a different Indy network.

## Run a business partner agent with docker-compose or helm

You can run the agent [via docker-compose](scripts/) (recommended for e.g. development / debugging) or deploy it into a [kubernetes cluster (via helm)](https://github.com/hyperledger-labs/business-partner-agent-chart).

## Run a business partner agent with gitpod

*EXPERIMENTAL! Please provide feedback in our [related discussion](https://github.com/hyperledger-labs/business-partner-agent/discussions/472)*

The easiest way to run two agents, is to work with [gitpod](https://gitpod.io/). 
Gitpod launches a pre-configured IDE in the browser and the agents being launched in the background.
See [debugging docu](https://github.com/hyperledger-labs/business-partner-agent/blob/main/docs/DEBUGGING.md#fronend-and-backend-with-gitpod).

# Documentation and Tutorials

User documentation see https://hyperledger-labs.github.io/business-partner-agent

Learn how to contribute in [Contributing](CONTRIBUTING.md). You can also start by filing an issue.

Regarding release process, we do not follow a strict process yet, nevertheless we follow the guidelines described in [Publishing](PUBLISHING.md).

# Hyperledger Labs Sponsor
- https://github.com/swcurran Co-Chair of the Aries Working Group

# Credits

See [Credits](./CREDITS.md)

# License

Project source code files are made available under the Apache License, Version 2.0 (Apache-2.0), located in the [LICENSE](LICENSE) file. Project documentation files are made available under the Creative Commons Attribution 4.0 International License (CC-BY-4.0), available at http://creativecommons.org/licenses/by/4.0/.
