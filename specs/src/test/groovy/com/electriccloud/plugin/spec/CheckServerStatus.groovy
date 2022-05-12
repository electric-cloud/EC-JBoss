package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

class CheckServerStatus extends PluginTestHelper {

    @Shared
    String procName = 'CheckServerStatus'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''


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
                              config            : '',
                        scriptphysicalpath      : '',
                        criteria                : '',
                        host                    : '',
                        server                  : '',
                        url_check               : '',
                        wait_time               : '',
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
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "Sanity"() {

        def runParams = [
                      config            : defaultConfigName,
                scriptphysicalpath      : defaultCliPath,
                criteria                : 'RUNNING',
                host                    : '',
                server                  : '',
                url_check               : '',
                wait_time               : '',
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"

    }
}