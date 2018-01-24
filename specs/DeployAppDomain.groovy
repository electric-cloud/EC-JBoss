import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class DeployAppDomain extends PluginTestHelper {

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
    def "DeployApp, 1st time, 1 server group, minimum params (C84582)"() {
        String testCaseId = "C84582"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "$serverGroup1", // deploy to one server group
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--server-groups=.*${runParams.assignservergroups}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, 2 server groups, minimum params (C84612)"() {
        String testCaseId = "C84612"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "$serverGroup1,$serverGroup2", // deploy to two server groups
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--server-groups=.*${runParams.assignservergroups}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        String[] expectedServerGroupsWithApp = [serverGroup1, serverGroup2]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, all server groups, minimum params (C111810)"() {
        String testCaseId = "C111810"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "1", // deploy to all server groups
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--all-server-groups"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        String[] expectedServerGroupsWithApp = [serverGroup1, serverGroup2]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name (xxx123123123)"() {
        String testCaseId = "xxx123123123"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname.war", // custom app name
                runtimename          : "",
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}"

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-appname.war"
        String expectedContextRoot = "$testCaseId-app-custom-appname"
        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app-custom-appname.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom runtime name (sklfklsnfd)"() {
        String testCaseId = "sklfklsnfd"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "$testCaseId-app-custom-runtimename.war", // custom runtime name
                force                : "",
                assignservergroups   : "$serverGroup1,$serverGroup2",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        String[] expectedServerGroupsWithApp = [serverGroup1, serverGroup2]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name, custom runtime name (kwejfiojscd)"() {
        String testCaseId = "kwejfiojscd"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname.war", // custom app name
                runtimename          : "$testCaseId-app-custom-runtimename.war", // and custom runtime name
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "1",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        String[] expectedServerGroupsWithApp = [serverGroup1, serverGroup2]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app-custom-appname.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name without extension, custom runtime name (sdfnsdfnsdf)"() {
        String testCaseId = "sdfnsdfnsdf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname", // no extension here
                runtimename          : "$testCaseId-app-custom-runtimename.war", // here we have usual extension .war
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app-custom-appname"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app-custom-appname")
    }

    @Unroll
    def "DeployApp, 1st time, both server groups and all server groups are specified (nfjsdnf)"() {
        String testCaseId = "nfjsdnf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-custom-runtimename.war",
                force                : "",
                assignservergroups   : "$serverGroup1", // this param to be ignored
                assignallservergroups: "1", // when this one is specified (so deploy to all server groups will be performed)
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--all-server-groups"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        String[] expectedServerGroupsWithApp = [serverGroup1, serverGroup2]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "Negative. DeployApp, 1st time, duplicated server groups (sdfnsdfnss)"() {
        String testCaseId = "sdfnsdfnss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1,$serverGroup2,$serverGroup1", // duplicated server groups here
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Duplicate resource"
    }

    @Unroll
    def "Negative. DeployApp, non existing server group (sdfsdfsdfdssdf)"() {
        String testCaseId = "sdfsdfsdfdssdf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "non-existing-server-group", // non existing server group
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "does not exist"
    }

    @Unroll
    def "DeployApp, app already deployed, force flag (sdfsdfdddss)"() {
        String testCaseId = "sdfsdfdddss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-new-runtimename.war",
                force                : "1", // force flag
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        String[] oldServerGroupsWithApp = [serverGroup1, serverGroup2]
        deployToServerGroups(oldServerGroupsWithApp, runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--force"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToServerGroupsCli(existingAppName, newRuntimeName, oldServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(newContextRoot, oldServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, server groups ignored (sdjfnjsdnfnna)"() {
        String testCaseId = "sdjfnjsdnfnna"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-new-runtimename.war",
                force                : "1", // when force is set
                assignservergroups   : "$serverGroup1", // this param to be ignored
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        String[] oldServerGroupsWithApp = [serverGroup2]
        deployToServerGroups(oldServerGroupsWithApp, runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--force"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToServerGroupsCli(existingAppName, newRuntimeName, oldServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(newContextRoot, oldServerGroupsWithApp)

        // let's verify that assignservergroups was ignored
        String[] expectedServerGroupsWithoutApp = [serverGroup1]
        checkAppNotDeployedToServerGroups(existingAppName, expectedServerGroupsWithoutApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    def "DeployApp, 1st time, force flag, all server groups ignored - just upload (snwejjejjejj)"() {
        String testCaseId = "snwejjejjejj"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "1", // when force is set
                assignservergroups   : "",
                assignallservergroups: "1", // this param to be ignored
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--force"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

        // let's verify that assignallservergroups was ignored
        String[] expectedServerGroupsWithoutApp = [serverGroup1, serverGroup2]
        checkAppNotDeployedToServerGroups(expectedAppName, expectedServerGroupsWithoutApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    def "Negative. DeployApp, app already deployed, no force flag (kkk)"() {
        String testCaseId = "kkk"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "0", // when force is not set, but app exists
                assignservergroups   : "",
                assignallservergroups: "1", // this will not help
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        String oldContextRoot = "$testCaseId-app-old-runtimename"
        String[] oldServerGroupsWithApp = [serverGroup2]
        deployToServerGroups(oldServerGroupsWithApp, runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "'$existingAppName' already exists in the deployment repository"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*"

        // let's check that app was not upgraded and runtime name was not changed
        checkAppDeployedToServerGroupsCli(existingAppName, oldRuntimeName, oldServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(oldContextRoot, oldServerGroupsWithApp)

        // let's verify that assignallservergroups was not applied
        String[] expectedServerGroupsWithoutApp = [serverGroup1]
        checkAppNotDeployedToServerGroups(existingAppName, expectedServerGroupsWithoutApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, whitespace in path (lldlld)"() {
        String testCaseId = "lldlld"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app with whitespace.war", // whitespaces in path
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--server-groups=.*${runParams.assignservergroups}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "Negative. DeployApp, incorrect param, undef required param, path to app (sdfnsdfnss)"() {
        String testCaseId = "sdfnsdfnss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "", // required param not provided
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        //todo: check runProcedureJob.getUpperStepSummary()
    }

    @Unroll
    def "Negative. DeployApp, non existing filepath (sdfnsdfnss)"() {
        String testCaseId = "sdfnsdfnss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/non-existing-file.war", // non existing file
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        //todo: check runProcedureJob.getUpperStepSummary()
    }

    @Unroll
    def "DeployApp, 1st time, 1 server group in additional options (asss)"() {
        String testCaseId = "asss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : " --server-groups=$serverGroup1" // deploy to one server group
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*${runParams.additional_options}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, disabled flag in additional options (aaaaaa)"() {
        String testCaseId = "aaaaaa"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : " --disabled" // disabled flag (upload to content repo)
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*${runParams.additional_options}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, force flag in additional options (sdfsdfdddss)"() {
        String testCaseId = "sdfsdfdddss"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-new-runtimename.war",
                force                : "0",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : " --force" // force flag
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        String[] oldServerGroupsWithApp = [serverGroup1, serverGroup2]
        deployToServerGroups(oldServerGroupsWithApp, runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*${runParams.additional_options}"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToServerGroupsCli(existingAppName, newRuntimeName, oldServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(newContextRoot, oldServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups("$testCaseId-app.war")
    }

    @Unroll
    def "Negative. DeployApp, additional options conflicts with defined params (sdfsdfsdfdssdf)"() {
        String testCaseId = "sdfsdfsdfdssdf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1", // this one
                assignallservergroups: "0",
                additional_options   : "--all-server-groups" // vs this one
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        //todo: runProcedureJob.getUpperStepSummary()
    }

    @Unroll
    def "Negative. DeployApp, wrong additional options (sdfsdfsdfdssdf)"() {
        String testCaseId = "sdfsdfsdfdssdf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "$serverGroup1",
                assignallservergroups: "0",
                additional_options   : "--some-wrong-param" // wrong param
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        //todo: runProcedureJob.getUpperStepSummary()
    }

    /*
    todo: test common cases (config/pathToCli/wrongCreds)
    todo: test deploy of txt files instead of jars
    todo: test incorrect runtimeName/appName/serverGroup values, e.g. too long or spec chars
     */

    void checkAppDeployedToServerGroupsCli(String appName, String runtimeName, def serverGroups) {
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
}