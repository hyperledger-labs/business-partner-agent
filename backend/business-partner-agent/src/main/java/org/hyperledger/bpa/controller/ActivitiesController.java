/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.RequestBean;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.bpa.controller.api.activity.ActivityItem;
import org.hyperledger.bpa.controller.api.activity.ActivitySearchParameters;
import org.hyperledger.bpa.impl.ActivityManager;

import javax.validation.Valid;
import java.util.List;

@Controller("/api/activities")
@Tag(name = "Activities")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ActivitiesController {

    @Inject
    ActivityManager activityManager;

    /**
     * List Items, if no filters return all
     *
     * @param parameters ActivitySearchParameters Filters for list
     * @return list of {@link ActivityItem}
     */
    @Get
    public HttpResponse<List<ActivityItem>> listActivities(@RequestBean @Valid ActivitySearchParameters parameters) {
        return HttpResponse.ok(activityManager.getItems(parameters));
    }

}
