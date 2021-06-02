# Security - Keycloak

By default, the security is session based, and uses local users stored in the database.  

This document is how to add Keycloak security on top of the basic configuration.


## Setting up Keycloak Client
There are a few things that need to be set up in Keycloak to make it work with this configuration.

You, or your Keycloak realm administrator will need to create a new Client, Client roles and Client Scopes.

First, you should know what URL you are protecting, for this guide our BPA will be running at `https://my-bpa.com` and our Keycloak Realm will be `https://my-keycloak.com/auth/realms/master`.

We will be using `my-bpa-client` as an example client id.

Obviously rename URLs and client ids as appropriate.

1. Create New Client
    * Set Client ID = `my-bpa-client`
    * Set Root URL = `https://my-bpa.com`
    * Click Save
1.  Edit Client
    * Set Access Type to confidential 
    * Standard Flow Enabled = true
    * Implicit Flow Enabled = false
    * Direct Access Grants Enabled = true
    * Service Accounts Enabled = false
    * OAuth 2.0 Device Authorization Grant Enabled = false
    * Authorization Enabled = false
    * Valid Redirect URIs = `https://my-bpa.com/*`
    * Web origins = `+`
    * Click Save
1.  Roles
    * Add Role, set name to `ADMIN`
    * Add Role, set name to `USER`
1.  Client Scopes
    * Set Assigned Default Client Scopes to `email`, `profile`.
1.  Mappers
    * Add Builtin, select client roles
    * Select your client id: `my-bpa-client`
    * Set Client Role prefix = `ROLES_`
    * Multivalued = true
    * Token Claim Name = `roles`
    * Add to ID token = true
    * Add to access token = true
    * Add to userinfo = true (this is optional)
1.  Credentials
    * Copy the client secret, we will need them for `BPA_KEYCLOAK_CLIENT_SECRET` environment variable.

Now you can manage your users (through Groups, or Default Roles, or however you want). Add your BPA users to `USER` and/or `ADMIN`. 

By adding `email` and `profile` to the Assigned Default Client Scopes, we can ask for `openid` and get the profile and email without specifically asking for them.
 
Because of the Mapper, these Keycloak roles will appear in the claims under the property `roles` and the role names will be `ROLE_ADMIN` and `ROLE_USER`.  

Example Access Token
```json
{
  "exp": 1621626402,
  "iat": 1621626342,
  "jti": "b1df2e5d-3a21-42d4-9f23-88f2d800bb22",
  "iss": "http://my-keycloak.com/auth/realms/master",
  "sub": "aacdfe53-ecd7-48ec-86b4-d8d86dc0aa02",
  "typ": "Bearer",
  "azp": "keycloak",
  "session_state": "c58a76d0-625f-4f65-b809-dfd3c4f451d2",
  "acr": "1",
  "scope": "openid profile email",
  "email_verified": true,
  "roles": [
    "ROLE_USER",
    "ROLE_ADMIN"
  ],
  "name": "Admin User",
  "preferred_username": "adminUser",
  "given_name": "Admin",
  "family_name": "User",
  "email": "admin@my-bpa.com"
}
```  

## Environment Variables
Review [security-keycloak.yml](../../../business-partner-agent/backend/business-partner-agent/src/main/resources/security-keycloak.yml) and note the defaults and environment variable overrides. 

| Variable | Purpose | Default | Example |
| --- | --- | --- | --- |
| BPA_KEYCLOAK_REDIRECT_URI | Redirect called by Keycloak after logging out (BPA_KEYCLOAK_ENDSESSION_URL) | https://my-bpa.com/logout | |
| BPA_KEYCLOAK_CLIENT_ID | Your client's id | | my-bpa-client |
| BPA_KEYCLOAK_CLIENT_SECRET | Your client's secret | | |
| BPA_KEYCLOAK_ISSUER | Keycloak Issuer URL, to configure the authentication handlers | https://my-keycloak.com/auth/realms/master | |
| BPA_KEYCLOAK_ENDSESSION_URL | Tell Keycloak to log you out  | | https://my-keycloak.com/auth/realms/master/protocol/openid-connect/logout |
| BPA_KEYCLOAK_SCOPES | Which scopes to request | openid | |
| BPA_KEYCLOAK_ROLES_NAME | Indicate which claim to use for user roles | roles | |
| BPA_KEYCLOAK_NAME_KEY | Indicate which claim to use for user name | preferred_username | |


## Adding security-keycloak to Application configuration
The [security-keycloak.yml](../../../business-partner-agent/backend/business-partner-agent/src/main/resources/security-keycloak.yml) file must be added to Micronaut startup. This file builds on top of the existing `micronaut.security` configuration found in [application.yml](../../../business-partner-agent/backend/business-partner-agent/src/main/resources/application.yml). In the `JAVA_OPTS` for the application, set the following: 

```text
-Dmicronaut.config.files=classpath:application.yml,classpath:security-keycloak.yml
```

This will load `application.yml`, then `security-keycloak.yml` which will override and enhance `micronaut.security` configuration.
