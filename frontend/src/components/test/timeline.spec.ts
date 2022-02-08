/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import Vue from "vue";
import { capitalize, formatDateLong } from "@/filters";
import Timeline from "@/components/Timeline.vue";
import localeEnglish from "@/locales/en.json";
import localeGerman from "@/locales/de.json";
import { shallowMountWithI18n } from "@@/test-helper";

describe("Timeline", () => {
  test(`should have '${localeEnglish.component.timeline.title}' as title and no entries when rendering the timeline component without values.`, () => {
    const wrapper = shallowMountWithI18n(Timeline);

    expect(wrapper.text()).toEqual(localeEnglish.component.timeline.title);
  });

  test(`should have '${localeGerman.component.timeline.title}' as title after language change to German.`, async () => {
    const wrapper = shallowMountWithI18n(Timeline);

    wrapper.vm.$i18n.locale = "de";
    await Vue.nextTick();

    expect(wrapper.text()).toEqual(localeGerman.component.timeline.title);
  });

  test(`should display given entries in timeline.`, () => {
    const offerSentLabel = "offer_sent";
    const offerSentDate = 1_638_778_808_470;
    const problemLabel = "problem";
    const problemDate = 1_638_779_945_084;
    const problemText = "This is a test text.";

    const wrapper = shallowMountWithI18n(Timeline, {
      propsData: {
        timeEntries: [
          [offerSentLabel, offerSentDate, undefined],
          [problemLabel, problemDate, problemText],
        ],
      },
    });

    const allEntries = wrapper.findAll("v-timeline-item-stub");
    const firstTimelineEntry = allEntries.at(0).findAll("v-col-stub");
    const secondTimelineEntry = allEntries.at(1).findAll("v-col-stub");

    expect(allEntries.length).toEqual(2);
    expect(firstTimelineEntry.length).toEqual(2);
    expect(secondTimelineEntry.length).toEqual(2);

    expect(firstTimelineEntry.at(0).text()).toEqual(
      formatDateLong(offerSentDate)
    );
    expect(firstTimelineEntry.at(1).text()).toEqual(
      capitalize(offerSentLabel.replace("_", " "))
    );
    expect(firstTimelineEntry.at(1).find("div").text()).toHaveLength(0);

    expect(secondTimelineEntry.at(0).text()).toEqual(
      formatDateLong(problemDate)
    );
    expect(secondTimelineEntry.at(1).find("strong").text()).toEqual(
      capitalize(problemLabel.replace("_", " "))
    );
    expect(secondTimelineEntry.at(1).find("div").text()).toEqual(problemText);
  });
});
