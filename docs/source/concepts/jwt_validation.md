# JWT Token Validation

JSON Web Token (JWT) is a term standing for two implementations: JSON Web Signature (JWS) and JSON Web Encryption (JWE). 
JWE is not very common, and most of the time you will see JWS.

## JWS Basics

A token consists of three parts:

1. Header
2. Payload
3. Signature

Header and Payload are base64 encoded JSON documents whose contents are based on [RFC7519](https://tools.ietf.org/html/rfc7519). 
The signature is created by hashing header and payload and then signing the result with a private key and base64 encoding the result. 
The JWS presentation is built by concatenating all three parts with dots in between, so in short the result looks like: 
Concat(Base64(Header) + '.' + Base64(Payload) + '.' + Base64(Signature).

Note: To restrict parsing effort the CA only allows EdDSA as the signature algorithm with an Ed25519 key pair.

## JWS in a regular (web-) service context

To verify the token, you need the public key of the signing party. 
In a web service context this key is usually statically configured and only changes when the key is rotated or expired. 
As both header and payload are part of the signature any manipulation of the header or the payload becomes evident immediately.

## JWS in the context of business-partner-agent:web

Here we have the issue that the public key is delivered separately from the token, this opens possibilities for manipulation. 
In this context we have the following token retrieval and validation process:

* Step 1. Resolve the public profile service endpoint via the did document

This depends on the did method, so the did document can be written on a ledger or made available by a web service. 
As did document presentations are not always the same, resolution happens with the help of the universal resolver, 
that (hopefully) returns a normalised presentation of the did document.

Example did document with a profile type endpoint within the service section:
```json
{
  "@context": [
    "https://www.w3.org/ns/did/v1"
  ],
  "id": "did:sov:F6dB7dMVHUQSC64qemnBi7",
  "verificationMethod": [
    {
      "id": "did:sov:F6dB7dMVHUQSC64qemnBi7#key-1",
      "type": "Ed25519VerificationKey2018",
      "controller": "did:sov:F6dB7dMVHUQSC64qemnBi7",
      "publicKeyBase58": "8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D"
    }
  ],
  "service": [
    {
      "id": "did:sov:F6dB7dMVHUQSC64qemnBi7#did-communication",
      "type": "did-communication",
      "serviceEndpoint": "http://localhost:8080",
      "recipientKeys": [
        "did:sov:F6dB7dMVHUQSC64qemnBi7#key-1"
      ],
      "routingKeys": [],
      "priority": 1
    },
    {
      "id": "did:sov:F6dB7dMVHUQSC64qemnBi7#profile",
      "type": "profile",
      "serviceEndpoint": "http://localhost:8080/profile.jsonld"
    }
  ]
}
```

* Step 2. Resolve the public profile

In this case the profile service endpoint returns a JSON structure that contains the JWT.

```
{
  "decoded": {},
  "jwt": ""
}
```

* Step 3. Decode token and verify signature

Decoding just means breaking the token down into the three parts mentioned above and verifying its integrity. 
Verification is the tricky part though.

* Step 4. Resolve the public key

If the token is self-signed the key is already part of the did document. 
If the token was signed by a third party the public key needs to be resolved by pulling the did document of the third party.

### Token verification scenarios

A token can be either self-signed or signed by a third party. 
Self-signed in the context of verifiable credentials means subject (sub) == issuer (iss). 
In JWT terms iss === sub. In general, we have to decide if we want to trust an issuer to make specific claim about a subject. 
In the self-signed public profile data case we have (human) trust if issuer == subject. 
But in some cases we would probably not trust did:indy:1 making claims about did:indy:2.

The second step is cryptographic trust, to check if the issuer authenticated the credential. 
This is the part where we have to check that the public key which verifies the JWT is authorized by the issuer. 
Therefore, we have to check if the verifying public key is part of the did document of the issuer

Therefore, we can state that:

1. If there is no kid and iss == sub then the token is considered self-signed
2. If kid && iss == sub then the token is also considered self-signed, and kid must be part of the issuers did document
3. If sub != iss the token is signed by a third party
4. If kid && sub != iss, the token is signed by a third party, and kid must be part of the issuers did document


#### Self-signed - no key id in the header

If the JWT is in the following form:

```jsom
{
  "typ": "JWT",
  "alg": "EdDSA"
},
{
  "sub": "did:web:faber.iil.network",
  "iss": "did:web:faber.iil.network"
}
```

The did document must only contain a single key. If there is more than one key element the token is considered invalid. 
If the structure is valid the token signature is validated with the value of "publicKeyBase58".

```json
{
  "id": "did:web:faber.iil.network",
  "publicKey": [
    {
      "id": "did:web:faber.iil.network#key-1",
      "type": "Ed25519VerificationKey2018",
      "publicKeyBase58": "24j9iYZTPRE3L4W5kRixBAEQLHJzfzuHsqiQyCnVEbKZ"
    }
  ]
}
```

#### Self-signed - key id in the header

If the header contains a kid...

```
{
  "kid": "did:web:faber.iil.network#key-1",
  "typ": "JWT",
  "alg": "EdDSA"
},
{
  "sub": "did:web:faber.iil.network",
  "iss": "did:web:faber.iil.network"
}
```

...the did document is allowed to have multiple keys. If the did document is missing a matching publicKey:id the token is considered invalid.

```json
{
  "id": "did:web:faber.iil.network",
  "publicKey": [
    {
      "id": "did:web:faber.iil.network#key-1",
      "type": "Ed25519VerificationKey2018",
      "publicKeyBase58": "24j9iYZTPRE3L4W5kRixBAEQLHJzfzuHsqiQyCnVEbKZ"
    }
  ]
}
```

#### Signed by third party

Like mentioned above, the public key needs to be resolved by yet another call to the universal resolver to retrieve 
the did document and public key of the third party. The `did` used for resolution is taken from the iss field of the Payload. 
If there is a kid then the same mechanism as above is applied.

This use-case is currently out of scope and validation will fail.