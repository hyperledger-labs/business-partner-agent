# Configuration of Schemas and Trusted Issuers


The BPA allows handle documents and (verified) credentials based on schemas. Currently, mostly Indy Schemas are supported. One exception is the Organizational Profile which is defined by a JSON-LD context.

![](https://i.imgur.com/GGotdrE.png)

When adding a new document to the wallet you can select from the configured schemas.


Schemas can be registered either during configuration or during runtime.





## Static configuration 
Example of the configuration of a bank acount schema in the [`application.yml`](https://github.com/hyperledger-labs/business-partner-agent/blob/main/backend/business-partner-agent/src/main/resources/application.yml).

```
schemas:
    #test ledger schemas, can be overwritten / extended when e.g. working with other ledger
    bank-account:
      id: "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0"
      label: "Bank Account"
      defaultAttributeName: "iban"
      # Note: this also works json style restrictions: [{id: 123, label: myLabel}]
      restrictions:
        - issuerDid: "${bpa.did.prefix}M6Mbe3qx7vB4wpZF4sBRjt"
          label: "Demo Bank"
```

* `id` is the Indy schema id
* `label` is the label shown in the UI
* `defaultAttributeName` defines the default label that is used for a document/credential instance based on the schema
* `restrictions` allow to specifiy trusted issuers for this schema. This is used in the Request Presentation flow. The user can select 0..n issuers when [requesting a presentation](request_presentations.md) from a business partner

## Dynamic Configuration

Schemas and trusted issuers can also be configured dynamically using the REST API or the UI. However, all schemas that are configured statically can't be modified by the API.

### Configuration using the REST API

#### Adding a new schema

```
POST: /api/admin/schema

{
  "defaultAttributeName": "string",
  "label": "string",
  "schemaId": "string",
  "trustedIssuer": [
    {
      "issuerDid": "string",
      "label": "string"
    }
  ]
}
```
The `trustedIssuer` array is optional.

#### Adding a trusted issuer to an already configured schema

```
POST: /api/admin/schema/{id}/trustedIssuer
{
  "issuerDid": "string",
  "label": "string"
}
```

Have look at the OpenAPI specification under `/api/admin/schema` to see all endpoints related to schema management.

#### Configuration using the UI

You can configure schemas in the UI under `Settings > Schema and Trusted Issuers`.
