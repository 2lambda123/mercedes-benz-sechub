// SPDX-License-Identifier: MIT

package cli

import (
	"flag"
	"fmt"
	"os"
	"reflect"
	"strconv"
	"time"

	"daimler.com/sechub/util"
	sechubUtil "daimler.com/sechub/util"
)

// Config for internal CLI calls
type Config struct {
	action             string
	apiToken           string
	configFilePath     string
	configFileRead     bool
	debug              bool
	file               string
	keepTempFiles      bool
	outputFolder       string
	projectID          string
	quiet              bool
	reportFormat       string
	secHubJobUUID      string
	server             string
	stopOnYellow       bool
	timeOutNanoseconds int64
	timeOutSeconds     int
	trustAll           bool
	user               string
	waitNanoseconds    int64
	waitSeconds        int
}

// Initialize Config with default values
var configFromInit Config = Config{
	configFilePath: DefaultSecHubConfigFile,
	reportFormat:   DefaultReportFormat,
	timeOutSeconds: DefaultTimeoutInSeconds,
	waitSeconds:    DefaultWaitTime,
}

var flagHelp bool
var flagVersion bool

var missingFieldHelpTexts = map[string]string{
	"server":         "Server URL is missing. Can be defined with option '-server' or in environment variable SECHUB_SERVER or in config file.",
	"user":           "User id is missing. Can be defined with option '-user' or in environment variable SECHUB_USERID or in config file.",
	"apiToken":       "API Token is missing. Can be defined in environment variable SECHUB_APITOKEN.",
	"projectID":      "Project id is missing. Can be defined with option '-project' or in config file.",
	"configFileRead": "Unable to read config file (defaults to 'sechub.json'). Config file is mandatory for this action.",
	"file":           "Input file name is not provided which is mandatory for this action. Can be defined with option '-file'.",
}

/* internal stuff - only necessary for development and testing*/
var ignoreDefaultExcludes = os.Getenv("SECHUB_IGNORE_DEFAULT_EXCLUDES") == "true" // make it possible to switch off default excludes

func init() {
	prepareOptionsFromCommandline(&configFromInit)
	parseConfigFromEnvironment(&configFromInit)
}

func prepareOptionsFromCommandline(config *Config) {
	flag.StringVar(&config.apiToken,
		"apitoken", config.apiToken, "The api token - Mandatory. Please try to avoid '-apitoken' parameter for security reasons. Use environment variable SECHUB_APITOKEN instead!")
	flag.StringVar(&config.configFilePath,
		"configfile", config.configFilePath, "Path to sechub config file")
	flag.StringVar(&config.file,
		"file", "", "Defines file to read from for actions '"+markFalsePositivesAction+"' or '"+interactiveMarkFalsePositivesAction+"' or '"+unmarkFalsePositivesAction+"'")
	flag.BoolVar(&flagHelp,
		"help", false, "Shows help and terminates")
	flag.StringVar(&config.secHubJobUUID,
		"jobUUID", "", "SecHub job uuid - Mandatory for actions '"+getStatusAction+"' or '"+getReportAction+"'")
	flag.StringVar(&config.outputFolder,
		"output", ".", "Output folder for reports etc.")
	flag.StringVar(&config.projectID,
		"project", config.projectID, "SecHub project id - Mandatory, but can also be defined in config file")
	flag.StringVar(&config.reportFormat,
		"reportformat", config.reportFormat, "Output format for reports, supported currently: [html,json].")
	flag.StringVar(&config.server,
		"server", config.server, "Server url of sechub server to use - e.g. 'https://sechub.example.com:8443'. Mandatory, but can also be defined in environment variable SECHUB_SERVER or in config file")
	flag.BoolVar(&config.stopOnYellow,
		"stop-on-yellow", config.stopOnYellow, "Makes a yellow traffic light in the scan also break the build")
	flag.IntVar(&config.timeOutSeconds,
		"timeout", config.timeOutSeconds, "Timeout for network communication in seconds.")
	flag.StringVar(&config.user,
		"user", config.user, "User id - Mandatory, but can also be defined in environment variable SECHUB_USERID or in config file")
	flag.BoolVar(&flagVersion,
		"version", false, "Shows version info and terminates")
	flag.IntVar(&config.waitSeconds,
		"wait", config.waitSeconds, "Wait time in seconds - Will be used for periodic status checks when action='"+scanAction+"'. Can also be defined in environment variable SECHUB_WAITTIME_DEFAULT")
}

func parseConfigFromEnvironment(config *Config) {
	apiTokenFromEnv :=
		os.Getenv("SECHUB_APITOKEN")
	config.debug =
		os.Getenv("SECHUB_DEBUG") == "true"
	config.keepTempFiles =
		os.Getenv("SECHUB_KEEP_TEMPFILES") == "true"
	config.quiet =
		os.Getenv("SECHUB_QUIET") == "true"
	serverFromEnv :=
		os.Getenv("SECHUB_SERVER")
	config.trustAll =
		os.Getenv("SECHUB_TRUSTALL") == "true"
	userFromEnv :=
		os.Getenv("SECHUB_USERID")
	waittimeFromEnv :=
		os.Getenv("SECHUB_WAITTIME_DEFAULT")

	if apiTokenFromEnv != "" {
		config.apiToken = apiTokenFromEnv
	}
	if serverFromEnv != "" {
		config.server = serverFromEnv
	}
	if userFromEnv != "" {
		config.user = userFromEnv
	}
	if waittimeFromEnv != "" {
		config.waitSeconds, _ = strconv.Atoi(waittimeFromEnv)
	}
}

// NewConfigByFlags parses commandline flags which override environment variable settings or defaults defined in init()
func NewConfigByFlags() *Config {
	// Parse command line options
	flag.Parse()

	// We use the configFromInit struct already prefilled with defaults and from environment variables
	oneSecond := 1 * time.Second
	configFromInit.waitNanoseconds = int64(configFromInit.waitSeconds) * oneSecond.Nanoseconds()
	configFromInit.timeOutNanoseconds = int64(configFromInit.timeOutSeconds) * oneSecond.Nanoseconds()

	if flagHelp {
		configFromInit.action = showHelpAction
	} else if flagVersion {
		configFromInit.action = showVersionAction
	} else {
		// read action from commandline
		configFromInit.action = flag.Arg(0)
	}

	return &configFromInit
}

func assertValidConfig(configPtr *Config) {
	if configPtr.trustAll {
		if !configPtr.quiet {
			sechubUtil.LogWarning("Configured to trust all - means unknown service certificate is accepted. Don't use this in production!")
		}
	}

	// -------------------------------------
	//  Define mandatory fields for actions
	// -------------------------------------
	checklist := map[string][]string{
		scanAction:                            {"server", "user", "apiToken", "projectID", "configFileRead"},
		scanAsynchronAction:                   {"server", "user", "apiToken", "projectID", "configFileRead"},
		getStatusAction:                       {"server", "user", "apiToken", "projectID", "secHubJobUUID"},
		getReportAction:                       {"server", "user", "apiToken", "projectID", "secHubJobUUID"},
		getFalsePositivesAction:               {"server", "user", "apiToken", "projectID"},
		markFalsePositivesAction:              {"server", "user", "apiToken", "projectID", "file"},
		unmarkFalsePositivesAction:            {"server", "user", "apiToken", "projectID", "file"},
		interactiveMarkFalsePositivesAction:   {"server", "user", "apiToken", "projectID"},
		interactiveUnmarkFalsePositivesAction: {"server", "user", "apiToken", "projectID"},
		showHelpAction:                        {},
		showVersionAction:                     {},
	}

	/* --------------------------------------------------
	 * 					Validation
	 * --------------------------------------------------
	 */
	if configPtr.action == "" {
		util.LogError("sechub action not set")
		showHelpHint()
		os.Exit(ExitCodeMissingParameter)
	}

	errorsFound := false
	if mandatoryFields, ok := checklist[configPtr.action]; ok {
		for _, fieldname := range mandatoryFields {
			if !isConfigFieldFilled(configPtr, fieldname) {
				errorsFound = true
			}
		}
	} else {
		util.LogError("Unknown action: '" + configPtr.action + "'")
		errorsFound = true
	}

	if errorsFound {
		showHelpHint()
		os.Exit(ExitCodeMissingParameter)
	}

	// For convenience: lowercase user id and project id if needed
	configPtr.user = lowercaseOrWarning(configPtr.user, "user id")
	configPtr.projectID = lowercaseOrWarning(configPtr.projectID, "project id")
}

// isConfigFieldFilled checks if field is not empty or is 'true' in case of boolean type
func isConfigFieldFilled(configPTR *Config, field string) bool {
	value := fmt.Sprintf("%v", reflect.ValueOf(configPTR).Elem().FieldByName(field))
	if value == "" || value == "false" {
		util.LogError(missingFieldHelpTexts[field])
		return false
	}
	return true
}
