import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class DeployApplicationDomain extends PluginTestHelper {

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
        createServerGroupModels()
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(serverGroup2Server1, hostNameMaster))
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
    def "DeployApplication, 1st time, file, enabled server group: 1 server group, minimum params (C278234)"() {
        String testCaseId = "C278234"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : "$serverGroup1",
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup1 server groups."

        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups(expectedAppName)
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

    void checkAppNotDeployedToServerGroups(String appName, String[] serverGroups) {
        for (String serverGroup : serverGroups) {
            checkAppNotDeployedToServerGroup(appName, serverGroup)
        }
    }

    void checkAppNotDeployedToServerGroup(String appName, String serverGroup) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerGroupInfo(serverGroup)).result
        assert result.containsKey('deployment') && (!result.'deployment' || !result.'deployment'.keySet().contains(appName))
    }

    void checkAppUploadedToContentRepo(String appName, String runtimeName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void checkAppDeployedToServerGroupsUrl(String contextRoot, def serverGroups) {
        for (String rootUrls : getExpectedRootUrls(serverGroups)) {
            String url = "$rootUrls/$contextRoot"
            assert isUrlAvailable(url)
        }
    }

    void undeployFromAllRelevantServerGroups(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromAllRelevantServerGroups(appName))
    }

    void deployToServerGroups(String[] serverGroups, String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToServerGroups(serverGroups, filePath, appName, runtimeName))
    }

    Set<String> getExpectedRootUrls(def serverGroupsWithApp) {
        Set<String> expectedRootUrls = new HashSet<String>()
        for (String serverGroup : serverGroupsWithApp) {
            for (ServerHelper server : getServerGroupModel(serverGroup).getServers()) {
                String hostname = EnvPropertiesHelper.getResourceHostname()
                String port = 8080 + server.getSocketBindingPortOffset()
                String expectedRootUrl = "http://$hostname:$port"
                expectedRootUrls.add(expectedRootUrl)
            }
        }
        return expectedRootUrls
    }

    void createServerGroupModels() {
        serverGroup1Model = new ServerGroupHelper(serverGroup1)
        serverGroup2Model = new ServerGroupHelper(serverGroup2)
        ServerHelper serverGroup1Server1Model = new ServerHelper(serverGroup1Server1, serverGroup1, hostNameMaster)
        serverGroup1Server1Model.setSocketBindingPortOffset(0)
        ServerHelper serverGroup1Server2Model = new ServerHelper(serverGroup1Server2, serverGroup1, hostNameMaster)
        serverGroup1Server2Model.setSocketBindingPortOffset(150)
        ServerHelper serverGroup2Server1Model = new ServerHelper(serverGroup2Server1, serverGroup2, hostNameMaster)
        serverGroup2Server1Model.setSocketBindingPortOffset(250)
        serverGroup1Model.addServer(serverGroup1Server1Model)
        serverGroup1Model.addServer(serverGroup1Server2Model)
        serverGroup2Model.addServer(serverGroup2Server1Model)
    }

    ServerGroupHelper getServerGroupModel(String serverGroupName) {
        switch (serverGroupName) {
            case serverGroup1: return serverGroup1Model
            case serverGroup2: return serverGroup2Model
            default: throw new Exception("Unknown server group")
        }
    }
}