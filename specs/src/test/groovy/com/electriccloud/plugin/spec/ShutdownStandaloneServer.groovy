package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

class ShutdownStandaloneServer extends PluginTestHelper {

    @Shared
    String procName = 'ShutdownStandaloneServer'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''

    @Shared
    def jbossVersion = EnvPropertiesHelper.getVersion()

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
                        scriptphysicalpath: '',
                        serverconfig: '',
                ]
        ]

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: 'StartStandaloneServer',
                params  : [
                        additionalOptions: '',
                        alternatejbossconfig: '',
                        scriptphysicalpath: '',
                        serverconfig: '',
                        logFileLocation: '',
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

    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Sanity
    @Unroll
    def "Sanity"() {

        def runParams = [
                scriptphysicalpath: defaultCliPath,
                serverconfig: defaultConfigName,
        ]

        def runParamsStart = [
                additionalOptions   : '-b 0.0.0.0 -bmanagement 0.0.0.0 -c standalone-full.xml',
                alternatejbossconfig: '',
                scriptphysicalpath: '/opt/jboss/bin/standalone.sh',
                serverconfig: defaultConfigName,
                logFileLocation: '',

        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"

        if (jbossVersion in ['7.1', '7.0', '6.4']){
            assert runCliCommandAnyResult(CliCommandsGeneratorHelper.getStandaloneStatus()).isStatusError()
        }

        cleanup:
        runProcedureJob = runProcedureDsl(projectName, 'StartStandaloneServer', runParamsStart)
        assert runProcedureJob.getStatus() == "success"
    }

}