/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent

 SPDX-License-Identifier: Apache-2.0
*/

import Vue from "vue";
import VueRouter from "vue-router";
import Dashboard from "../views/Dashboard.vue";
import Identity from "../views/Identity.vue";
import PublicProfile from "../views/PublicProfile.vue";
import ContactPerson from "../views/ContactPerson.vue";
import Wallet from "../views/Wallet.vue";
import Document from "../views/Document.vue";
import RequestVerification from "../views/RequestVerification.vue";
import RequestPresentation from "../views/RequestPresentation.vue";
import SendPresentation from "../views/SendPresentation.vue";
import Credential from "../views/Credential.vue";
import Presentation from "../views/Presentation.vue";
import Partners from "../views/Partners.vue";
import Partner from "../views/Partner.vue";
import AddPartner from "../views/AddPartner.vue";
import AddPartnerbyURL from "../views/AddPartnerbyURL.vue";
import Settings from "../views/Settings.vue";
import TagManagement from "../views/TagManagement";
import SchemaSettings from "../views/SchemaSettings.vue";
import Schema from "../views/Schema.vue";
import AddSchema from "../views/AddSchema.vue";
import About from "../views/About.vue";
import CredentialManagement from "@/views/issuer/CredentialManagement.vue";
import Notifications from "@/views/Notifications.vue";
import PresentationRequestDetails from "@/views/PresentationRequestDetails.vue";
import ProofTemplates from "@/views/ProofTemplates";

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
    path: "/app/publicprofile",
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
    path: "/app/partners/:id/request",
    name: "RequestPresentation",
    component: RequestPresentation,
    props: true,
  },
  {
    path: "/app/presentation-request/:id/details",
    name: "PresentationRequestDetails",
    component: PresentationRequestDetails,
    props: true,
  },
  {
    path: "/app/partners/:id/send",
    name: "SendPresentation",
    component: SendPresentation,
    props: true,
  },
  {
    path: "/app/partners/:id/presentation/:presentationId",
    name: "Presentation",
    component: Presentation,
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
    path: "/app/credentialManagement",
    name: "CredentialManagement",
    component: CredentialManagement,
  },
  {
    path: "/app/notifications",
    name: "Notifications",
    component: Notifications,
  },
  {
    path: "/app/proofTemplates",
    name: "ProofTemplates",
    component: ProofTemplates,
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
