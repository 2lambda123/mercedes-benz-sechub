// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.pds.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mercedesbenz.sechub.commons.pds.PDSDefaultParameterKeyConstants;
import com.mercedesbenz.sechub.commons.pds.PDSDefaultParameterValueConstants;
import com.mercedesbenz.sechub.pds.PDSJSONConverterException;
import com.mercedesbenz.sechub.pds.PDSMustBeDocumented;
import com.mercedesbenz.sechub.pds.PDSShutdownService;

@Service
public class PDSServerConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(PDSServerConfigurationService.class);

    private static final String DEFAULT_PATH = "./pds-config.json";

    private static final int defaultMinutesToWaitForProduct = PDSDefaultParameterValueConstants.DEFAULT_MINUTES_TO_WAIT_FOR_PRODUCT;
    private static final int defaultMaxConfigurableMinutesToWaitForProduct = PDSDefaultParameterValueConstants.MAXIMUM_CONFIGURABLE_TIME_TO_WAIT_FOR_PRODUCT_IN_MINUTES;
    private static final int minimumConfigurableMinutesToWaitForProduct = PDSDefaultParameterValueConstants.MINIMUM_CONFIGURABLE_TIME_TO_WAIT_FOR_PRODUCT_IN_MINUTES;

    @PDSMustBeDocumented(value = "Define path to PDS configuration file", scope = "startup")
    @Value("${sechub.pds.config.file:" + DEFAULT_PATH + "}")
    String pathToConfigFile;

    @PDSMustBeDocumented(value = "Set maximum time a PDS will wait for a product before canceling execution automatically. This value can be overriden as a job parameter as well.", scope = "execution")
    @Value("${" + PDSDefaultParameterKeyConstants.PARAM_KEY_PDS_CONFIG_PRODUCT_TIMEOUT_MINUTES + ":" + defaultMinutesToWaitForProduct + "}")
    int minutesToWaitForProduct = defaultMinutesToWaitForProduct;

    @PDSMustBeDocumented(value = "Set maximum configurable time in minutes for parameter: `"
            + PDSDefaultParameterKeyConstants.PARAM_KEY_PDS_CONFIG_PRODUCT_TIMEOUT_MINUTES + "`. The minimum time is not configurable. It is a fixed value of "
            + minimumConfigurableMinutesToWaitForProduct + " minute(s).", scope = "execution")
    @Value("${" + PDSDefaultParameterKeyConstants.PARAM_KEY_PDS_CONFIG_PRODUCT_TIMEOUT_MAX_CONFIGURABLE_MINUTES + ":"
            + defaultMaxConfigurableMinutesToWaitForProduct + "}")
    int maximumConfigurableMinutesToWaitForProduct = defaultMaxConfigurableMinutesToWaitForProduct;

    @Autowired
    PDSShutdownService shutdownService;

    @Autowired
    PDSServerConfigurationValidator serverConfigurationValidator;

    private PDSServerConfiguration configuration;

    private String storageId;

    @PostConstruct
    protected void postConstruct() {
        Path p = Paths.get(pathToConfigFile);
        File file = p.toFile();
        if (file.exists()) {
            try {
                String json = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
                PDSServerConfiguration loadedConfiguration = PDSServerConfiguration.fromJSON(json);
                String message = serverConfigurationValidator.createValidationErrorMessage(loadedConfiguration);
                if (message == null) {
                    configuration = loadedConfiguration;
                } else {
                    LOG.error("configuration file '{}' not valid - reason: {}", file.getAbsolutePath(), message);
                }

            } catch (PDSJSONConverterException | IOException e) {
                LOG.error("no configuration available, because cannot read config file", e);
            }
        } else {
            LOG.error("No config file found at {} !", file.getAbsolutePath());
        }
        if (configuration == null) {
            LOG.error(
                    "PDS configuration failure\n*****************************\nCONFIG ERROR CANNOT START PDS\n*****************************\nNo configuration available (see former logs for reason), so cannot start PDS server - trigger shutdown to ensure application no longer alive");
            shutdownService.shutdownApplication();
        }
        /* define storage id */
        storageId = "pds/" + getServerId();

        handleSystemWideProductTimeOutSetting();

    }

    private void handleSystemWideProductTimeOutSetting() {
        if (minutesToWaitForProduct < minimumConfigurableMinutesToWaitForProduct) {
            LOG.warn("System wide minutesToWaitForProduct was defined as {}, which is less than minimum of {} minute. Will fallback to one minute!",
                    minutesToWaitForProduct, minimumConfigurableMinutesToWaitForProduct);
            minutesToWaitForProduct = minimumConfigurableMinutesToWaitForProduct;
        }

        if (minutesToWaitForProduct > maximumConfigurableMinutesToWaitForProduct) {
            LOG.warn("System wide minutesToWaitForProduct was defined as {}, which exceeds maximum. Will set maximum of {} as fallback!",
                    minutesToWaitForProduct, maximumConfigurableMinutesToWaitForProduct);
            minutesToWaitForProduct = maximumConfigurableMinutesToWaitForProduct;
        }
    }

    public PDSServerConfiguration getServerConfiguration() {
        return configuration;
    }

    public PDSProductSetup getProductSetupOrNull(String productId) {
        for (PDSProductSetup setup : configuration.getProducts()) {
            if (setup.getId().equals(productId)) {
                return setup;
            }
        }
        return null;
    }

    public String getServerId() {
        if (configuration == null) {
            return "undefined-no-configuration";
        }
        return configuration.getServerId();
    }

    /**
     * @return storage ID to use for this PDS server - is always "pds/${serverId}"
     */
    public String getStorageId() {
        return storageId;
    }

    /**
     * Returns the system wide configuration of minutes to wait for a product until
     * automatic cancellation will be done. The returned value will always be valid,
     * even when there is no dedicated configuration or a wrong configuration.
     *
     * @return system wide configuration of minutes to wait for a product
     */
    public int getMinutesToWaitForProduct() {
        return minutesToWaitForProduct;
    }

    public int getMaximumConfigurableMinutesToWaitForProduct() {
        return maximumConfigurableMinutesToWaitForProduct;
    }

    public int getMinimumConfigurableMinutesToWaitForProduct() {
        return minimumConfigurableMinutesToWaitForProduct;
    }
}
