package com.electriccloud.plugin.spec

import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

@Requires({ env.OS == 'UNIX' && env.JBOSS_VERSION in ['7.0', '7.1']})
class CheckHostControllerStatus extends PluginTestHelper {

    @Shared
    String procName = 'CheckHostControllerStatus'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        conditionallyDeleteProject(projectName)
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                          config        : '',
                    scriptphysicalpath  : '',
                    criteria            : '',
                    hostcontroller_name : '',
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

    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    @Sanity
    @Unroll
    def "Sanity"() {
        def runParams = [
                      config        : defaultConfigName,
                scriptphysicalpath  : defaultCliPath,
                criteria            : 'running',
                hostcontroller_name : 'master',
                wait_time           : '60',
        ]

        when:

        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()

        then:
        assert runProcedureJob.getStatus() == "success"
        assert jobUpperStepSummary =~ "Criteria was met. HostController is in 'running' status"

    }

}