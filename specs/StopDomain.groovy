import Services.CliCommandsGeneratorHelper
import Models.JBoss.Domain.ServerHelper
import Utils.EnvPropertiesHelper
import spock.lang.*
import Utils.EnvPropertiesHelper

@Requires({ env.JBOSS_TOPOLOGY == 'master_slave' })
class StopDomain extends PluginTestHelper {

	@Shared
	String procName = 'StopDomain'
	@Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultWaitTime = '100'
    @Shared
    def resName
    @Shared
    def resSlaveName
    @Shared
    def serverNames = [
        [Name: 'server-one', Host: 'master'], 
        [Name: 'server-two', Host: 'master'], 
        [Name: 'server-three', Host: 'master'],
        [Name: 'server-one', Host: 'jbossslave1'], 
        [Name: 'server-two', Host: 'jbossslave1']]

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        createDefaultConfiguration(defaultConfigName)
        resName = createJBossResource()
        resSlaveName = createJBossResource('slave')
        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        serverconfig     		: '',
                        allControllersShutdown  : '',
                        jbossTimeout        	: '',

                ]
        ]

        createHelperProject(resName, defaultConfigName)
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }    

    @Unroll
    def "StopDomain, with minimum parameters (no wait time - undef) (C289387)"() {
    	String testCaseId = "C289387"
        def serverGroupName = "default"
        
        def runParams = [
    		            serverconfig     		: defaultConfigName,
                        allControllersShutdown  : '',
                        jbossTimeout        	: '',
    	]
    	when:
    	RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

    	then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() == "Performed stop-servers operation for domain\nFound 5 servers with expected statuses (STOPPED or DISABLED)"
        for (item in serverNames){
           ServerHelper server = new ServerHelper(item.Name, serverGroupName, item.Host)
           def expectedStatus = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerAutoStartInDomain(server)).result ? 'STOPPED' : 'DISABLED' 
           assert  runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server)).result == expectedStatus
        }

        cleanup:
        for (item in serverNames){
           ServerHelper server = new ServerHelper(item.Name, serverGroupName, item.Host)
           runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server))
        }
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }

    @Unroll
    def "StopDomain, with default parameters (C289399)"() {
        String testCaseId = "C289399"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '0',
                        jbossTimeout            : '60',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() == "Performed stop-servers operation for domain\nFound 5 servers with expected statuses (STOPPED or DISABLED)"
        for (item in serverNames){
           ServerHelper server = new ServerHelper(item.Name, serverGroupName, item.Host)
           def expectedStatus = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerAutoStartInDomain(server)).result ? 'STOPPED' : 'DISABLED' 
           assert  runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server)).result == expectedStatus
        }
        cleanup:
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }

 
    @Unroll
    def "StopDomain, stop with no wait time - 0 (C289400)"() {
        String testCaseId = "C289400"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '',
                        jbossTimeout            : '0',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() == "Performed stop-servers operation for domain\nFound 5 servers with expected statuses (STOPPED or DISABLED)"
        for (item in serverNames){
           ServerHelper server = new ServerHelper(item.Name, serverGroupName, item.Host)
           def expectedStatus = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerAutoStartInDomain(server)).result ? 'STOPPED' : 'DISABLED' 
           assert  runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerStatusInDomain(server)).result == expectedStatus
        }


        cleanup:
        for (item in serverNames){
           ServerHelper server = new ServerHelper(item.Name, serverGroupName, item.Host)
           runCliCommand(CliCommandsGeneratorHelper.startServerCmd(server))
        }
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }    

    @Unroll
    def "StopDomain, all fields are filled (All Controllers Shutdown=true) (C289401)"() {
        String testCaseId = "C289401"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        def expectedSummary = "Performed stop-servers operation for domain\nFound 5 servers with expected statuses (STOPPED or DISABLED)\nShutdown was performed for slave host controller 'jbossslave1'\nShutdown was performed for master host controller 'master'"
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() == expectedSummary
        def expectedStatus = 'Failed to connect to the controller: The controller is not available at'
        assert runCliCommandAnyResult(":read-attribute(name=launch-type)").getUpperStepSummary().contains(expectedStatus)
        cleanup:
        def jbossDomainPath = EnvPropertiesHelper.getJbossDomainPath();
        runCustomCliCommand("nohup $jbossDomainPath -b 0.0.0.0 -bmanagement 0.0.0.0 > log &", resName)
        runCustomCliCommand("nohup $jbossDomainPath -Djboss.domain.master.address=\"jboss\" -b 0.0.0.0 -bmanagement 0.0.0.0 --host-config=host-slave.xml > log &", resSlaveName)
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }   

    @Unroll
    def "StopDomain, one host with 'All Controllers Shutdown'=true (C289402)"() {
        String testCaseId = "C289402"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        //precondtion, stop slave host controller
        runCliCommand("/host=jbossslave1:shutdown")
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        def expectedSummary = "Performed stop-servers operation for domain\nFound 3 servers with expected statuses (STOPPED or DISABLED)\nShutdown was performed for master host controller 'master'"
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() == expectedSummary
        def expectedStatus = 'Failed to connect to the controller: The controller is not available at'
        assert runCliCommandAnyResult(":read-attribute(name=launch-type)").getUpperStepSummary().contains(expectedStatus)
        cleanup:
        def jbossDomainPath = EnvPropertiesHelper.getJbossDomainPath();
        runCustomCliCommand("nohup $jbossDomainPath -b 0.0.0.0 -bmanagement 0.0.0.0 > log &", resName)
        runCustomCliCommand("nohup $jbossDomainPath -Djboss.domain.master.address=\"jboss\" -b 0.0.0.0 -bmanagement 0.0.0.0 --host-config=host-slave.xml > log &", resSlaveName)
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }   

    @Unroll
    def "StopDomain, with not existing 'Configuration name' (C289412)"() {
        String testCaseId = "C289412"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : 'jboss_conf_not_exist',
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        assert runProcedureJob.getStatus() == 'error' 
        assert runProcedureJob.getUpperStepSummary() == "Configuration jboss_conf_not_exist doesn't exist.\n"
    }   


    @Unroll
    def "StopDomain, without 'Configuration name'  (C289411)"() {
        String testCaseId = "C289411"
        def serverGroupName = "default"
        
        def runParams = [
                        serverconfig            : '',
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        assert runProcedureJob.getStatus() == 'error' 
        // assert runProcedureJob.getLogs() =~ "Configuration_name doesn't exist at*"
    }   

    @Unroll
    def "StopDomain, host controller 'master' is stopped   (C289407)"() {
        String testCaseId = "C289407"
        def serverGroupName = "default"
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        //precondtion, stop slave host controller
        runCliCommand("/host=master:shutdown")
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        def expectedStatus = 'Failed to connect to the controller: The controller is not available at'
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary().contains(expectedStatus)
        cleanup:
        def jbossDomainPath = EnvPropertiesHelper.getJbossDomainPath();
        runCustomCliCommand("nohup $jbossDomainPath -b 0.0.0.0 -bmanagement 0.0.0.0 > log &", resName)
        runCustomCliCommand("nohup $jbossDomainPath -Djboss.domain.master.address=\"jboss\" -b 0.0.0.0 -bmanagement 0.0.0.0 --host-config=host-slave.xml > log &", resSlaveName)
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }   

    @Unroll
    def "StopDomain, host controllers 'master' and 'slave' are stopped   (C289410)"() {
        String testCaseId = "C289410"
        def serverGroupName = "default"
        def runParams = [
                        serverconfig            : defaultConfigName,
                        allControllersShutdown  : '1',
                        jbossTimeout            : '60',
        ]
        when:
        //precondtion, stop slave and master host controller
        runCliCommand("/host=jbossslave1:shutdown")
        runCliCommand("/host=master:shutdown")
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        then:
        def expectedStatus = 'Failed to connect to the controller: The controller is not available at'
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary().contains(expectedStatus)
        cleanup:
        def jbossDomainPath = EnvPropertiesHelper.getJbossDomainPath();
        runCustomCliCommand("nohup $jbossDomainPath -b 0.0.0.0 -bmanagement 0.0.0.0 > log &", resName)
        runCustomCliCommand("nohup $jbossDomainPath -Djboss.domain.master.address=\"jboss\" -b 0.0.0.0 -bmanagement 0.0.0.0 --host-config=host-slave.xml > log &", resSlaveName)
        waitUntilServerIsUp('master')
        waitUntilServerIsUp('jbossslave1')
    }

    def waitUntilServerIsUp(def serverName){
        def cond = true
        while(cond){
            try {
                sleep(3000)
                if (runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getHostStatus(serverName)).result == 'running') {
                    cond = false
                }
            }
            catch (Exception e){
                println e.getMessage()
            }
        }

    }

}
