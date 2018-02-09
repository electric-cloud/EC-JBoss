import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*
import com.electriccloud.spec.*

class PluginTestHelper extends PluginSpockTestSupport {

    static def helperProjName = 'JBoss Helper Project'
    static def helperProcedureRunCustomCommand = 'RunCustomCommand'
    static def helperProcedureDownloadArtifact = 'DownloadArtifact'
    static def helperProcedureCheckUrl = 'CheckUrl'

    def createDefaultConfiguration(String configName, props = [:]) {
        String pluginName = "EC-JBoss"

        def username = EnvPropertiesHelper.getJbossUsername();
        def password = EnvPropertiesHelper.getJbossPassword();
        def jboss_url = EnvPropertiesHelper.getJbossControllerUrl();
        def jboss_cli_path = EnvPropertiesHelper.getJbossCliPath();
        def jboss_log_level = EnvPropertiesHelper.getJbossLogLevelValue();

        createPluginConfiguration(
                'EC-JBoss',
                configName,
                [jboss_url         : jboss_url,
                 scriptphysicalpath: jboss_cli_path,
                 java_opts         : '',
                 log_level         : jboss_log_level],
                username,
                password,
                props
        )
    }

    def createJBossResource() {
        def hostname = EnvPropertiesHelper.getResourceHostname()
        def port = EnvPropertiesHelper.getResourcePort()

        def resources = dsl "getResources()"
        logger.debug(objectToJson(resources))

        def resource = resources.resource.find {
            it.hostName == hostname && it.port == port
        }
        if (resource) {
            logger.debug("JBoss resource already exists")
            return resource.resourceName
        }
        logger.debug("Creating new JBoss resource")

        def result = dsl """
            createResource(
                resourceName: '${randomize("JBoss")}',
                hostName: '$hostname',
                port: '$port'
            )
        """

        logger.debug(objectToJson(result))
        def resName = result?.resource?.resourceName
        assert resName
        resName
    }

    def deleteProject(String projectName) {
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def redirectLogs(String parentProperty = '/myJob') {
        def propertyLogName = parentProperty + '/debug_logs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty",
                value: "$propertyLogName"
            )
        """
        return propertyLogName
    }

    def redirectLogsToPipeline() {
        def propertyName = '/myPipelineRuntime/debugLogs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty",
                value: "$propertyName"
            )
        """
        propertyName
    }

    String getJobLogs(def jobId) {
        assert jobId
        String property = "/myJob/debug_logs"
        String logs
        try {
            logs = getJobProperty(property, jobId)
        } catch (Throwable e) {
            logger.debug("cannot retrieve logs from the property '$property'")
        }
        return logs
    }

    def getPipelineLogs(flowRuntimeId) {
        assert flowRuntimeId
        getPipelineProperty('/myPipelineRuntime/debugLogs', flowRuntimeId)
    }

    def flexibleIgnore(name = null) {
        def ignoreVar = System.getenv('SPEC_CASE')
        println ignoreVar
        if (!ignoreVar) {
            return false
        }
        if (!name) {
            return true
        }
        return name =~ /$ignoreVar/ ? false : true
    }

    RunProcedureJob runProcedureDsl(String projectName, String procedureName, def parameters) {
        String parametersString = parameters.collect { k, v -> "$k: '$v'" }.join(', ')

        String dslString = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: '$procedureName',
                    actualParameter: [
                        $parametersString
                    ]
                )
        """

        redirectLogs()

        def result = dsl(dslString)
        String jobId = result.jobId
        assert jobId
        waitUntil {
            jobCompleted result
        }

        RunProcedureJob runProcedureJob = new RunProcedureJob(jobId, projectName, procedureName, parameters)
        String logs = runProcedureJob.getLogs()
        String status = runProcedureJob.getStatus()
        String upperStepSummary = runProcedureJob.getUpperStepSummary()
        String lowerStepSummary = runProcedureJob.getLowerStepSummary()

        logger.debug("============================")
        logger.debug("INFORMATION ABOUT JOB $jobId")
        logger.debug("DSL: $dslString")
        logger.debug("Status: " + (status ? status : "[undef]"))
        logger.debug("Upper step summary: " + (upperStepSummary ? upperStepSummary : "[undef]"))
        logger.debug("Lower step summary: " + (lowerStepSummary ? lowerStepSummary : "[undef]"))
        logger.debug("Logs: " + (logs ? logs : "[undef]"))
        logger.debug("============================")

        return runProcedureJob
    }

    def createHelperProject(resName, configName) {
        dslFile 'dsl/RunProcedure.dsl', [
                projName: helperProjName,
                resName : resName,
                procName: helperProcedureRunCustomCommand,
                params  : [
                        serverconfig      : configName,
                        scriptphysicalpath: '',
                        customCommand     : '',
                        propertyName      : '',
                        dumpFormat        : '',
                ]
        ]

        dslFile 'dsl/UtilProcedures.dsl', [
                projName: helperProjName,
                resName: resName,
                procNameDownloadArtifact: helperProcedureDownloadArtifact,
                procNameCheckUrl: helperProcedureCheckUrl
        ]
    }

    def runCliCommandAndGetJBossReply(String command) {
        return runCliCommand(command, "/myJob/RunCustomCommandResult")
                .getPropertiesUnderPropertySheet("/myJob/RunCustomCommandResult")
    }

    RunProcedureJob runCliCommand(String command, String jbossReplyPropertyName = "") {
        def runParams = [
                customCommand: command,
                propertyName : jbossReplyPropertyName,
                dumpFormat   : 'propertySheet'
        ]

        RunProcedureJob runProcedureJob = runProcedureDsl(helperProjName, helperProcedureRunCustomCommand, runParams)
        if (!runProcedureJob.isStatusSuccess()) {
            throw new Exception("Run CLI Command failed");
        }

        return runProcedureJob
    }

    def downloadArtifact(String sourceUrl, String targetPath) {
        def res = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedureDownloadArtifact',
                actualParameter: [
                    url: '$sourceUrl',
                    artifactPath: '$targetPath'
                ]
            )
        """
        assert res.jobId
        waitUntil {
            jobCompleted res
        }

        assert jobStatus(res.jobId).outcome == 'success'
    }

    boolean isUrlAvailable(String url) {
        def res = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedureCheckUrl',
                actualParameter: [
                    url: '$url'
                ]
            )
        """
        assert res.jobId
        waitUntil {
            jobCompleted res
        }

        return jobStatus(res.jobId).outcome == 'success'
    }

    boolean isNotUrlAvailable(String url) {
        def res = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedureCheckUrl',
                actualParameter: [
                    url: '$url'
                ]
            )
        """
        assert res.jobId
        waitUntil {
            jobCompleted res
        }

        return jobStatus(res.jobId).outcome == 'error'
    }

    class RunProcedureJob {
        private String jobId
        private String projectName
        private String procedureName
        private def runParams

        RunProcedureJob(String jobId, String projectName, String procedureName, def runParams) {
            this.jobId = jobId
            this.projectName = projectName
            this.procedureName = procedureName
            this.runParams = runParams
        }

        String getJobId() {
            return jobId
        }

        String getProjectName() {
            return projectName
        }

        String getProcedureName() {
            return procedureName
        }

        def getRunParams() {
            return runParams
        }

        String getLogs() {
            return getJobLogs(jobId)
        }

        String getStatus() {
            return jobStatus(jobId).outcome
        }

        String getUpperStepSummary() {
            String property = "/myJob/jobSteps/$procedureName/summary"
            String summary
            try {
                summary = getJobProperty(property, jobId)
            } catch (Throwable e) {
                logger.debug("cannot retrieve upper step summary from the property '$property'")
            }
            return summary
        }

        String getLowerStepSummary() {
            String property = "/myJob/jobSteps/$procedureName/jobSteps/$procedureName/summary"
            String summary
            try {
                summary = getJobProperty(property, jobId)
            } catch (Throwable e) {
                logger.debug("cannot retrieve lower step summary from the property '$property'")
            }
            return summary
        }

        Boolean isStatusSuccess() {
            return this.getStatus() == 'success'
        }

        Boolean isStatusWarning() {
            return this.getStatus() == 'warning'
        }

        Boolean isStatusError() {
            return this.getStatus() == 'error'
        }

        def getPropertiesUnderPropertySheet(String propertySheetName) {
            def props = getPropertiesRecursive(propertySheetName, this.getJobId(), { path, id ->
                def res = dsl """
                getProperties(path: '$path', jobId: '$id')
            """
                res?.propertySheet?.property
            })
            return props
        }
    }


}
