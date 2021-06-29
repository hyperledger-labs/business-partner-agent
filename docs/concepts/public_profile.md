# Public Profile

The public profile allows an organization to share public information about itself in the form of a W3C Verifiable Presentation (VP). 

The public profile can be seen as a very simple, limited implementation of the Identity Hub concept for an organization.

## Rationale

- Provide basc public information without relying on DIDcomm and Indy Credentials (Anoncreds), i.e. such that even a human with a browser and a ledger explorer can read the data.
-  Allow the integration of documents (self-attested data) and credentials (third-party attested data)
-  Allow the public profile to be a "static" ressource, i.e. a request should not require access to cryptographic keys.

## Design

### Profile Endpoint

The service that returns the public profile VP is advertised in the DID document as a service endpoint of type `profile`.

```
{
    "service":
    [{
        "type":"profile",
        "serviceEndpoint":"https://alice.iil.network/profile.jsonld"
    }]
}
```

### Organizational Profile Credential

The Organizational Profile Credential contains basic organizational master data and provides the means for a self description of the organization. It is natively defined as W3C Verifiable Credential (VC) in JSON-LD.
Hence it supports a richer structure with nested objects and arrays. The JSON-LD Context is available [here](https://raw.githubusercontent.com/iil-network/contexts/master/masterdata.jsonld).

### Indy Credentials

In order to embed Indy Anconcreds in the public profile, we wrap their content in a dynamically generated VC with an additional [Indy Credential JSON-LD context](https://raw.githubusercontent.com/iil-network/contexts/master/indycredential.jsonld). The VC itself is not directly cryptographically verifiable, but contains all information required to request a presentation (proof) via DIDcomm. The intent is that an organization self-attests that it holds these credentials and provides the content. A business partner supporting DIDcomm is able to request a verification if required. 

#### Example of a wrapped in Indy Credential

Given an Indy Credential based on [this schema](https://indy-test.bosch-digital.de/browse/domain?page=1&query=M6Mbe3qx7vB4wpZF4sBRjt%3A2%3Abank_account%3A1.0&txn_type=), we dynamically create the following unsigned VC.
It becomes verifiably self-attested by including it in a signed VP (see example at the end). The actual content of the original credential gets included in the `credentialSubject`.
```
{
         "@context":[
            "https://www.w3.org/2018/credentials/v1",
            {
               "@context":{
                  "sc":"did:sov:M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0",
                  "bic":{
                     "@id":"sc:bic"
                  },
                  "iban":{
                     "@id":"sc:iban"
                  }
               }
            },
            "https://raw.githubusercontent.com/iil-network/contexts/master/indycredential.jsonld"
         ],
         "type":[
            "VerifiableCredential",
            "IndyCredential"
         ],
         "id":"urn:583100e7-9141-4444-b3bf-3bd27fb1e33e",
         "issuanceDate":"2020-12-09T19:21:58Z",
         "credentialSubject":{
            "bic":"456",
            "iban":"1234"
         },
         "label":"1234",
         "indyIssuer":"did:sov:M6Mbe3qx7vB4wpZF4sBRjt",
         "schemaId":"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0",
         "credDefId":"M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc"
      }
```


### Verifiable Presentation

The VP can be verified with the public key listed in the authentication section of the DID document.

### Example of a public profile 

The following example shows a public profile that consists of a VP including two verifiable credentials.
- Organization Profile Credential
- Bank Account Indy Credential

```
{
   "@context":[
      "https://www.w3.org/2018/credentials/v1"
   ],
   "type":[
      "VerifiablePresentation"
   ],
   "verifiableCredential":[
      {
         "@context":[
            "https://www.w3.org/2018/credentials/v1",
            "https://raw.githubusercontent.com/iil-network/contexts/master/masterdata.jsonld",
            "https://raw.githubusercontent.com/iil-network/contexts/master/labeled-credential.jsonld"
         ],
         "type":[
            "VerifiableCredential",
            "LabeledCredential",
            "OrganizationalProfileCredential"
         ],
         "id":"urn:992c5897-e897-484f-99b2-33070593a128",
         "issuer":"did:sov:VoSfM3eGaPxduty34ySygw",
         "issuanceDate":"2020-12-14T14:48:08Z",
         "credentialSubject":{
            "id":"did:sov:VoSfM3eGaPxduty34ySygw",
            "type":"Legal Entity",
            "altName":"",
            "legalName":"Robert Bosch GmbH (ILL)",
            "identifier":[
               {
                  "id":"123",
                  "type":"D-U-N-S"
               }
            ],
            "registeredSite":{
               "id":"ff35b805-c7c2-49aa-bb1c-d90383fd3919",
               "address":{
                  "city":"Gerlingen",
                  "region":"",
                  "country":"Germany",
                  "zipCode":"79999",
                  "streetAddress":"Mauserstrasse 1"
               }
            }
         },
         "label":"Robert Bosch GmbH (ILL)"
      },
      {
         "@context":[
            "https://www.w3.org/2018/credentials/v1",
            {
               "@context":{
                  "sc":"did:sov:M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0",
                  "bic":{
                     "@id":"sc:bic"
                  },
                  "iban":{
                     "@id":"sc:iban"
                  }
               }
            },
            "https://raw.githubusercontent.com/iil-network/contexts/master/indycredential.jsonld"
         ],
         "type":[
            "VerifiableCredential",
            "IndyCredential"
         ],
         "id":"urn:583100e7-9141-4444-b3bf-3bd27fb1e33e",
         "issuanceDate":"2020-12-09T19:21:58Z",
         "credentialSubject":{
            "bic":"456",
            "iban":"1234"
         },
         "label":"1234",
         "indyIssuer":"did:sov:M6Mbe3qx7vB4wpZF4sBRjt",
         "schemaId":"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0",
         "credDefId":"M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc"
      }
   ],
   "proof":{
      "type":"Ed25519Signature2018",
      "created":"2020-12-14T14:48:08Z",
      "verificationMethod":"did:sov:VoSfM3eGaPxduty34ySygw#key-1",
      "proofPurpose":"authentication",
      "jws":"eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0..fYhHd3qJlGZjsiDMgYH-0cFlhgPlvW4NQw5pkKQX5KN0_jBKhbjJwIwzrplo9tPDASB2tRJAXnWBykgKPz8FAQ"
   }
}
```