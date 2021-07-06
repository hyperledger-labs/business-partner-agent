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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hyperledger.bpa.controller.api.activity.ActivityItem;
import org.hyperledger.bpa.impl.ActivitiesManager;

import javax.inject.Inject;
import java.util.List;

@Controller("/api/activities")
@Tag(name = "Activities")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class ActivitiesController {

    @Inject
    ActivitiesManager activitiesManager;

    /**
     * List Items, if no filters return all
     *
     * @param activity Boolean Filter for activities
     * @param task     Boolean Filter for tasks
     * @return list of {@link ActivityItem}
     */
    @Get("/")
    public HttpResponse<List<ActivityItem>> listActivities(@Nullable @QueryValue Boolean activity,
            @Nullable @QueryValue Boolean task) {
        return HttpResponse.ok(activitiesManager.getItems(activity, task));
    }

}
