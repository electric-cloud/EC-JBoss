package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Models.JBoss.Domain.ServerGroupHelper
import com.electriccloud.plugin.spec.Models.JBoss.Domain.ServerHelper
import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class CheckServerGroupStatus extends PluginTestHelper {

    @Shared
    String procName = 'CheckServerGroupStatus'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''
    @Shared
    String defaultWaitTime = '100'
    @Shared
    def resName

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        createDefaultConfiguration(defaultConfigName)
        resName = createJBossResource()
        logger.info("Hello World! doSetupSpec")

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                              config      : '',
                        scriptphysicalpath: '',
                        serversgroup      : '',
                        criteria          : '',
                        wait_time         : '',
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
        setup:
        String testCaseId = "C444838"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String serverName2 = "server-2-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname()

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)
        ServerHelper server2 = new ServerHelper(serverName2, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server2))

        when:
        def runParamsStarted = [
                      config        : defaultConfigName,
                scriptphysicalpath  : defaultCliPath,
                serversgroup        : serverGroupName,
                criteria            : 'STARTED',
                wait_time           : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParamsStarted)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()

        then:
        assert runProcedureJob.getStatus() == "success"
        assert jobUpperStepSummary =~ "Criteria 'STARTED' is met"
        assert jobUpperStepSummary =~ "Servers in '${serverGroupName}' server group have statuses STARTED"

        when:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        def runParamsStopped = [
                      config        : defaultConfigName,
                scriptphysicalpath  : defaultCliPath,
                serversgroup        : serverGroupName,
                criteria            : 'STOPPED',
                wait_time           : defaultWaitTime
        ]
        runProcedureJob = runProcedureUnderTest(runParamsStopped)
        jobUpperStepSummary = runProcedureJob.getUpperStepSummary()

        then:
        assert runProcedureJob.getStatus() == "success"
        assert jobUpperStepSummary =~ "Criteria 'STOPPED' is met"
        assert jobUpperStepSummary =~ "Servers in '${serverGroupName}' server group have statuses STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

}