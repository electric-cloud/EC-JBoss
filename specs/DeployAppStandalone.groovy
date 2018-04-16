import Services.CliCommandsGeneratorHelper
import spock.lang.*
import Utils.EnvPropertiesHelper

@Requires({ env.JBOSS_MODE == 'standalone' })
class DeployAppStandalone extends PluginTestHelper {

    @Shared
    String procName = 'DeployApp'
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

    static String getPathApp(){
        String warphysicalpath = "/tmp/"
        EnvPropertiesHelper.getOS() == "WINDOWS" ? warphysicalpath = "C:\\\\tmp\\\\" : warphysicalpath
        return  warphysicalpath
    }

    static String getPathAppLogs() {
        String warphysicalpath = "/tmp/"
        EnvPropertiesHelper.getOS() == "WINDOWS" ? warphysicalpath = "C:.*tmp.*" : warphysicalpath
        return  warphysicalpath
    }

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
    def "DeployApp, 1st time, minimum params (C111844)"() {
        String testCaseId = "C111844"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, server groups ignored (C277822)"() {
        String testCaseId = "C277822"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "some-group", // to be ignored
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, all server groups ignored (C277823)"() {
        String testCaseId = "C277823"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "1", // to be ignored
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name (C277824)"() {
        String testCaseId = "C277824"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname.war", // custom app name
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}"

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-appname.war"
        String expectedContextRoot = "$testCaseId-app-custom-appname"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom runtime name (C277825)"() {
        String testCaseId = "C277825"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "$testCaseId-app-custom-runtimename.war", // custom runtime name
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"

        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--runtime-name=.*${runParams.runtimename}"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name, custom runtime name (C277826)"() {
        String testCaseId = "C277826"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname.war", // custom app name
                runtimename          : "$testCaseId-app-custom-runtimename.war", // and custom runtime name
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname.war")
    }

    @Unroll
    def "DeployApp, 1st time, custom app name without extension, custom runtime name (C277827)"() {
        String testCaseId = "C277827"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app-custom-appname", // no extension here
                runtimename          : "$testCaseId-app-custom-runtimename.war", // here we have usual extension .war
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app-custom-appname"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname")
    }

    @Unroll
    def "DeployApp, 1st time, both server groups and all server groups ignored (C277828)"() {
        String testCaseId = "C277828"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-custom-runtimename.war",
                force                : "",
                assignservergroups   : "some-group", // to be ignored
                assignallservergroups: "1", // to be ignored
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, force flag, update app (C277829)"() {
        String testCaseId = "C277829"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "1", // force flag
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        deployAppToStandalone(runParams.warphysicalpath, "$testCaseId-app.war", "$testCaseId-app.war")
        downloadArtifact(linkToSampleWarFile2, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--force"

        String existingAppName = "$testCaseId-app.war"
        String newRuntimeName = "$testCaseId-app.war"
        String newContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, force flag, update app, server groups and all server groups ignored (C277831)"() {
        String testCaseId = "C277831"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "1", // force flag
                assignservergroups   : "some-group", // to be ignored as usual for standalone
                assignallservergroups: "1", // to be ignored as usual for standalone
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)
        String existingAppName = "$testCaseId-app.war"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, "$testCaseId-app.war")
        downloadArtifact(linkToSampleWarFile2, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--force"

        String newRuntimeName = "$testCaseId-app.war"
        String newContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    def "Negative. DeployApp, app already deployed, no force flag, update app failed (C277833)"() {
        String testCaseId = "C277833"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "$testCaseId-app.war",
                force                : "0", // when force is not set, but app exists
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)
        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app.war"
        String oldContextRoot = "$testCaseId-app"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, oldRuntimeName)
        downloadArtifact(linkToSampleWarFile2, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "'$existingAppName' already exists in the deployment repository"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath

        checkAppDeployedToStandaloneCli(existingAppName, oldRuntimeName)
        checkAppDeployedToStandaloneUrl(oldContextRoot, "1", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, whitespace in path (C277834)"() {
        String testCaseId = "C277834"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app with whitespace.war", // whitespaces in path
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app with whitespace.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }


    @Unroll
    def "Negative. DeployApp, non existing filepath (C277836)"() {
        String testCaseId = "C277836"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"non-existing-file.war", // non existing file
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        String expectedPath = getPathAppLogs()+"non-existing-file.war"
        assert runProcedureJob.getUpperStepSummary() =~ "File '"+expectedPath+"' doesn't exists"
        assert runProcedureJob.getLogs() =~ "read-attribute\\(name=launch-type\\)"
    }

    @Unroll
    def "DeployApp, 1st time, disabled flag in additional options (C277837)"() {
        String testCaseId = "C277837"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--disabled" // disabled flag
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedRuntimeName, "", false)

        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*${runParams.additional_options}"

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, force flag (in additional options), update app (C277838)"() {
        String testCaseId = "C277838"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "0",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--force" // force flag
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)
        String existingAppName = "$testCaseId-app.war"
        String runtimeName = "$testCaseId-app.war"
        String contextRoot = "$testCaseId-app"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, runtimeName)
        downloadArtifact(linkToSampleWarFile2, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*${runParams.additional_options}"

        checkAppDeployedToStandaloneCli(existingAppName, runtimeName)
        checkAppDeployedToStandaloneUrl(contextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, additional options conflicts with defined params (C277839)"() {
        String testCaseId = "C277839"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "1", // this one
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--force" // vs this one
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String runtimeName = "$testCaseId-app.war"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, runtimeName)

        downloadArtifact(linkToSampleWarFile2, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '${runParams.warphysicalpath}'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}.*${runParams.additional_options}.*--force"

        String contextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(existingAppName, runtimeName)
        checkAppDeployedToStandaloneUrl(contextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "Negative. DeployApp, wrong additional options (C277840)"() {
        String testCaseId = "C277840"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : getPathApp()+"$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--some-wrong-param" // wrong param
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        String expectedPath = getPathAppLogs()+"$testCaseId-app.war"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized arguments: [--some-wrong-param]"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*"+expectedPath+".*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}.*${runParams.additional_options}"

    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "DeployApp, 1st time, application content source path --url(C278037)"() {
        String testCaseId = "C278037"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "--url=$linkToSampleWarFile",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '$linkToSampleWarFile'"
        assert runProcedureJob.getLogs() =~ "Source with deployment is URL \\(such option available for EAP 7 and later versions\\): '--url=$linkToSampleWarFile'"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "DeployApp, app already deployed, force flag, application content source path --url(C278039)"() {
        String testCaseId = "C278039"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "--url=$linkToSampleWarFile2",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "1",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]
        setup:
        downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")
        String existingAppName = "$testCaseId-app.war"
        String runtimeName = "$testCaseId-app.war"
        deployAppToStandalone(getPathApp()+"$testCaseId-app.war", existingAppName, runtimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '$linkToSampleWarFile2'"
        assert runProcedureJob.getLogs() =~ "Source with deployment is URL \\(such option available for EAP 7 and later versions\\): '--url=$linkToSampleWarFile2'"

        String expectedContextRoot = "$testCaseId-app"

        checkAppDeployedToStandaloneCli(existingAppName, runtimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "DeployApp, app already deployed, force flag (in additional options), application content source path --url (C278041)"() {
        String testCaseId = "C278041"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "--url=$linkToSampleWarFile2",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--force"
        ]
        setup:
        downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")
        String existingAppName = "$testCaseId-app.war"
        String runtimeName = "$testCaseId-app.war"
        deployAppToStandalone(getPathApp()+"$testCaseId-app.war", existingAppName, runtimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '${runParams.appname}' has been successfully deployed from '$linkToSampleWarFile2'"
        assert runProcedureJob.getLogs() =~ "Source with deployment is URL \\(such option available for EAP 7 and later versions\\): '--url=$linkToSampleWarFile2'"

        String expectedContextRoot = "$testCaseId-app"

        checkAppDeployedToStandaloneCli(existingAppName, runtimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot, "2", true)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "Negative. DeployApp, 1st time, application content source path --url incorrect value (C278038)"() {
        String testCaseId = "C278038"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "--url=https://github.com/electric-cloud/incorrect-path/hello-world.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app.war",
                force                : "",
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Cannot create input stream from URL 'https://github.com/electric-cloud/incorrect-path/hello-world.war'"
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}.*${runParams.additional_options}"

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
        checkAppDeployedToStandaloneUrl(contextRoot, "1", true)
    }

    void checkAppDeployedToStandaloneUrl(String contextRoot, String version, boolean available) {
        String hostname = EnvPropertiesHelper.getResourceHostname()
        String port = 8080
        String url = "http://$hostname:$port/$contextRoot/version$version"
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