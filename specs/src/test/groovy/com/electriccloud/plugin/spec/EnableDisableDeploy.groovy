package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

@Requires({ env.JBOSS_MODE == 'standalone' })
class EnableDisableDeploy extends PluginTestHelper {

    @Shared
    String procName = 'EnableDeploy'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''
    @Shared
    String linkToSampleWarFile = "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/versions/hello-world-war-version-1.war"

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

//        EnableDeploy
        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: 'EnableDeploy',
                params  : [
                        serverconfig            : '',
                        scriptphysicalpath      : '',
                        assignservergroups      : '',
                        appname                 : '',
                ]
        ]

//        DisableDeploy
        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: 'DisableDeploy',
                params  : [
                        serverconfig            : '',
                        scriptphysicalpath      : '',
                        assignservergroups      : '',
                        appname                 : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        conditionallyDeleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Sanity
    @Unroll
    def "Sanity"() {
        String testCaseId = "C111844"
        def appName = "$testCaseId-app.war"
        def warPhysicalPath = getPathApp()+appName

        def runParams = [
                serverconfig            : defaultConfigName,
                scriptphysicalpath      : defaultCliPath,
                appname                 : appName,
                assignservergroups      : '',
        ]

        setup:

        downloadArtifact(linkToSampleWarFile, warPhysicalPath)
        deployAppToStandalone(warPhysicalPath, appName, appName)

        when:
        RunProcedureJob runProcedureJob = runProcedureDsl(projectName, 'DisableDeploy', runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${appName} has been successfully disabled"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result.'enabled' == false

        when:
        runProcedureJob = runProcedureDsl(projectName, 'EnableDeploy', runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application ${appName} has been successfully enabled"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result.'enabled' == true

        cleanup:
        undeployAppFromStandalone(appName)

    }

    void undeployAppFromStandalone(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromStandalone(appName))
    }

    void deployAppToStandalone(String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToStandalone(filePath, appName, runtimeName))
    }
}