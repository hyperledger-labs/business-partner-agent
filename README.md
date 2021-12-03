# Business Partner Agent [![join the chat][rocketchat-image]][rocketchat-url]

[rocketchat-url]: https://chat.hyperledger.org/channel/business-partner-agent
[rocketchat-image]: https://open.rocket.chat/images/join-chat.svg

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![CI/CD](https://github.com/hyperledger-labs/business-partner-agent/workflows/CI/CD/badge.svg)](https://github.com/hyperledger-labs/business-partner-agent/actions?query=workflow%3ACI%2FCD+branch%3Amain)

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/hyperledger-labs/business-partner-agent)

# Short Description
The BPA allows organizations to verify, hold, and issue verifiable credentials.

The Business Partner Agent is built on top of the Hyperledger Self-Sovereign Identity Stack, in particular 
[Hyperledger Indy](https://www.hyperledger.org/use/hyperledger-indy) and 
[Hyperledger Cloud Agent Python](https://github.com/hyperledger/aries-cloudagent-python).

![](https://i.imgur.com/kz4s0gQ.png)

## Most Important Features

- Attach a public organizational profile to your public DID (either did:indy/sov or did:web)
- Add business partners by their public DIDs or via invitations and view their public profiles
- Business partners can be other cloud agents or smartphone wallets
- Basic chat functionality to interact with business partners
- Add documents based on Indy schemas and request verifications from business partners
- Issue verifiable credentials to your business partners
- Create templates for presentation requests supporting zero knowledge proofs (selective disclose and predicate proofs) 
- Send and respond to presentation requests

## Features in Detail

| Role/Feature     | Flow                                                                    | Protocol Version                  |
|------------------|-------------------------------------------------------------------------|-----------------------------------|
| Issuer           |                                                                         |                                   |
|                  | auto: issue credential                                                  | indy: v1, v2                      |
|                  | manual: send credential offer to holder                                 | indy: v1, v2                      |
|                  | manual: receive credential proposal from holder                         | indy: v1, v2                      |
|                  | manual: decline credential proposal from holder and provide reason      | indy: v1, v2                      |
|                  | revoke issued credential (requires tails server)                        | n/a                               |
| Holder           |                                                                         |                                   |
|                  | auto: receive credential                                                | indy: v1, v2                      |
|                  | manual: send credential proposal to issuer (based on document)          | indy: v1, v2                      |
|                  | manual: receive credential offer from issuer                            | indy: v1, v2                      |
|                  | manual: decline credential offer from issuer                            | indy: v1, v2                      |
|                  | scheduled revocation check on all received credentials                  | n/a                               |
| Prover           |                                                                         |                                   |
|                  | auto: send presentation to verifier                                     | indy: v1, v2                      |
|                  | auto: answer presentation request                                       | indy: v1, v2                      |
|                  | manual: accept/decline presentation request and provide reason          | indy: v1, v2                      |
| Verifier         |                                                                         |                                   |
|                  | auto: request presentation from prover based on proof template          | indy: v1, v2                      |
|                  | auto: receive and verify presentation from prover                       | indy: v1, v2                      |
| Connection       |                                                                         |                                   |
|                  | connect by did:sov, did:web (if endpoint is aca-py)                     | did-exchange                      |
|                  | receive invitation by URL                                               | connection-protocol, OOB          |
|                  | create invitation (barcode or URL)                                      | connection-protocol, OOB          |
|                  | auto: accept incoming connection                                        | did-exchange, connection-protocol |
|                  | manual: accept incoming connection                                      | did-exchange, connection-protocol |
|                  | optional: scheduled trust ping to check connection status               | n/a                               |
|                  | tag a connection, e.g. as trusted issuer                                | n/a                               |
| Ledger           |                                                                         |                                   |
|                  | send schema to the ledger (requires endorser role)                      | n/a                               |
|                  | create a credential definition on the ledger (requires endorser role)   | n/a                               |
| Basic Message    |                                                                         |                                   |
|                  | send and receive basic messages via chat window                         | n/a                               |
| Tasks/Activities |                                                                         |                                   |
|                  | list of tasks that need attention, and list of past activities          | n/a                               |
| TAA              |                                                                         |                                   |
|                  | if ledger is configured with a TAA, show it and give option to accept   | n/a                               |
| Read Only Ledger |                                                                         |                                   |
|                  | if mode is set to web only                                              | n/a                               |
| Public Profile   |                                                                         |                                   |
|                  | web accessible (self signed) imprint based on credentials or documents  | n/a                               |

## Upcoming Features

- Support additional verifiable credential formats (W3C JSON-LD VCs in addition to Indy Anoncreds)
- Business rules to automate processes
- Endorser support (both as endorser and transaction author)
- Multi-user and roles support

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

The easiest way to run two agents, is to work with [gitpod](https://gitpod.io/). 
Gitpod launches a pre-configured IDE in the browser and the agents being launched in the background.
See [debugging docu](https://github.com/hyperledger-labs/business-partner-agent/blob/main/docs/DEBUGGING.md#fronend-and-backend-with-gitpod).

# Documentation and Tutorials

User documentation is located at https://hyperledger-labs.github.io/business-partner-agent

Learn how to contribute in [Contributing](CONTRIBUTING.md). You can also start by filing an issue.

Regarding release process, we do not follow a strict process yet, nevertheless we follow the guidelines described in [Publishing](PUBLISHING.md).

Learn what aries protocols can be controlled by the BPA in [aca-py-args](scripts/aca-py-args.md)

## Business Partner Agent in Action
- [COP26 Presented by BC Goverment and OpenEarth Foundation](https://www.youtube.com/watch?v=q0Jml3isSh8)
- [Use Case and Technical Demonstration Playlist](https://www.youtube.com/watch?v=TGiiNOoVoJs&list=PL9CV_8JBQHiooHv05idOTrR2eBAJM89LX)

# Hyperledger Labs Sponsor
- https://github.com/swcurran Co-Chair of the Aries Working Group

# Credits

See [Credits](./CREDITS.md)

# License

Project source code files are made available under the Apache License, Version 2.0 (Apache-2.0), located in the [LICENSE](LICENSE) file. Project documentation files are made available under the Creative Commons Attribution 4.0 International License (CC-BY-4.0), available at http://creativecommons.org/licenses/by/4.0/.
