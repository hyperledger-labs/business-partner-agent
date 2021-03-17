# Request Verifications

BPA supports two mechanisms to receive Indy credentials in its wallet. 

1. An issuer can trigger the usual [issue credential flow](https://github.com/hyperledger/aries-rfcs/tree/master/features/0036-issue-credential) and the credential shows up in the wallet, because ACA-Py is configured with auto flags concerning credential issuance.
2. A user can request a verification for a document in the wallet.