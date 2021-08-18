# Demo Scenario

In this scenario you will setup a public profile and get your bank account verified by your bank.

## Prerequisites 
We assume that you started the Business Partner Agent in Aries mode on the default ledger, and your Agent's endpoint is publicly available.

## Setup a public profile for your organization

The first thing you should do is to setup your public profile.

1. Go to Wallet, click the `+ button`, and select `Organizational Profile`
2. Enter your master data and click `save`
3. The public profile is now available for your business partners

## Add a bank account to your public profile

Next we add some bank account information

1. Go to `Wallet`, click the `+ button` and select `Bank Account`
2. Set the visible in public profile toggle to true
3. Enter your IBAN and BIC and click on `save`

This information is currently self-attested, but we like to have this information verified by our bank, such that our business partners can automatically verify the authenticity of the bank account.

## Add our bank as a business partner

1. Go to `Business Partners` and click the `+ button`
2. Paste `did:sov:M6Mbe3qx7vB4wpZF4sBRjt` in the DID Box. This is the DID of your bank
3. Click on `Lookup Partner`. You'll see the public profile of your partner.
4. Click on `Add Partner`. Now you have a connection with your bank.


## Get your bank account verified by your bank

1. Go to `Wallet` and click on the Bank Account document
2. Click on `>` in the Verification section
3. Select your bank and click `submit`
4. You should see the bank account in the `Verified Credentials` section of your wallet
   
   
You can now send a proof of your verified bank account to one of your business partners.