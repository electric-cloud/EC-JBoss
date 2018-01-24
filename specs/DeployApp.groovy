import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import Utils.UtilsHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class DeployApp extends PluginTestHelper {

    @Shared
    String procName = 'DeployApp'
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
    String hostNameMaster = EnvPropertiesHelper.getJbossDomainMasterHostname();


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
                        serverconfig         : '',
                        scriptphysicalpath   : '',
                        warphysicalpath      : '',
                        appname              : '',
                        runtimename          : '',
                        force                : '',
                        assignservergroups   : '',
                        assignallservergroups: '',
                        additional_options   : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createServerGroupModels()
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
//        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Unroll
    def "TestCase #id: #name"(
            String id,
            String name,
            // procedure params
            String warphysicalpath,
            String appname,
            String runtimename,
            String force,
            String assignservergroups,
            String assignallservergroups,
            String additional_options,
            // setup
            LinkedList<SetupService> setupServices,
            // check
            String expectedStatus,
            String expectedUpperStepSummaryRegex,
            String[] expectedLogRegexes,
            String expectedAppName,
            String expectedRuntimeName,
            String expectedContextRoot,
            LinkedList<CheckService> checkServices,
            // cleanup
            LinkedList<CleanupService> cleanupServices) {
        setup:
        for (SetupService setupService : setupServices) {
            setupService.setup()
        }

        when:
        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : warphysicalpath,
                appname              : appname,
                runtimename          : runtimename,
                force                : force,
                assignservergroups   : assignservergroups,
                assignallservergroups: assignallservergroups,
                additional_options   : additional_options
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == expectedStatus
        assert runProcedureJob.getUpperStepSummary() =~ expectedUpperStepSummaryRegex
        for (String expectedLogRegex : expectedLogRegexes) {
            assert runProcedureJob.getLogs() =~ expectedLogRegex
        }

        for (CheckService checkService : checkServices) {
            checkService.check()
        }

        cleanup:
        for (CleanupService cleanupService : cleanupServices) {
            cleanupService.cleanup()
        }

        where:
        id           | name                                                                                  | warphysicalpath         | appname           | runtimename       | force | assignservergroups                          | assignallservergroups | additional_options | setupServices                                                                                                                                                                            | expectedStatus | expectedUpperStepSummaryRegex                                                 | expectedLogRegexes                                                                                     | expectedAppName                      | expectedRuntimeName                          | expectedContextRoot                 | checkServices                                                                                                                                                                                         | cleanupServices
        "C84582"     | "Domain, DeployApp, 1st time, 1 server group, minimum params"                         | "/tmp/$id-app.war"      | ""                | ""                | ""    | "$serverGroup1"                             | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--server-groups=.*$assignservergroups"]            | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
        "C84612"     | "Domain, DeployApp, 1st time, 2 server groups, minimum params"                        | "/tmp/$id-app.war"      | ""                | ""                | ""    | "$serverGroup1,$serverGroup2"               | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--server-groups=.*$assignservergroups"]            | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
        "C111810"    | "Domain, DeployApp, 1st time, all server groups, minimum params"                      | "/tmp/$id-app.war"      | ""                | ""                | ""    | ""                                          | "1"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--all-server-groups"]                              | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
        "Csdjf"      | "Domain, DeployApp, 1st time, custom app name"                                        | "/tmp/$id-app.war"      | "$id-app-ABC.war" | ""                | ""    | "$serverGroup1"                             | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--name=.*$appname"]                                | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
        "Casnnnf"    | "Domain, DeployApp, 1st time, custom runtime name"                                    | "/tmp/$id-app.war"      | ""                | "$id-app-XYZ.war" | ""    | "$serverGroup1,$serverGroup2"               | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--runtime-name=.*$runtimename"]                    | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
        "Cqwewew"    | "Domain, DeployApp, 1st time, custom app name, custom runtime name"                   | "/tmp/$id-app.war"      | "$id-app-ABC.war" | "$id-app-XYZ.war" | ""    | ""                                          | "1"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--name=.*$appname.*--runtime-name=.*$runtimename"] | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
        "Cmmmw"      | "Domain, DeployApp, 1st time, custom app name without extension, custom runtime name" | "/tmp/$id-app.war"      | "$id-app-ABC"     | "$id-app-XYZ.war" | ""    | "$serverGroup1"                             | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | []                                                                                                     | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
        "Cxxx"       | "Domain, DeployApp, 1st time, both server groups and all server groups are specified" | "/tmp/$id-app.war"      | "$id-app.war"     | "$id-app-XYZ.war" | ""    | "$serverGroup1"                             | "1"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--all-server-groups"]                              | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
        "qj1"        | "Domain, Negative. DeployApp, 1st time, duplicated server groups"                     | "/tmp/$id-app.war"      | "$id-app.war"     | "$id-app.war"     | ""    | "$serverGroup1,$serverGroup2,$serverGroup1" | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "error"        | "Duplicate resource"                                                          | []                                                                                                     | null                                 | null                                         | null                                | []                                                                                                                                                                                                    | []
        "qj1as"      | "Domain, Negative. DeployApp, 1st time, non existing server group"                    | "/tmp/$id-app.war"      | "$id-app.war"     | "$id-app.war"     | ""    | "non-existing-server-group"                 | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath)]                                                                                                                      | "error"        | "does not exist"                                                              | []                                                                                                     | null                                 | null                                         | null                                | []                                                                                                                                                                                                    | []
        "wlfjwe"     | "Domain, DeployApp, app already deployed, force flag, minimum params"                 | "/tmp/$id-app.war"      | ""                | ""                | "1"   | ""                                          | "0"                   | ""                 | [new DownloadArtifactService(linkToSampleWarFile, warphysicalpath), new DeployDomainService(warphysicalpath, expectedAppName, "$id-old-runtime-name.war", [serverGroup1, serverGroup2])] | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--force"]                                          | getAppName(appname, warphysicalpath) | "$id-old-runtime-name.war"                   | "$id-old-runtime-name"              | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1, serverGroup2]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1, serverGroup2])] | [new UndeployAppService(expectedAppName)]
//        "wlsdffjwe"  | "Domain, DeployApp, app already deployed, force flag, server groups ignored"          | "/tmp/$id-app.war"      | ""                | ""                | "1"   | "$serverGroup1"                             | "0"                   | ""                 | []                                                                                                                                                                                       | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--force"]                                          | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
//        "wlfsdfsjwe" | "Domain, DeployApp, 1st time, force flag, all server groups ignored - just upload"    | "/tmp/$id-app.war"      | ""                | ""                | "1"   | ""                                          | "1"                   | ""                 | []                                                                                                                                                                                       | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*--force"]                                          | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
//        "sdfkksdmf"  | "Domain, Negative. DeployApp, app already deployed, no force flag"                    | "/tmp/$id-app.war"      | ""                | ""                | ""    | ""                                          | "1"                   | ""                 | []                                                                                                                                                                                       | "error"        | "already exists in the deployment repository"                                 | []                                                                                                     | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
//        "sdfln"      | "Domain, DeployApp, 1st time, whitespace in path"                                     | "/tmp/$id-app name.war" | ""                | ""                | ""    | "$serverGroup1"                             | "0"                   | ""                 | []                                                                                                                                                                                       | "success"      | "Application $appname \\($warphysicalpath\\) has been successfully deployed." | ["jboss-cli.*--command=.*deploy .*$warphysicalpath.*"]                                                 | getAppName(appname, warphysicalpath) | getRuntimeName(runtimename, expectedAppName) | getContextRoot(expectedRuntimeName) | [new AppDeployedToServerGroupsCliCheck(expectedAppName, expectedRuntimeName, [serverGroup1]), new AppDeployedToServerGroupsUrlCheck(expectedContextRoot, [serverGroup1])]                             | [new UndeployAppService(expectedAppName)]
        //todo: migrate othe test cases for Standalone to this format

    }

    ///////////////////////
    // setup services
    ///////////////////////
    interface SetupService {
        void setup()
    }

    class DownloadArtifactService implements SetupService {
        String url
        String path

        DownloadArtifactService(String url, String path) {
            this.url = url
            this.path = path
        }

        @Override
        void setup() {
            downloadArtifact(url, path)
        }
    }

    class DeployDomainService implements SetupService {
        String pathToFile
        String appName
        String runtimeName
        String[] serverGroups

        DeployDomainService(String pathToFile, String appName, String runtimeName, def serverGroups) {
            this.pathToFile = pathToFile
            this.appName = appName
            this.runtimeName = runtimeName
            this.serverGroups = serverGroups
        }

        @Override
        void setup() {
            runCliCommand(CliCommandsGeneratorHelper.deployToServerGroups(serverGroups, pathToFile, appName, runtimeName))
        }
    }

    ///////////////////////
    // check services
    ///////////////////////
    interface CheckService {
        void check()
    }

    class AppUploadedToContentRepoCliCheck implements CheckService {
        String appName
        String runtimeName

        AppUploadedToContentRepoCliCheck(String appName, String runtimeName) {
            this.appName = appName
            this.runtimeName = runtimeName
        }

        @Override
        void check() {
            def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result
            assert result.'name' == appName
            assert result.'runtime-name' == runtimeName
        }
    }

    class AppDeployedToServerGroupsCliCheck implements CheckService {
        String appName
        String runtimeName
        String[] serverGroups

        AppDeployedToServerGroupsCliCheck(String appName, String runtimeName, def serverGroups) {
            this.appName = appName
            this.runtimeName = runtimeName
            this.serverGroups = serverGroups
        }

        @Override
        void check() {
            for (String serverGroup : serverGroups) {
                def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnServerGroup(serverGroup, appName)).result
                assert result.'name' == appName
                assert result.'runtime-name' == runtimeName
            }
        }
    }

    class AppDeployedToServerGroupsUrlCheck implements CheckService {
        String contextRoot
        String[] serverGroups

        AppDeployedToServerGroupsUrlCheck(String contextRoot, def serverGroups) {
            this.contextRoot = contextRoot
            this.serverGroups = serverGroups
        }

        @Override
        void check() {
            for (String rootUrl : getExpectedRootUrls(serverGroups)) {
                String expectedUrl = "$rootUrl/$contextRoot"
                assert isUrlAvailable(expectedUrl)
            }
        }
    }

    class AppNotDeployedToServerGroupsCliCheck implements CheckService {
        String appName
        String runtimeName
        String[] serverGroups

        AppNotDeployedToServerGroupsCliCheck(String appName, String runtimeName, String[] serverGroups) {
            this.appName = appName
            this.runtimeName = runtimeName
            this.serverGroups = serverGroups
        }

        @Override
        void check() {
            for (String serverGroup : serverGroups) {
                def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerGroupInfo(serverGroup)).result
                assert result.containsKey('deployment') && (!result.'deployment' || !result.'deployment'.keySet().contains(appName))
            }
        }
    }

    ///////////////////////
    // cleanup services
    ///////////////////////
    interface CleanupService {
        void cleanup()
    }

    class UndeployAppService implements CleanupService {
        String appName

        UndeployAppService(String appName) {
            this.appName = appName
        }

        @Override
        void cleanup() {
            runCliCommand(CliCommandsGeneratorHelper.undeployFromAllRelevantServerGroups(appName))
        }
    }

    ///////////////////////
    // other utils
    ///////////////////////
    Set<String> getExpectedRootUrls(String[] serverGroupsWithApp) {
        Set<String> expectedRootUrls = new HashSet<String>();
        for (String serverGroup : serverGroupsWithApp) {
            for (ServerHelper server : getServerGroupModel(serverGroup).getServers()) {
                String hostname = "jboss"; // todo: change to EnvPropertiesHelper.getResourceHostname()
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

    static String getAppName(String appName, String pathToApp) {
        return (appName ? appName : new File(pathToApp).getName())
    }

    static String getRuntimeName(String runtimeName, String appName) {
        return (runtimeName ? runtimeName : appName)
    }

    static String getContextRoot(String runtimeName) {
        return UtilsHelper.stripExtension(runtimeName)
    }
}