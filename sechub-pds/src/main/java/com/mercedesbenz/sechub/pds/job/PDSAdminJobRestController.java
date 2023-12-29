// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.pds.job;

import com.mercedesbenz.sechub.pds.PDSAPIConstants;
import com.mercedesbenz.sechub.pds.security.PDSRoleConstants;
import com.mercedesbenz.sechub.pds.usecase.PDSStep;
import com.mercedesbenz.sechub.pds.usecase.UseCaseAdminFetchesJobMetaData;
import com.mercedesbenz.sechub.pds.usecase.UseCaseAdminFetchesJobResultOrFailureText;
import java.util.UUID;
import javax.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST API for PDS jobs
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(PDSAPIConstants.API_ADMIN)
@RolesAllowed({PDSRoleConstants.ROLE_SUPERADMIN})
public class PDSAdminJobRestController {

  @Autowired private PDSGetJobResultService jobResultService;

  @Autowired
  private PDSGetJobExecutionDataContentService jobStreamContentService;

  /* @formatter:off */
  @Validated
  @RequestMapping(path = "job/{jobUUID}/result", method = RequestMethod.GET)
  @UseCaseAdminFetchesJobResultOrFailureText(@PDSStep(
      name = "rest call",
      description = "an admin fetches result or failure text for job from db.",
      number = 1))
  public String
  getJobResultOrFailureText(@PathVariable("jobUUID") UUID jobUUID) {
    /* @formatter:on */
    return jobResultService.getJobResultOrFailureText(jobUUID);
  }

  /* @formatter:off */
  @Validated
  @RequestMapping(path = "job/{jobUUID}/metadata", method = RequestMethod.GET,
                  produces = MediaType.TEXT_PLAIN_VALUE)
  @UseCaseAdminFetchesJobMetaData(@PDSStep(
      name = "rest call",
      description = "an admin fetches meta data text or null.", number = 1))
  public String
  getJobMetaDataContentAsText(@PathVariable("jobUUID") UUID jobUUID) {
    /* @formatter:on */
    return jobStreamContentService.getJobMetaDataTextOrNull(jobUUID);
  }
}
