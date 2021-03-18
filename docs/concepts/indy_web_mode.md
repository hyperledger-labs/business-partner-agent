# Indy and Web mode

BPA allows to be started in two different modes:

1. Indy (default)
2. Web

The mode can be configured by setting the `BPA_WEB_MODE` environmental variable.
You won't be able to change the mode of an provisioned agent. If you want to change the mode, you need to delete the database.

## Indy mode

In Indy mode, BPA needs to have a DID registered on an Indy network with role endorser.
BPA will register a DID communication endpoint and a profile endpoint on the ledger.

## Web mode

In web mode, BPA will itself serve a DID document at `.well-known/did.json`. Hence, BPA will be represented with a [`did:web` identifier](https://w3c-ccg.github.io/did-method-web/) depending on the domain BPA is hosted.
Similar to Indy mode, both a DID communication endpoint and a profile endpoint will be published in the DID document.