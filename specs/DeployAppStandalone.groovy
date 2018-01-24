import Services.CliCommandsGeneratorHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'domain' })
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
//        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Unroll
    def "DeployApp, 1st time, minimum params (dmdmmdd)"() {
        String testCaseId = "dmdmmdd"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
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
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, server groups ignored (dsdd)"() {
        String testCaseId = "dsdd"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
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
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, all server groups ignored (aaa)"() {
        String testCaseId = "aaa"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
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
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}"

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-appname.war"
        String expectedContextRoot = "$testCaseId-app-custom-appname"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname.war")
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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

        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname.war")
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--name=.*${runParams.appname}.*--runtime-name=.*${runParams.runtimename}"

        String expectedAppName = "$testCaseId-app-custom-appname"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app-custom-appname")
    }

    @Unroll
    def "DeployApp, 1st time, both server groups and all server groups ignored (nfjsdnf)"() {
        String testCaseId = "nfjsdnf"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--force"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, app already deployed, server groups and all server groups ignored (sdjfnjsdnfnna)"() {
        String testCaseId = "sdjfnjsdnfnna"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "$testCaseId-app.war",
                runtimename          : "$testCaseId-app-new-runtimename.war",
                force                : "1", // force flag
                assignservergroups   : "some-group", // to be ignored as usual for standalone
                assignallservergroups: "1", // to be ignored as usual for standalone
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*--force"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    def "DeployApp, app already deployed, no force flag (kkk)"() {
        String testCaseId = "kkk"

        def runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                warphysicalpath      : "/tmp/$testCaseId-app.war",
                appname              : "",
                runtimename          : "",
                force                : "0", // when force is not set, but app exists - it's Ok fo standalone
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : ""
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.warphysicalpath)

        String existingAppName = "$testCaseId-app.war"
        String oldRuntimeName = "$testCaseId-app-old-runtimename.war"
        String oldContextRoot = "$testCaseId-app-old-runtimename"
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*"

        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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
                assignservergroups   : "",
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
                assignservergroups   : "",
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
                additional_options   : " --disabled" // disabled flag
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
        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        //todo: check app is uploaded but not enabled

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
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
        deployAppToStandalone(runParams.warphysicalpath, existingAppName, oldRuntimeName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${runParams.appname} \\(${runParams.warphysicalpath}\\) has been successfully deployed."
        assert runProcedureJob.getLogs() =~ "jboss-cli.*--command=.*deploy .*${runParams.warphysicalpath}.*${runParams.additional_options}"

        // let's check that app was upgraded and runtime name was changed
        String newRuntimeName = "$testCaseId-app-new-runtimename.war"
        String newContextRoot = "$testCaseId-app-new-runtimename"
        checkAppDeployedToStandaloneCli(existingAppName, newRuntimeName)
        checkAppDeployedToStandaloneUrl(newContextRoot)

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
                force                : "1", // this one
                assignservergroups   : "",
                assignallservergroups: "0",
                additional_options   : "--force" // vs this one
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
        //todo: runProcedureJob.getUpperStepSummary()
    }

    /*
    todo: test common cases (config/pathToCli/wrongCreds)
    todo: test deploy of txt files instead of jars
    todo: test incorrect runtimeName/appName/serverGroup values, e.g. too long or spec chars
     */

    void checkAppDeployedToStandaloneCli(String appName, String runtimeName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void checkAppDeployedToStandaloneUrl(String contextRoot) {
        String hostname = "jboss"; // todo: change to EnvPropertiesHelper.getResourceHostname()
        String port = 8080
        String url = "http://$hostname:$port/$contextRoot"
        assert isUrlAvailable(url)
    }

    void undeployAppFromStandalone(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromStandalone(appName))
    }

    void deployAppToStandalone(String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToStandalone(filePath, appName, runtimeName))
    }
}