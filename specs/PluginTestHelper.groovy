import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class PluginTestHelper extends PluginSpockTestSupport {

    static def helperProjName = 'JBoss Helper Project'
    static def helperProcedureRunCustomCommand = 'RunCustomCommand'
    static def helperProcedureDownloadArtifact = 'DownloadArtifact'
    static def helperProcedureCheckUrl = 'CheckUrl'
    static def helperProcedureMkdir = 'Mkdir'

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

        def workspaceName = randomize("JBoss")
        def agentDrivePath = EnvPropertiesHelper.getOS() == "WINDOWS" ? 'c:/tmp/workspace' : '/tmp'
        def agentUncPath = EnvPropertiesHelper.getOS() == "WINDOWS" ? 'c:\\\\tmp\\\\workspace' : '/tmp'
        def agentUnixPath = EnvPropertiesHelper.getOS() == "WINDOWS" ? '' : "/opt/electriccloud/electriccommander/workspace"
           
           def workspaceResult = dsl """
            createWorkspace(
                workspaceName: '${workspaceName}',
                agentDrivePath: '${agentDrivePath}',
                agentUncPath: '${agentUncPath}',
                agentUnixPath: '${agentUnixPath}',
                local: '1'
            )
        """

        logger.debug(objectToJson(workspaceResult))


        def result = dsl """
            createResource(
                resourceName: '${randomize("JBoss")}',
                hostName: '$hostname',
                port: '$port',
                workspaceName: '$workspaceName'
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

    def createCredential(String projectName, String credName, String userName, String password) {
        def credentialResult = dsl """
            createCredential(
                projectName: '${projectName}',
                credentialName: '${credName}',
                userName: '${userName}',
                password: '${password}',
                description: '',
                passwordRecoveryAllowed: 'true'
            )
        """

        logger.debug(objectToJson(credentialResult))
    }

    def attachCredential(String projectName, String credName, String procName) {
        def credentialResult = dsl """
            attachCredential(
                projectName: '${projectName}',
                credentialName: '${credName}',
                procedureName: '${procName}',
                processStepName: '${procName}',
                stepName: '${procName}'
                
            )
        """

        logger.debug(objectToJson(credentialResult))
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
        runProcedureDsl(projectName, procedureName, parameters, '')
    }

    RunProcedureJob runProcedureDsl(String projectName, String procedureName, def parameters, def credential) {
        String parametersString = parameters.collect { k, v -> "$k: '$v'" }.join(', ')
        String dslString = ''
        if(procedureName == "CreateConfiguration"){
        String credentialString = credential.collect { k, v -> "$k: '$v'" }.join(', ')
        dslString = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: '$procedureName',
                    credential: [
                       $credentialString
                    ],
                    actualParameter: [
                        $parametersString
                    ]
                )
        """
        }
            else if(procedureName == "CreateOrUpdateXADataSource"){
            String credentialString = credential.collect { k, v -> "$k: '$v'" }.join(', ')
            dslString = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: '$procedureName',
                    actualParameter: [
                        $parametersString
                    ],
                    credential: [
                        $credentialString
                    ]
                )
        """
        }

        else {
            dslString = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: '$procedureName',
                    actualParameter: [
                        $parametersString
                    ]
                )
        """
        }

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
                ]
        ]

        dslFile 'dsl/UtilProcedures.dsl', [
                projName: helperProjName,
                resName: resName,
                procNameDownloadArtifact: helperProcedureDownloadArtifact,
                procNameCheckUrl: helperProcedureCheckUrl,
                procNameMkdir: helperProcedureMkdir
        ]
    }

    def runCliCommandAndGetJBossReply(String command) {
        RunProcedureJob runProcedureJob =  runCliCommand(command)

        def commandsHistoryString = getJobProperty('/myJob/jobSteps/RunCustomCommand/commands_history', runProcedureJob.getJobId());
        logger.info("RunCustomCommand commands_history: $commandsHistoryString")

        def jsonSlurper = new JsonSlurper()

        def commandsHistoryObject = jsonSlurper.parseText(commandsHistoryString)
        assert commandsHistoryObject instanceof List

        def stdoutObject;
        for (def element: commandsHistoryObject) {
            def stdoutString = element.result.stdout
            logger.info("RunCustomCommand commands_history result stdout before replacing: $stdoutString")
            stdoutString = stdoutString.replaceAll(/(?s)"hash" => bytes \{.*?\}/, /"hash" => "skipped by system test"/);
            stdoutString = stdoutString.replaceAll(/(?m)( => )(\d+L)(,?)$/, /$1"$2"$3/)
            stdoutString = stdoutString.replace(" => undefined", " => null")
            stdoutString = stdoutString.replace(" => ", ":")
            logger.info("RunCustomCommand commands_history result stdout after replacing: $stdoutString")
            stdoutObject = jsonSlurper.parseText(stdoutString)
            assert stdoutObject instanceof Map
        }
        return stdoutObject
    }

    RunProcedureJob runCliCommand(String command) {
        def runParams = [
                customCommand: command
        ]

        RunProcedureJob runProcedureJob = runProcedureDsl(helperProjName, helperProcedureRunCustomCommand, runParams)
        if (runProcedureJob.isStatusError()) {
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

    def createDir(dirName) {
        def shell = EnvPropertiesHelper.getOS() == "WINDOWS" ? 'powershell' : 'bash'
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedureMkdir',
                actualParameter: [
                    directory: '$dirName'
                ]
            )
        """

        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
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

    def checkVersionApplication(String url, String version){ //for parse web page
        String data = new URL(url).getText().replaceAll("\t", "").replaceAll("\\s+", "")
        assert data =~ "Theversionis$version.0.0"
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
