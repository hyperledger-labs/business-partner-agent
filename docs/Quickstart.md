## What is the Business Partner Agent

The Business Partner Agent leverages the [aries-cloudagent-python](https://github.com/hyperledger/aries-cloudagent-python) which a python implementation of the [Hyperledger ARIES](https://www.hyperledger.org/use/aries) protocols, which define the issuance, holding, and verification of the [Hyperledger Indy Verifiable Credentials](https://www.hyperledger.org/use/hyperledger-indy) (and [W3C Verifiable Credentials](https://www.w3.org/TR/vc-data-model/)). 

## Why do I need to know those things
There are operations and concepts critical to the usage, security, and privacy that should not be overlooked. The Business Partner Agent's goal is to simplify the usage of these technologies, but is still constrained by the requirements of the infrastructure. 

## Before you start the application

Configure the ledger

This system is build on a distributed ledger (based on indy) that holds public cryptographic keys for Digital IDentities (DID's) as well as other artifacts required to exchange Indy Credentials. 

.env file sets the following important environment variable. 

`ACAPY_GENESIS_URL`
This is the web address the ACA-py agent will use to discover all the nodes on the Indy Ledger you wish to use.

Example Values: 
- **`http://host.docker.internal:9000/genesis`** will configure the agent that the Business Partner Agent uses to write to a VON network hosted directly on your local machine, this is great for local development/testing. That ledger is started when using the [local-network](../scripts/scenarios/local-network) scenario.
- **`http://test.bcovrin.vonx.io/genesis`** will configure the agent that the Business Partner Agent uses to write to a Sovrin ledger hosted by the BC Provincial Government that is for development purposes and is free to use.
- For a production use case, you will need to pick your ledger carefully for one that meets your needs. [Sovrin MainNET](https://sovrin.org/transaction-endorsers/) is one ledger that would be appropriate in production. 


`ACAPY_SEED`

This is a [seed](https://en.wikipedia.org/wiki/Random_seed) value that aca-py uses to randomly generate your cryptographic keys (private and public) and DID. This SEED is the master key to your DID, if someone gained a copy of your SEED, they would be able to impersonate your DID, [identity theft is not a joke](https://youtu.be/WaaANll8h18?t=61). If you lost or overwrote your own SEED, all data stored in any previous wallet would be inaccessible as aca-py no longer has key to decrypt the wallet (however credentials that were already issues can still be verified as your public key is still on the ledger)

If you intend to create a new DID, then the `ACAPY_SEED` value is not important. However if you intend to start the Business Partner Agent to manage an existing DID, you would need to set the `ACAPY_SEED` appropriately. 

## Automatically on startup

1) The ACA-py agent will write your DID and Public Key (VerKey) to the Hyperledger defined by `ACAPY_GENESIS_URL`.
2) The ACA-py agent will provision it's wallet (encrypted database) to the Postgres instance defined in the .env file, that instance is defined and managed by the scripts in this project. 


## The Alice Faber Demo

The Verifiable Credential Community uses the [Alice/Faber Demo](https://kctheservant.medium.com/demonstration-of-hyperledger-aries-cloud-agent-6e476a5426b0) as the standard example. It involves an Individual 'Alice' getting a transcript/degree from the 'Faber' College and presenting that transcript to the 'Acme' Business. 

Verifiable Credentials allow Alice to complete this trusted sharing of data without Acme and Faber needing to communicate directly. 

To execute this demo, you will need...

1) Configure and start Faber College (a BPA) to use the BCovrin Test Ledger [deploy this](../scripts/docker-compose.yaml), ensure `ACAPY_GENESIS_URL=http://test.bcovrin.vonx.io/genesis`

2) Download a SSI Wallet to your Smartphone to act as `Alice's` personal wallet. We have used the `Trinsic Wallet` and `estatus Wallet` for our development testing, but are not affiliated or guarantee that will they operate correctly (The BPA is alpha software)

Steps of the Issuance Demo using Alice Faber.

1) Create a Schema, with whatever values seem appropriate (degree_name, graduation_date) [Demo](https://www.youtube.com/watch?v=wi6Q6WVYHbM&t=1859s)
2) Create a CredDef [Demo](https://www.youtube.com/watch?v=wi6Q6WVYHbM&t=1950s)
3) Create a Connection to your Personal Wallet [Demo](https://youtu.be/wi6Q6WVYHbM?t=1200)
4) Issue Credential to your Personal Wallet [Demo](https://youtu.be/wi6Q6WVYHbM?t=1249)


Steps of the Issuance Demo using Alice Faber.

1) Configure and deploy second BPA to act as ACME
2) Import Schema by Schema ID
3) Create Proof Request template
4) Create Connection to Personal Wallet (same steps as Issuance)
5) Make Proof Request from ACME to Alice for proof of Degree. 



