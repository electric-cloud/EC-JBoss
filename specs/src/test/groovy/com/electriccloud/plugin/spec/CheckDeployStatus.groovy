package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

@Requires({ env.JBOSS_MODE == 'standalone' })
class CheckDeployStatus extends PluginTestHelper {

    @Shared
    String procName = 'CheckDeployStatus'
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

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        serverconfig        : '',
                        scriptphysicalpath  : '',
                        criteria            : '',
                        appname             : '',
                        hosts               : '',
                        servers             : '',
                        serversgroup        : '',
                        wait_time           : '',
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
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                criteria            : 'OK',
                appname             : appName,
                hosts               : '',
                servers             : '',
                serversgroup        : '',
                wait_time           : '',
        ]

        setup:

        downloadArtifact(linkToSampleWarFile, warPhysicalPath)
        deployAppToStandalone(warPhysicalPath, appName, appName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"

        when:
        runParams = [
                serverconfig         : defaultConfigName,
                scriptphysicalpath   : defaultCliPath,
                criteria            : 'failed',
                appname             : appName,
                hosts               : '',
                servers             : '',
                serversgroup        : '',
                wait_time           : '',
        ]
        undeployAppFromStandalone(appName)
        runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"

    }

    void undeployAppFromStandalone(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromStandalone(appName))
    }

    void deployAppToStandalone(String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToStandalone(filePath, appName, runtimeName))
    }
}