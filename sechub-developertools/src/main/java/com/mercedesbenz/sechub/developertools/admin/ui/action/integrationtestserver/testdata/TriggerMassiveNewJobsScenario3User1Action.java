// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.developertools.admin.ui.action.integrationtestserver.testdata;

import java.awt.event.ActionEvent;
import java.util.UUID;

import com.mercedesbenz.sechub.developertools.admin.ui.UIContext;
import com.mercedesbenz.sechub.developertools.admin.ui.action.integrationtestserver.IntegrationTestAction;
import com.mercedesbenz.sechub.integrationtest.api.IntegrationTestMockMode;
import com.mercedesbenz.sechub.integrationtest.api.TestAPI;
import com.mercedesbenz.sechub.integrationtest.scenario3.Scenario3;

public class TriggerMassiveNewJobsScenario3User1Action extends IntegrationTestAction {
    private static final int MAXIMUM_WEB_SCANS_LONG_RUNNING = 20;
    private static final long serialVersionUID = 1L;

    public TriggerMassiveNewJobsScenario3User1Action(UIContext context) {
        super("Trigger massive new scan jobs (Scenario3)", context);
    }

    @Override
    protected void executeImplAfterRestHelperSwitched(ActionEvent e) {
        for (int i = 0; i < MAXIMUM_WEB_SCANS_LONG_RUNNING; i++) {
            UUID uuid = TestAPI.as(Scenario3.USER_1).createWebScan(Scenario3.PROJECT_1, IntegrationTestMockMode.WEBSCAN__NETSPARKER_GREEN__10_SECONDS_WAITING);
            outputAsTextOnSuccess("Long running web scan job created:" + uuid);
            TestAPI.as(Scenario3.USER_1).approveJob(Scenario3.PROJECT_1, uuid);
            outputAsTextOnSuccess("job approved:" + uuid);
        }
    }

}
