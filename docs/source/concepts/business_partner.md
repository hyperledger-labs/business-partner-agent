# Business Partner

In principle a business partner can be any entity with a DID. Currently, we focus on organizations with public DIDs.

At the moment BPA supports ony adding a business partner by public DID. We resolve DIDs with a remote instance of an universal resolver.
The universal resolver instance can be configured by setting the `BPA_RESOLVER_URL` environment variable. Thereby, we can support multiple DID methods.
If the DID document contains a service endpoint of type `profile`, the BPA retrieves the VP and verifies it. If the DID document contains a DIDcomm endpoint it initiates the [connection protocol](https://github.com/hyperledger/aries-rfcs/blob/main/features/0160-connection-protocol/README.md).

BPA currenty uses an ACA-PY with auto flags concerning connections enabled, i.e. a BPA will automatically accept a connection request. Also the ACA-Py is set to support public invitations, such that connection requests based on public DIDs work.

If a business partner does not support DIDcomm, but provides only a `profile` endpoint, it won't know if it got added. If a business partner supports DIDcomm, a new entry will appear in its list of business partners. However,  the connection protocol uses peer DIDs for every connection. Therefore, the entry won't be the public DID of the requesting business partner, and the profile endpoint can't be resolved.

We manually approach this issue currently by issuing `identity credentials` to the business partners that include their public DID. This identity credential can be requested and the connection gets upgraded.

## Used Aries protocols

[0160: Connection Protocol](https://github.com/hyperledger/aries-rfcs/blob/main/features/0160-connection-protocol/README.md)