/*
 Copyright (c) 2020 - for information on the respective copyright owner
 see the NOTICE file and/or the repository at
 https://github.com/hyperledger-labs/organizational-agent
 
 SPDX-License-Identifier: Apache-2.0
*/

import { getAllPartners } from "@/api/partners.api";
import { CredentialTypes } from "../../constants";
import {
  getPartner,
  refreshPartner,
  removePartner,
  updatePartner,
  lookupPartner,
} from "../../api/partners.api";

const state = {
  partners: [],
  lookedUpPartners: [],
  partnersLoading: true,
};

const _getPartnerProfile = (partner) => {
  if ({}.hasOwnProperty.call(partner, "credential")) {
    let profile = partner.credential.find((cred) => {
      return cred.type === CredentialTypes.PROFILE.name;
    });
    if (profile) {
      if ({}.hasOwnProperty.call(profile, "credentialData")) {
        return profile.credentialData;
      } else if ({}.hasOwnProperty.call(profile, "documentData")) {
        return profile.documentData;
      }
    }
  }
  return null;
};

const _getPartnerName = (partner) => {
  if (typeof partner !== "object") {
    return "";
  } else if ({}.hasOwnProperty.call(partner, "alias")) {
    return partner.alias;
  } else {
    let profile = _getPartnerProfile(partner);
    if (profile && {}.hasOwnProperty.call(profile, "legalName")) {
      return profile.legalName;
    } else if (partner.did) {
      return partner.did;
    } else {
      return partner.id;
    }
  }
};

const getters = {
  getPartners(state) {
    return state.partners;
  },
  isPartnersLoading(state) {
    return state.partnersLoading;
  },
  getPartnerProfile: (state) => (id) => {
    return _getPartnerProfile(
      state.partners.find((partner) => {
        return partner.id === id;
      })
    );
  },
  getPartnerName: (state) => (id) => {
    return _getPartnerName(
      state.partners.find((partner) => {
        return partner.id === id;
      })
    );
  },
  getPartnerByDid: (state) => (did) => {
    return state.partners.find((partner) => {
      return partner.did === did;
    });
  },
  getPartnerNameByDid: (state) => (did) => {
    return (
      _getPartnerName(
        state.partners.find((partner) => {
          return partner.did === did;
        })
      ) || did
    );
  },
  getPartner: (state) => (id) => {
    return state.partners.find((partner) => {
      return partner.id === id;
    });
  },
};

const mutations = {
  SET_PARTNERS(state, payload) {
    state.partners = payload;
  },
  SET_PARTNER(state, payload) {
    let index = state.partners.findIndex((partner) => {
      return partner.partnerId === payload.partnerId;
    });
    if (index > -1) {
      state.partners[index] = payload;
    } else {
      state.partners.push(payload);
    }
  },
  REMOVE_PARTNER(state, payload) {
    state.partners = state.partners.filter((partner) => {
      return partner.partnerId !== payload.partnerId;
    });
  },
  SET_lOOKUP_PARTNER(state, payload) {
    let index = state.lookedUpPartners.findIndex((partner) => {
      return partner.partnerId === payload.partnerId;
    });
    if (index > -1) {
      state.lookedUpPartners[index] = payload;
    } else {
      state.lookedUpPartners.push(payload);
    }
  },
  SET_PARTNERS_LOADING(state, payload) {
    state.partnersLoading = payload;
  },
};

const actions = {
  async fetchPartners({ commit }) {
    commit("SET_PARTNERS_LOADING", true);
    try {
      console.log("Getting partners");
      const res = await getAllPartners();

      // TODO: Check for unseen/new partners
      if (res.status === 200) {
        commit("SET_PARTNERS", res.data);
      }
      commit("SET_PARTNERS_LOADING", false);
    } catch (e) {
      console.error(e);
      commit("SET_PARTNERS_LOADING", false);
    }
  },
  async fetchPartner({ commit }, payload) {
    try {
      if (payload.partnerId) {
        const res = await getPartner(payload.partnerId);
        if (res.status === 200) {
          commit("SET_PARTNER", res.data);
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
  async updatePartnerName({ commit }, payload) {
    try {
      if (payload.partnerId && payload.alias) {
        const res = await updatePartner(payload.partnerId, {
          alias: payload.alias,
        });

        // TODO: On success, set alias in store
        if (res.status === 200) {
          commit();
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
  async removePartner({ commit }, payload) {
    try {
      if (payload.partnerId) {
        const res = await removePartner(payload.partnerId);
        if (res.status === 200) {
          commit("REMOVE_PARTNER", payload.partnerId);
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
  async lookupPartner({ commit }, payload) {
    try {
      if (payload.did) {
        const res = lookupPartner(payload.did);
        if (res.status === 200) {
          commit("SET_lOOKUP_PARTNER", res.data);
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
  async refreshPartner({ commit }, payload) {
    try {
      if (payload.partnerId) {
        const res = await refreshPartner(payload.partnerId);
        if (res.status === 200) {
          commit("SET_PARTNER", res.data);
        }
      }
    } catch (e) {
      console.error(e);
    }
  },
};

export default {
  state,
  mutations,
  actions,
  getters,
};
