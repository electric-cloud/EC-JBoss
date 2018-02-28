import Services.CliCommandsGeneratorHelper
import spock.lang.*
import Utils.EnvPropertiesHelper

@IgnoreIf({ env.JBOSS_MODE == 'domain' })
class DeployApplicationStandalone extends PluginTestHelper {

    @Shared
    String procName = 'DeployApplication'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String linkToSampleWarFile = "https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war"


    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        additionalOptions              : '',
                        applicationContentSourcePath   : '',
                        deploymentName                 : '',
                        disabledServerGroups           : '',
                        enabledServerGroups            : '',
                        runtimeName                    : '',
                        serverconfig                   : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Unroll
    def "DeployApp, 1st time, file, minimum params  (C278099)"() {
        String testCaseId = "C278099"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }



    /*
todo: test common cases (config/pathToCli/wrongCreds)
 */

    void checkAppDeployedToStandaloneCli(String appName, String runtimeName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void checkAppDeployedToStandaloneUrl(String contextRoot) {
        checkAppDeployedToStandaloneUrl(contextRoot, true)
    }

    void checkAppDeployedToStandaloneUrl(String contextRoot, boolean available) {
        String hostname = EnvPropertiesHelper.getResourceHostname()
        String port = 8080
        String url = "http://$hostname:$port/$contextRoot"
        if (available)
            assert isUrlAvailable(url)
        else
            assert isNotUrlAvailable(url)
    }

    void undeployAppFromStandalone(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromStandalone(appName))
    }

    void deployAppToStandalone(String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToStandalone(filePath, appName, runtimeName))
    }
}