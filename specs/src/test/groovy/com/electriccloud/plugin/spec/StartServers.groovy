package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Models.JBoss.Domain.ServerGroupHelper
import com.electriccloud.plugin.spec.Models.JBoss.Domain.ServerHelper
import com.electriccloud.plugin.spec.Services.*
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity

import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class StartServers extends PluginTestHelper {

    @Shared
    String procName = 'StartServers'
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
        String testCaseId = "C111838"

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

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

       @NewFeature(pluginVersion = "2.6.0")
       @Unroll
       def "StartServers, group with all servers stopped (C111838)"() {
           setup:
           String testCaseId = "C111838"

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

           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

           when:
           def runParams = [
                         config      : defaultConfigName,
                   scriptphysicalpath: defaultCliPath,
                   serversgroup      : serverGroupName,
                   wait_time         : defaultWaitTime
           ]
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

           cleanup:
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
       }

       @NewFeature(pluginVersion = "2.6.0")
       @Unroll
       def "StartServers, group with all servers stopped (different auto-start options on each - check DISABLED status) (C259557)"() {
           setup:
           String testCaseId = "C259557"

           String serverGroupName = "server-group-$testCaseId"
           String serverName1 = "server-1-$testCaseId"
           String serverName2 = "server-2-$testCaseId"
           String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

           ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
           ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)
           server1.setAutoStart(false)
           ServerHelper server2 = new ServerHelper(serverName2, serverGroupName, hostName)

           runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server2))

           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "DISABLED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

           when:
           def runParams = [
                         config      : defaultConfigName,
                   scriptphysicalpath: defaultCliPath,
                   serversgroup      : serverGroupName,
                   wait_time         : defaultWaitTime
           ]
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == 'success'
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state DISABLED.*Found server $serverName1 on host $hostName in state STARTED/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 on host $hostName in state STOPPED.*Found server $serverName2 on host $hostName in state STARTED/
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

           cleanup:
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
       }

       @NewFeature(pluginVersion = "2.6.0")
       @Unroll
       def "StartServers, group with all servers started (C111840)"() {
           setup:
           String testCaseId = "C111840"

           String serverGroupName = "server-group-$testCaseId"
           String serverName1 = "server-1-$testCaseId"
           String serverName2 = "server-2-$testCaseId"
           String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

           ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
           ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)
           ServerHelper server2 = new ServerHelper(serverName2, serverGroupName, hostName)

           runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server2))

           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

           when:
           def runParams = [
                         config      : defaultConfigName,
                   scriptphysicalpath: defaultCliPath,
                   serversgroup      : serverGroupName,
                   wait_time         : defaultWaitTime
           ]
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "warning"
           assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in STARTED state/
           assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STARTED state/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STARTED.*Found server $serverName1 on host $hostName in state STARTED/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 on host $hostName in state STARTED.*Found server $serverName2 on host $hostName in state STARTED/
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

           cleanup:
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
       }

       @NewFeature(pluginVersion = "2.6.0")
       @Unroll
       def "StartServers, group with started and stopped servers (C259526)"() {
           setup:
           String testCaseId = "C259526"

           String serverGroupName = "server-group-$testCaseId"
           String serverName1 = "server-1-$testCaseId"
           String serverName2 = "server-2-$testCaseId"
           String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

           ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
           ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)
           ServerHelper server2 = new ServerHelper(serverName2, serverGroupName, hostName)

           runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))

           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

           when:
           def runParams = [
                         config      : defaultConfigName,
                   scriptphysicalpath: defaultCliPath,
                   serversgroup      : serverGroupName,
                   wait_time         : defaultWaitTime
           ]
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == 'warning'
           assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in STARTED state/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STARTED.*Found server $serverName1 on host $hostName in state STARTED/
           assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 on host $hostName in state STOPPED.*Found server $serverName2 on host $hostName in state STARTED/
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
           assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

           cleanup:
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
           runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
           runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
       }

    //todo: docker env with different hosts
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    @IgnoreIf({ env.JBOSS_MODE == 'domain' })
    def "StartServers, group with all servers stopped on different hosts (C277750)"() {
        setup:
        String testCaseId = "C277750"

        String serverGroupName = "server-group-$testCaseId"

        String serverMasterName1 = "server-1-$testCaseId"
        String serverMasterName2 = "server-2-$testCaseId"
        String serverSlaveName1 = "server-3-$testCaseId"
        String serverSlaveName2 = "server-4-$testCaseId"

        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname()
        String hostName2 = EnvPropertiesHelper.getJbossDomainSlaveHostname()

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        //create servers for master host
        ServerHelper serverMaster1 = new ServerHelper(serverMasterName1, serverGroupName, hostName)
        ServerHelper serverMaster2 = new ServerHelper(serverMasterName2, serverGroupName, hostName)
        //create servers for slave host
        ServerHelper serverSlave1 = new ServerHelper(serverSlaveName1,serverGroupName, hostName2)
        ServerHelper serverSlave2 = new ServerHelper(serverSlaveName2, serverGroupName, hostName2)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))

        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(serverMaster1))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(serverMaster2))

        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(serverSlave1))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(serverSlave2))
        //stop all servers (both hosts)
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverMaster1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverMaster2))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverSlave1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverSlave2))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverMaster1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverMaster2)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverSlave1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverSlave2)).result == "STOPPED"

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverMasterName1 on host $hostName in state STOPPED.*Found server $serverMasterName1 on host $hostName in state STARTED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverMasterName2 on host $hostName in state STOPPED.*Found server $serverMasterName2 on host $hostName in state STARTED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverSlaveName1 on host $hostName in state STOPPED.*Found server $serverSlaveName1 on host $hostName in state STARTED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverSlaveName2 on host $hostName in state STOPPED.*Found server $serverSlaveName2 on host $hostName in state STARTED/

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverMaster1)).result == "STARTED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverMaster2)).result == "STARTED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverSlave1)).result == "STARTED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(serverSlave2)).result == "STARTED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverMaster1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverMaster2))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverSlave1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(serverSlave2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(serverMaster1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(serverMaster2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(serverSlave1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(serverSlave2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }


    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "StartServers, start with no wait time - undef (C259543)"() {
        setup:
        String testCaseId = "C259543"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();
        String waitTime = ''

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "StartServers, start with no wait time - 0 (C259544)"() {
        setup:
        String testCaseId = "C259544"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();
        String waitTime = '0'

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 on host $hostName in state STOPPED.*Found server $serverName1 on host $hostName in state STARTED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. StartServers, non existing server group (C84614)"() {
        setup:
        String testCaseId = "C84614"

        String serverGroupName = "server-group-$testCaseId"

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. StartServers, empty server group (C259484)"() {
        setup:
        String testCaseId = "C259484"

        String serverGroupName = "server-group-$testCaseId"

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. StartServers, incorrect param, wait time - negative value (C259525)"() {
        setup:
        String testCaseId = "C259525"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        String waitTime = '-10'

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runProcedureJob.getUpperStepSummary() =~ "Wait time expected to be positive integer \\(wait time in seconds\\), 0 \\(unlimited\\) or undefined \\(one time check\\)"

                cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. StartServers, incorrect param, wait time - decimal value (C277845)"() {
        setup:
        String testCaseId = "C277845"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        String waitTime = '0.1'

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runProcedureJob.getUpperStepSummary() =~ "Wait time expected to be positive integer \\(wait time in seconds\\), 0 \\(unlimited\\) or undefined \\(one time check\\)."

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. StartServers, incorrect param, undef required param, server group (C259524)"() {
        setup:
        String testCaseId = "C259524"

        String serverGroupName = ''

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
        assert runProcedureJob.getLowerStepSummary() =~ "Parameter 'serversgroup' of procedure 'StartServers' is marked as required, but it does not have a value"
    }

    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negative. Controller is not available (C278096)"() { 
        setup:
        String testCaseId = "C278096"

        String serverGroupName = "server-group-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();
        shutdownHost(hostName)

        when:
        def runParams = [
                      config      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
        assert runProcedureJob.getUpperStepSummary() =~ "Failed to connect to the controller"
        
        cleanup:
        startDomain('master', resName)
    }



    /*
    todo: test common cases (config/pathToCli/wrongCreds)
     */

    void shutdownHost(String hostName) {
        runCliCommand(CliCommandsGeneratorHelper.shutDownHostDomain(hostName))
    }
}