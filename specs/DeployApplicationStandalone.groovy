import Services.CliCommandsGeneratorHelper
import spock.lang.*
import Utils.EnvPropertiesHelper

@IgnoreIf({ env.JBOSS_MODE == 'domain' })
class DeployAppStandalone extends PluginTestHelper {

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
//        deleteProject(projectName)
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

    @Unroll
    def "DeployApp, 1st time, file, disabled server groups ignored (C278101)"() {
        String testCaseId = "C278101"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : '',
                disabledServerGroups           : 'disabled-server-group',
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

    @Unroll
    def "DeployApp, 1st time, file, enabled server groups ignored (C278133)"() {
        String testCaseId = "C278133"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : 'enabled-server-group',
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

    @Unroll
    def "DeployApp, 1st time, file, custom app name (C278102)"() {
        String testCaseId = "C278102"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app-custom-appname.war",
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
        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-appname.war"
        String expectedContextRoot = "$testCaseId-app-custom-appname"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, 1st time, file, custom runtime name (C278103)"() {
        String testCaseId = "C278103"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-custom-runtimename.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone("$testCaseId-app.war")
    }

    @Unroll
    def "DeployApp, 1st time, file, custom app name, custom runtime name (C278104)"() {
        String testCaseId = "C278104"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app-custom-appname.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-custom-runtimename.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, 1st time, file, custom app name without extension, custom runtime name (C278105)"() {
        String testCaseId = "C278105"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app",
                deploymentName                 : "$testCaseId-app-custom-appname.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-custom-runtimename.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app-custom-appname.war"
        String expectedRuntimeName = "$testCaseId-app-custom-runtimename.war"
        String expectedContextRoot = "$testCaseId-app-custom-runtimename"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, 1st time, file, whitespace in path (C278109)"() {
        String testCaseId = "C278109"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/app with whitespace.war",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app.war",
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
        undeployAppFromStandalone(expectedAppName)
    }

    @Unroll
    def "DeployApp, 1st time, file, disabled flag in additional options (C278110)"() {
        String testCaseId = "C278110"

        def runParams = [
                additionalOptions              : '--disabled',
                applicationContentSourcePath   : "/tmp/$testCaseId-app",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app.war",
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
        checkAppDeployedToStandaloneUrl(expectedContextRoot, false)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }

    @Unroll
    def "DeployApp, 1st time, url (for EAP 7 and later), custom app name, custom runtime name (C278115)"() {
        String testCaseId = "C278115"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : "$testCaseId-app-customname.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-customname-runtime.war",
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app-customname.war"
        String expectedRuntimeName = "$testCaseId-app-customname-runtime.war"
        String expectedContextRoot = "$testCaseId-app-customname-runtime"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, 1st time, url (for EAP 7 and later), disabled flag in additional options (C278115)"() {
        String testCaseId = "C278115"

        def runParams = [
                additionalOptions              : '--disabled',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app.war",
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot, false)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }

    @Unroll
    def "DeployApp, app already deployed, url (for EAP 7 and later), change runtime name (C278116)"() {
        String testCaseId = "C278116"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-new-runtime-name.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, "/tmp/$testCaseId-app.war")
        deployAppToStandalone("/tmp/$testCaseId-app.war",runParams.deploymentName,"$testCaseId-app.war")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-new-runtime-name.war"
        String expectedContextRoot = "$testCaseId-app-new-runtime-name"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        expectedAppName = "$testCaseId-app.war"
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, app already deployed, file, change runtime-name (C278107)"() {
        String testCaseId = "C278107"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-new-runtime-name.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)
        deployAppToStandalone(runParams.applicationContentSourcePath,runParams.deploymentName,"$testCaseId-app.war")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-new-runtime-name.war"
        String expectedContextRoot = "$testCaseId-app-new-runtime-name"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }


    @Unroll
    def "DeployApp, app already deployed, file, change runtime-name, enabled server groups and disabled server groups ignored (C278108)"() {
        String testCaseId = "C278108"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : 'disabled-server-group',
                enabledServerGroups            : 'enabled-server-group',
                runtimeName                    : "$testCaseId-app-new-runtime-name.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)
        deployAppToStandalone(runParams.applicationContentSourcePath,runParams.deploymentName,"$testCaseId-app.war")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app-new-runtime-name.war"
        String expectedContextRoot = "$testCaseId-app-new-runtime-name"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }

    @Unroll
    def "DeployApp, 1st time, url (for EAP 7 and later), minimum params (C278195)"() {
        String testCaseId = "C278195"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "hello-world.war"
        String expectedRuntimeName = "hello-world.war"
        String expectedContextRoot = "hello-world"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'"

        checkAppDeployedToStandaloneCli(expectedAppName, expectedRuntimeName)
        checkAppDeployedToStandaloneUrl(expectedContextRoot)

        cleanup:
        undeployAppFromStandalone(expectedAppName)
    }

    @Unroll
    def "Negative. DeployApp, 1st time, file, non existing filepath (C278120)"() {
        String testCaseId = "C278120"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "/tmp/non-existing-file.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "File '${runParams.applicationContentSourcePath}' doesn't exists"
    }

    @Unroll
    def "Negative. DeployApp, 1st time, file, wrong additional options (C278121)"() {
        String testCaseId = "C278121"

        def runParams = [
                additionalOptions              : '--some-wrong-param',
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
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized arguments: \\[--some-wrong-param\\]"
    }


    @Unroll
    def "Negative. DeployApp, 1st time, incorrect param, undef required param, path to app (C278119)"() {
        String testCaseId = "C278119"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : '',
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'applicationContentSourcePath' is not provided"
    }


    @Unroll
    def "Negative. DeployApp, 1st time, url (for EAP 7 and later) incorrect value (C278125)"() {
        String testCaseId = "C278125"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=https://github.com/electric-cloud/incorrect-path/hello-world.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Cannot create input stream from URL 'https://github.com/electric-cloud/incorrect-path/hello-world.war'"
    }

    @Unroll
    def "Negative. DeployApp, app already deployed, url (for EAP 7 and later) is empty, change runtime name (C278196)"() {
        String testCaseId = "C278196"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app-new-runtime-name.war",
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, "/tmp/$testCaseId-app.war")
        deployAppToStandalone("/tmp/$testCaseId-app.war","$testCaseId-app.war","$testCaseId-app.war")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "--force requires a filesystem path or --url pointing to the deployment to be added to the repository."

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