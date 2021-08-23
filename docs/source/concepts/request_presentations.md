# Request Presentations

BPA allows to users to request presentations (proofs) of credentials from other business partners.

## Request a presentation using the REST API

The REST API allows to send either arbitrary raw [Indy proof requests](https://github.com/hyperledger/indy-sdk/blob/main/libindy/src/domain/anoncreds/proof_request.rs) or proof requests by schema id and an optional lists of issuers. Thereby, all attributes of a given credential are requested.

```
POST: /api/partners/{id}/proof-request

{
  "requestBySchema": {
    "issuerDid": [
      "string"
    ],
    "schemaId": "string"
  },
  "requestRaw": indyProofRequest
}
```

## Request a presentation using the UI

The UI only allows to request by schema id and issuer.
You can request a presentation from a business partner in the UI as follows:

`Business Partners > Select the respective business partner > Request Presentation`

## Possible Improvements
- Support for [Aries RFC 0454 Present Proof v2](https://github.com/hyperledger/aries-rfcs/blob/main/features/0454-present-proof-v2/README.md) to support W3C Verifiable Presentations.
- Handle presentation requests in BPA (not ACA-PY, see note below)
  - Answer requests manually in UI or via BPA managed settings
- Allow requesting documents
- Allow specifing `presentation definitions`, i.e. configuration of specific proof requests similar to [Schemas and trusted issuers](./schemas_trusted_issuers.md).

## Notes
- BPA in holder/prover role does currently rely on ACA-Py auto flags, i.e. presentation requests will be automatically fullfilled if possible.

## Used Aries protocols

[0037: Present Proof Protocol 1.0](https://github.com/hyperledger/aries-rfcs/blob/main/features/0037-present-proof/README.md)