import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class StopServers extends PluginTestHelper {

    @Shared
    String procName = 'StopServers'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String defaultWaitTime = '100'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()
        logger.info("Hello World! doSetupSpec")

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        serverconfig      : '',
                        scriptphysicalpath: '',
                        serversgroup      : '',
                        wait_time         : '',
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
    def "StopServers, group with all servers started (C259528)"() {
        setup:
        String testCaseId = "C259528"

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
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STARTED.*Found server $serverName1 in state STOPPED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 in state STARTED.*Found server $serverName2 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "StopServers, group with all servers started (different auto-start options on each - check DISABLED status) (C259542)"() {
        setup:
        String testCaseId = "C259542"

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
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server2))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STARTED"

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STARTED.*Found server $serverName1 in state DISABLED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 in state STARTED.*Found server $serverName2 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "DISABLED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "StopServers, group with all servers stopped (C259529)"() {
        setup:
        String testCaseId = "C259529"

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

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'warning'
        assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in STOPPED state/
        assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLowerStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLowerStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STOPPED.*Found server $serverName1 in state STOPPED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 in state STOPPED.*Found server $serverName2 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    def "StopServers, group with all servers stopped (different auto-start options on each - check DISABLED status) (C259556)"() {
        setup:
        String testCaseId = "C259556"

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
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'warning'
        assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in DISABLED state/
        assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLowerStepSummary() =~ /Warning: Server $serverName1 on $hostName is already in DISABLED state/
        assert runProcedureJob.getLowerStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state DISABLED.*Found server $serverName1 in state DISABLED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 in state STOPPED.*Found server $serverName2 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "DISABLED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "StopServers, group with started and stopped servers (C259531)"() {
        setup:
        String testCaseId = "C259531"

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
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'warning'
        assert runProcedureJob.getUpperStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLowerStepSummary() =~ /Warning: Server $serverName2 on $hostName is already in STOPPED state/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STARTED.*Found server $serverName1 in state STOPPED/
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName2 in state STOPPED.*Found server $serverName2 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server2)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server2))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "StopServers, stop with no wait time - undef (C259546)"() {
        setup:
        String testCaseId = "C259546"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();
        String waitTime = ''

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STARTED.*Found server $serverName1 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "StopServers, stop with no wait time - 0 (C259547)"() {
        setup:
        String testCaseId = "C259547"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();
        String waitTime = '0'

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'success'
        assert runProcedureJob.getLogs() =~ /(?s)Found server $serverName1 in state STARTED.*Found server $serverName1 in state STOPPED/
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STOPPED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "Negative. StopServers, non existing server group (C259535)"() {
        setup:
        String testCaseId = "C259535"

        String serverGroupName = "server-group-$testCaseId"

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
    }

    @Unroll
    def "Negative. StopServers, empty server group (C259538)"() {
        setup:
        String testCaseId = "C259538"

        String serverGroupName = "server-group-$testCaseId"

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
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

    @Unroll
    def "Negative. StopServers, incorrect param, wait time - negative value (C259539)"() {
        setup:
        String testCaseId = "C259539"

        String serverGroupName = "server-group-$testCaseId"
        String serverName1 = "server-1-$testCaseId"
        String hostName = EnvPropertiesHelper.getJbossDomainMasterHostname();

        ServerGroupHelper serverGroup = new ServerGroupHelper(serverGroupName)
        ServerHelper server1 = new ServerHelper(serverName1, serverGroupName, hostName)

        runCliCommand(CliCommandsGeneratorHelper.addServerGroupCmd(serverGroup))
        runCliCommand(CliCommandsGeneratorHelper.addServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server1))

        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        String waitTime = '-10'

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : waitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
        assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server1)).result == "STARTED"

        cleanup:
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerCmd(server1))
        runCliCommand(CliCommandsGeneratorHelper.removeServerGroupCmd(serverGroup))
    }

    @Unroll
    def "Negative. StopServers, incorrect param, undef required param, server group (C259548)"() {
        setup:
        String testCaseId = "C259548"

        String serverGroupName = ''

        when:
        def runParams = [
                serverconfig      : defaultConfigName,
                scriptphysicalpath: defaultCliPath,
                serversgroup      : serverGroupName,
                wait_time         : defaultWaitTime
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == 'error'
    }
}