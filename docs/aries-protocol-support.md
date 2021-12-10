## ACA-PY args file

`docker run bcgovimages/aries-cloudagent:py36-1.16-1_0.7.1 start --help`


An option that is supported by the BPA means that it should be set as false in the 'acapy-static-args.yml' file. The BPA will receive the webhook from aca-py and handle it appropriatly, saving relevant data in the database, and displaying any 'in-progress' or 'completed' items in the UI for Users to inspect and interact with. 

_For simplicity or demo purposes, it may be appropriate to set a supported flag to true. The BPA will still update any records appropriately, but aca-py will proceed with those protocols immediately, without interaction from the BPA, or the User._

Options that are 'NOT-SUPPORTED' must be left true, so aca-py can automatically proceed with the aries protocols and the BPA does not have the behaviour to proceded with those protocols, either automatically, or for the user to decide.

Feature should be built to allow the BPA to control protocols labelled 'NOT-SUPPORTED'

```
docker run bcgovimages/aries-cloudagent:py36-1.16-1_0.7.1 start --help
...
Debug:
...
  --auto-accept-invites
                        Automatically accept invites without firing a webhook
                        event or waiting for an admin request. Default: false.
                        [env var: ACAPY_AUTO_ACCEPT_INVITES]
        **SUPPORTED BY BPA**

  --auto-accept-requests
                        Automatically accept connection requests without
                        firing a webhook event or waiting for an admin
                        request. Default: false. [env var:
                        ACAPY_AUTO_ACCEPT_REQUESTS]
        **SUPPORTED BY BPA**

  --auto-respond-messages
                        Automatically respond to basic messages indicating the
                        message was received. Default: false. [env var:
                        ACAPY_AUTO_RESPOND_MESSAGES]
        **SUPPORTED BY BPA**

  --auto-respond-credential-proposal
                        Auto-respond to credential proposals with
                        corresponding credential offers [env var:
                        ACAPY_AUTO_RESPOND_CREDENTIAL_PROPOSAL]
        **SUPPORTED BY BPA**

  --auto-respond-credential-offer
                        Automatically respond to Indy credential offers with a
                        credential request. Default: false [env var:
                        ACAPY_AUTO_RESPOND_CREDENTIAL_OFFER]
        **SUPPORTED BY BPA**
        
  --auto-respond-credential-request
                        Auto-respond to credential requests with corresponding
                        credentials [env var:
                        ACAPY_AUTO_RESPOND_CREDENTIAL_REQUEST]
        **SUPPORTED BY BPA**
        
  --auto-respond-presentation-proposal
                        Auto-respond to presentation proposals with
                        corresponding presentation requests [env var:
                        ACAPY_AUTO_RESPOND_PRESENTATION_PROPOSAL]
        **NOT-SUPPORTED**
        
  --auto-respond-presentation-request
                        Automatically respond to Indy presentation requests
                        with a constructed presentation if a corresponding
                        credential can be retrieved for every referent in the
                        presentation request. Default: false. [env var:
                        ACAPY_AUTO_RESPOND_PRESENTATION_REQUEST]
        **SUPPORTED BY BPA**
        
  --auto-store-credential
                        Automatically store an issued credential upon receipt.
                        Default: false. [env var: ACAPY_AUTO_STORE_CREDENTIAL]
        **NOT-SUPPORTED**
        
  --auto-verify-presentation
                        Automatically verify a presentation when it is
                        received. Default: false. [env var:
                        ACAPY_AUTO_VERIFY_PRESENTATION]
        **NOT-SUPPORTED**
        
...
Protocol:
  --auto-ping-connection
                        Automatically send a trust ping immediately after a
                        connection response is accepted. Some agents require
                        this before marking a connection as 'active'. Default:
                        false. [env var: ACAPY_AUTO_PING_CONNECTION]
        **SUPPORTED BY BPA**             
...
```

The default 'acapy-static-args.yml` can be found [here](../scripts/acapy-static-args.yml). 
