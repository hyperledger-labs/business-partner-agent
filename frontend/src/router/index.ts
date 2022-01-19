/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Vue from "vue";
import VueRouter from "vue-router";
import Dashboard from "@/views/Dashboard.vue";
import Identity from "@/views/Identity.vue";
import PublicProfile from "@/views/PublicProfile.vue";
import ContactPerson from "@/views/ContactPerson.vue";
import Wallet from "@/views/Wallet.vue";
import Document from "@/views/Document.vue";
import RequestVerification from "@/views/RequestVerification.vue";
import RequestPresentation from "@/views/RequestPresentation.vue";
import SendPresentation from "@/views/SendPresentation.vue";
import Credential from "@/views/Credential.vue";
import Partners from "@/views/partner/Partners.vue";
import Partner from "@/views/partner/Partner.vue";
import AddPartner from "@/views/partner/AddPartner.vue";
import AddPartnerbyURL from "@/views/partner/AddPartnerbyURL.vue";
import Settings from "@/views/settings/Settings.vue";
import TagManagement from "@/views/settings/TagManagement.vue";
import SchemaSettings from "@/views/settings/SchemaSettings.vue";
import Schema from "@/views/Schema.vue";
import AddSchema from "@/views/AddSchema.vue";
import About from "@/views/About.vue";
import CredentialManagement from "@/views/issuer/CredentialManagement.vue";
import Notifications from "@/views/Notifications.vue";
import ProofTemplates from "@/views/ProofTemplates.vue";
import ProofTemplateCreate from "@/views/ProofTemplateCreate.vue";
import ProofTemplateView from "@/views/ProofTemplateView.vue";
import RequestPresentationCreateProofTemplate from "@/views/RequestPresentationCreateProofTemplate.vue";
import RequestCredential from "@/views/RequestCredential.vue";
import RequestCredentialCreateDocument from "@/views/RequestCredentialCreateDocument.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    name: "Dashboard",
    component: Dashboard,
  },
  {
    path: "/app/identity",
    name: "Identity",
    component: Identity,
  },
  {
    path: "/app/public-profile",
    name: "PublicProfile",
    component: PublicProfile,
    props: true,
  },
  {
    path: "/app/wallet/contact",
    name: "ContactPerson",
    component: ContactPerson,
    props: true,
  },
  {
    path: "/app/wallet",
    name: "Wallet",
    component: Wallet,
  },
  {
    path: "/app/wallet/document/new",
    name: "DocumentAdd",
    component: Document,
    props: true,
  },
  {
    path: "/app/wallet/document/:id",
    name: "Document",
    component: Document,
    props: true,
  },
  {
    path: "/app/wallet/document/:documentId/verify",
    name: "RequestVerification",
    component: RequestVerification,
    props: true,
  },
  {
    path: "/app/wallet/credential/:id",
    name: "Credential",
    component: Credential,
    props: true,
  },
  {
    path: "/app/partners",
    name: "Partners",
    component: Partners,
  },
  {
    path: "/app/partners/add",
    name: "AddPartner",
    component: AddPartner,
  },
  {
    path: "/app/partners/create-qr",
    name: "AddPartnerbyURL",
    component: AddPartnerbyURL,
  },
  {
    path: "/app/partners/:id",
    name: "Partner",
    component: Partner,
    props: true,
  },
  {
    path: "/app/partners/:id/request-presentation",
    name: "RequestPresentation",
    component: RequestPresentation,
    props: true,
  },
  {
    path: "/app/partners/:id/request-presentation/create-proof-template",
    name: "RequestPresentationCreateProofTemplate",
    component: RequestPresentationCreateProofTemplate,
    props: true,
  },
  {
    path: "/app/partners/:id/send-presentation",
    name: "SendPresentation",
    component: SendPresentation,
    props: true,
  },
  {
    path: "/app/partners/:id/request-credential",
    name: "RequestCredential",
    component: RequestCredential,
    props: true,
  },
  {
    path: "/app/partners/:id/request-credential/create-document",
    name: "RequestCredentialCreateDocument",
    component: RequestCredentialCreateDocument,
    props: true,
  },
  {
    path: "/app/settings",
    name: "Settings",
    component: Settings,
  },
  {
    path: "/app/settings/tag",
    name: "TagManagement",
    component: TagManagement,
  },
  {
    path: "/app/settings/schema",
    name: "SchemaSettings",
    component: SchemaSettings,
  },
  {
    path: "/app/schema/add",
    name: "AddSchema",
    component: AddSchema,
  },
  {
    path: "/app/schema/:id",
    name: "Schema",
    component: Schema,
    props: true,
  },
  {
    path: "/app/about",
    name: "About",
    component: About,
  },
  {
    path: "/app/credential-management",
    name: "CredentialManagement",
    component: CredentialManagement,
  },
  {
    path: "/app/notifications",
    name: "Notifications",
    component: Notifications,
  },
  {
    path: "/app/proof-templates",
    name: "ProofTemplates",
    component: ProofTemplates,
  },
  {
    path: "/app/proof-template",
    name: "ProofTemplateCreate",
    component: ProofTemplateCreate,
    props: true,
  },
  {
    path: "/app/proof-template/:id",
    name: "ProofTemplateView",
    component: ProofTemplateView,
    props: true,
  },
];

const router = new VueRouter({
  mode: "history",
  routes,
  scrollBehavior: function (to) {
    if (to.hash) {
      return {
        selector: to.hash,
        offset: { x: 0, y: 100 },
      };
    }
  },
});

export default router;
