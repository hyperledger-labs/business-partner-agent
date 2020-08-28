/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

export const CredentialTypes = Object.freeze({
    PROFILE: {
        name: "ORGANIZATIONAL_PROFILE_CREDENTIAL",
        label: "Organizational Profile"
    },
    BANK_ACCOUNT: {
        name: "BANK_ACCOUNT_CREDENTIAL",
        label: "Bank Account"
    },
    OTHER: {
        name: "OTHER",
        label: "Unkown"
    }
})

const Schemas = [{
    type: 'BANK_ACCOUNT_CREDENTIAL',
    fields: [{
        type: 'iban',
        label: 'IBAN',
        required: true
    }, {
        type: 'bic',
        label: 'BIC',
        required: true
    }]

}]

export const getSchema = (type) => {
    return Schemas.find(schema => {
        return schema.type === type
    })
}