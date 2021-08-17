# Request Verifications

BPA supports two mechanisms to receive Indy credentials in its wallet. 

1. An issuer can trigger the usual [issue credential flow](https://github.com/hyperledger/aries-rfcs/tree/main/features/0036-issue-credential) and the credential shows up in the wallet, because ACA-Py is configured with the following auto flags.
   - auto-respond-credential-offer: true
   - auto-store-credential: true 
2. A user can request a verification for a document in the wallet.

## Request verifications using the REST API

```
POST : /api/partners/{id}/credential-request
{
  "documentId": "string"
}
```

BPA will create a credential request based on the schema and attributes of the document.

## Request verifications using the UI

`Wallet > Select a document > Request Presentation > Verification > Select Business Partner > Submit`

If trusted issuers are provided for the schema, only business partners which are also trusted issuers are shown.
If a ledger explorer is configured and no trusted issuers are provided, only business partners which can issue the document based on ledger data are shown.

## Notes

The current process (2) assumes that an issuer will just issue a credential (i.e. provide a verification of a document) without further inquiry. This can be motivated as follows:
We assume that the issuer is able to identify the holder already in the process of setting up a connection (see [Business Partner](business_partner.md) for more information). Let's assume the issuer is a bank and an organization has a bank account with this bank. When the organization asks for a verification of the bank account, the bank can check its internal database if the provided bank account in the document belongs to the organization and hence issue the credential.

## Possible Improvements

- In practice, the verification process might trigger a workflow that includes one or more proof requests from the issuer. This could be implemented using [Coprotocols](https://github.com/hyperledger/aries-rfcs/blob/main/concepts/0478-coprotocols/README.md)
- Show all possible issuers for a document in the UI based on ledger data. If an issuer is not already a business partner provide a link to add it. 

## Used Aries protocols

[0036: Issue Credential Protocol 1.0](https://github.com/hyperledger/aries-rfcs/blob/main/features/0036-issue-credential/README.md)