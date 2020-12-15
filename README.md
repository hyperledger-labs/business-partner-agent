# Business Partner Agent [![join the chat][rocketchat-image]][rocketchat-url]

[rocketchat-url]: https://chat.hyperledger.org/channel/business-partner-agent
[rocketchat-image]: https://open.rocket.chat/images/join-chat.svg

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://dev.azure.com/economy-of-things/Business-Partner-Agent/_apis/build/status/hyperledger-labs.business-partner-agent?branchName=master)](https://dev.azure.com/economy-of-things/Business-Partner-Agent/_build/latest?definitionId=79&branchName=master)

# Short Description
The Business Partner Agent allows to manage and exchange master data between organizations. Exchange of master data should not happen via telephone, excel, e-mail or various supplier portals. Organizations should be able to publish documents like addresses, locations, contacts, bank accounts and certifications publicly, or exchange them privately with their business partners in a machine-readable and tamper-proof format. Furthemore, verified documents, issued by trusted institutions, are able streamline the process of onboarding new business partners.

The Business Partner Agent is built on top of the Hyperledger Self-Sovereign Identity Stack, in particular Hyperledger Indy and Hyperledger Cloud Agent Python.

## Current Features

- Attach a public organizational profile to your public DID
- Add business partners by their public DID and view their public profile
- Add documents (currently bank acccounts are supported) and request verifications from business partners
- Share and request verified documents with/from your business partners

## Project Status

A first alpha version of Business Partner Agent is available, see
[releases page](https://github.com/hyperledger-labs/business-partner-agent/releases) and related tags.
It is not ready for production use. 

# Getting Started

The Business Partner Agent supports two modes
1. Web mode: Serves a did:web identity and allows to publish a public organizational profile (Currently not maintained)
2. Aries mode: Utilizes an identity on an Hyperledger Indy ledger (default: https://indy-test.bosch-digital.de/) and enables Aries interaction protocols.

In our documentation, the agent will be started in Aries mode.

## Run a business partner agent

You can either run the agent [via docker-compose](scripts/) (recommended for e.g. development / debugging) or deploy it into a [kubernetes cluster (via helm)](charts/bpa).

## Play a demo scenario

See [demo](./demo.md)

# Initial Committers
- https://github.com/etschelp
- https://github.com/frank-bee
- https://github.com/domwoe

# Sponsor
- https://github.com/swcurran Co-Chair of the Aries Working Group

# Credits

See [Credits](./CREDITS.md)

# License

Project source code files are made available under the Apache License, Version 2.0 (Apache-2.0), located in the [LICENSE](LICENSE) file. Project documentation files are made available under the Creative Commons Attribution 4.0 International License (CC-BY-4.0), available at http://creativecommons.org/licenses/by/4.0/.
