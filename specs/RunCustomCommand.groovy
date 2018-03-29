import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

class RunCustomCommand extends PluginTestHelper {

    @Shared
    String procName = 'RunCustomCommand'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String linkToSampleWarFile = "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/versions/hello-world-war-version-1.war"
    @Shared
    String linkToSampleWarFile2 = "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/versions/hello-world-war-version-2.war"

    // 2 default server groups
    @Shared
    String serverGroup1 = "main-server-group"
    @Shared
    String serverGroup2 = "other-server-group"
    @Shared
    String serverGroup1Server1 = "server-one"
    @Shared
    String serverGroup1Server2 = "server-two"
    @Shared
    String serverGroup2Server1 = "server-three"

    @Shared
    ServerGroupHelper serverGroup1Model
    @Shared
    ServerGroupHelper serverGroup2Model

    @Shared
    String hostNameMaster = EnvPropertiesHelper.getJbossDomainMasterHostname()


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
                        additionalOptions           : '',
                        applicationContentSourcePath: '',
                        deploymentName              : '',
                        disabledServerGroups        : '',
                        enabledServerGroups         : '',
                        runtimeName                 : '',
                        serverconfig                : '',
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
    def "RunCustomCommand, performance"() {
        String testCaseId = "C278234"
        String existingAppName = "$testCaseId-app.war"
        setup:
        String runtimeName = "$testCaseId-app.war"
        String contextRoot = "$testCaseId-app"
        String[] oldServerGroupsWithApp = [serverGroup1]
        deployToServerGroups(oldServerGroupsWithApp, "--url=$linkToSampleWarFile", existingAppName, runtimeName)

        when:
        for (int i = 0; i<1000; i++){
        checkAppDeployedToServerGroupsCli(existingAppName, runtimeName, oldServerGroupsWithApp)
        }

        then:
        undeployFromAllRelevantServerGroups(existingAppName)
    }

    void checkAppDeployedToServerGroupsCli(String appName, String runtimeName, def serverGroups) { //not working for JBoss 6.4
        for (String serverGroup : serverGroups) {
            checkAppDeployedToServerGroupCli(appName, runtimeName, serverGroup)
        }
    }

    void checkAppDeployedToServerGroupCli(String appName, String runtimeName, String serverGroup) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnServerGroup(serverGroup, appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void deployToServerGroups(String[] serverGroups, String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToServerGroups(serverGroups, filePath, appName, runtimeName))
    }

    void undeployFromAllRelevantServerGroups(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromAllRelevantServerGroups(appName))
    }

}