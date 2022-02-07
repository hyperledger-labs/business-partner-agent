/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import localeEnglish from "@/locales/en.json";
import { mountWithI18n } from "@@/test-helper";
import ImportCredentialIndy from "@/components/schema-add/ImportCredentialIndy.vue";
import Vue from "vue";
import { Wrapper } from "@vue/test-utils";

describe("ImportCredentialIndy", () => {
  const testString = "test string";
  let wrapper: Wrapper<Vue>;

  beforeEach(() => {
    wrapper = mountWithI18n(ImportCredentialIndy, {});
  });

  test("should have 2 input fields and 2 buttons.", () => {
    const allTextInputs = wrapper.findAll("input[type='text']");
    const allButtons = wrapper.findAll("button");

    expect(allTextInputs.length).toBe(2);
    expect(allButtons.length).toBe(2);
  });

  test("should show a 'required' validation error on an empty schema ID input field after editing.", async () => {
    const validationMessages = wrapper.findAll(".v-messages");

    wrapper.findAll("input[type='text']").at(0).setValue(testString);
    await Vue.nextTick();

    expect(validationMessages.length).toBe(2);
    expect(validationMessages.at(0).text()).toEqual(
      localeEnglish.app.rules.required
    );
    expect(validationMessages.at(1).text().length).toBe(0);
  });

  test("should show a 'required' validation error on an empty schema label input field after editing.", async () => {
    const validationMessages = wrapper.findAll(".v-messages");

    wrapper.findAll("input[type='text']").at(1).setValue(testString);
    await Vue.nextTick();

    expect(validationMessages.length).toBe(2);
    expect(validationMessages.at(0).text().length).toBe(0);
    expect(validationMessages.at(1).text()).toEqual(
      localeEnglish.app.rules.required
    );
  });

  test("should have an enabled submit button when all mandatory fields are filled.", async () => {
    const allTextInputs = wrapper.findAll("input[type='text']");
    const submitButton = wrapper.findAll("button").at(1);

    expect(submitButton.find("[disabled]").exists()).toBe(true);

    allTextInputs.at(0).setValue(testString);
    await Vue.nextTick();

    expect(submitButton.find("[disabled]").exists()).toBe(true);

    allTextInputs.at(1).setValue(testString);
    await Vue.nextTick();

    expect(submitButton.find("[disabled]").exists()).toBe(false);
  });

  test("should turn the submit button disabled again when at least 1 mandatory field is cleared.", async () => {
    const allTextInputs = wrapper.findAll("input[type='text']");
    const submitButton = wrapper.findAll("button").at(1);

    expect(submitButton.find("[disabled]").exists()).toBe(true);

    allTextInputs.at(0).setValue(testString);
    allTextInputs.at(1).setValue(testString);
    await Vue.nextTick();

    expect(submitButton.find("[disabled]").exists()).toBe(false);

    allTextInputs.at(0).setValue("");
    await Vue.nextTick();

    expect(submitButton.find("[disabled]").exists()).toBe(true);
  });

  test("should emit a 'cancelled' event when clicking on the cancel button.", async () => {
    const allButtons = wrapper.findAll("button");

    allButtons.at(0).trigger("click");
    await Vue.nextTick();

    expect(wrapper.emitted("cancelled")).toBeTruthy();
    expect(wrapper.emitted("cancelled").length).toBe(1);
  });

  test(`should have '${localeEnglish.component.addSchema.schemaName}' and '${localeEnglish.component.addSchema.schemaId}' as text field titles (en).`, () => {
    const allTitles = wrapper.findAll(".v-list-item__title");

    expect(allTitles.at(0).text()).toEqual(
      `${localeEnglish.component.addSchema.schemaName}:`
    );
    expect(allTitles.at(1).text()).toEqual(
      `${localeEnglish.component.addSchema.schemaId}:`
    );
  });
});
