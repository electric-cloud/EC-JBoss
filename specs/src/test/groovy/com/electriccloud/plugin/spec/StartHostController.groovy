package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

@Requires({ env.OS == 'UNIX' && env.JBOSS_VERSION in ['7.0', '7.1']})
class StartHostController extends PluginTestHelper {

    @Shared
    String procName = 'StartHostController'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"

    @Shared
    def resSlaveName

    //  https://ecflow.testrail.net/index.php?/suites/view/27&group_by=cases:section_id&group_id=82608&group_order=asc
    @Shared
    def testCases = [
        systemTest1: [
            name: 'C323957',
            description: 'C323957, C323981, C323958'],  
        systemTest2: [
            name: 'C324137',
            description: 'C324137'],  
        systemTest3: [
            name: 'C323980',
            description: 'C323980'],
        systemTest4: [
            name: 'C323985',
            description: 'C323985'],
        systemTest5: [
            name: 'C323979',
            description: 'C323979'],
        systemTest6: [
            name: 'C323835',
            description: 'C323835'],
        systemTest7: [
            name: 'C324144',
            description: 'C324144'],
        systemTest8: [
            name: 'C324141',
            description: 'C324141'], 
        systemTest9: [
            name: 'C324140',
            description: 'C324140'],     
        systemTest10: [
            name: 'C324142',
            description: 'C324142'],
        systemTest11: [
            name: 'C323983',
            description: 'C323983'],
        systemTest12: [
            name: 'C323984',
            description: 'C323984'],
        systemTest13: [
            name: 'C323960',
            description: 'C323960'],                           
    ]

    @Shared
    def domainConfigs = [
        'default': '',
    ]

    @Shared
    def hostConfigs = [
        'default': '',
        'bootErrors': 'host_boot_errors.xml',
        'slave': 'host-slave.xml'
    ]

    @Shared
    def jbossHostNames = [
        'master': 'master',
        'slave': 'jbossslave1',
        'wrong': 'wrong',
        'empty': ''
    ]

    @Shared
    def startupScripts = [
        'default': (EnvPropertiesHelper.getOS()=='UNIX') ? '/opt/jboss/bin/domain.sh' : 'C:/tmp/jboss/bin/domain.bat',
    ]

    @Shared
    def logFileLocations = [
        'empty': '',
        'custom': (EnvPropertiesHelper.getOS()=='UNIX') ? '/tmp/qa/host-controller.log' : 'C:/tmp/qa/host-controller.log',
        'wrong': (EnvPropertiesHelper.getOS()=='UNIX') ? '/tmp/wrong/host-controller.log' : 'C:/tmp/wrong/host-controller.log',
    ]

    @Shared
    def additionalOptions = [
        'docker': '-b 0.0.0.0 -bmanagement 0.0.0.0', 
        'customLogs' : (EnvPropertiesHelper.getOS()=='UNIX') ? ' -Djboss.domain.log.dir=/tmp/qa' : ' -Djboss.domain.log.dir=C:/tmp/qa',
        'empty': '',
        'slave': "-Djboss.domain.master.address=\\\'jboss\\\' -b 0.0.0.0 -bmanagement 0.0.0.0",
        'wrongMaster': "-Djboss.domain.master.address=\\\'wrogn_jboss\\\' -b 0.0.0.0 -bmanagement 0.0.0.0",
        'wrong': "-b wrong -bmanagement 0.0.0.0",
    ]

    @Shared
    def summaries = [
        'default': "JBoss Host Controller 'master' has been launched, host state is 'running'\nNo boot errors of host controller 'master'\nServer 'server-one' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-one' on host 'master'\nServer 'server-three' on host 'master' has status 'DISABLED'\nServer 'server-two' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-two' on host 'master'",
        'defaultSlave': "JBoss Host Controller 'jbossslave1' has been launched, host state is 'running'\nNo boot errors of host controller 'jbossslave1'\nServer 'server-one' on host 'jbossslave1' has status 'STARTED'\nNo boot errors of server 'server-one' on host 'jbossslave1'\nServer 'server-two' on host 'jbossslave1' has status 'STARTED'\nNo boot errors of server 'server-two' on host 'jbossslave1'",
        'emptyHost': "Warning: JBoss Host Controller has been launched, but verification that it is started is not performed \\(due to 'jbossHostName' parameter is not provided\\).\nList of host controllers within Domain: master\nPlease refer to JBoss logs on file system for more information",
        'bootErrors': "JBoss Host Controller 'master' has been launched, host state is 'running'\nNo boot errors of host controller 'master'\nServer 'server-one' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-one' on host 'master'\nServer 'server-three' on host 'master' has status 'STARTED'\nWarning: Detected boot errors of server 'server-three' on host 'master', see log for details\nServer 'server-two' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-two' on host 'master'",
        'error': "Error: Failed to connect to Master CLI for verication of Host Controller 'master' state\nPlease refer to JBoss logs on file system for more information",
        'wrongLogsAndBootErrors': "JBoss Host Controller 'master' has been launched, host state is 'running'\nWarning: Cannot find JBoss log file '/tmp/wrong/host-controller.log'\nNo boot errors of host controller 'master'\nServer 'server-one' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-one' on host 'master'\nServer 'server-three' on host 'master' has status 'STARTED'\nWarning: Detected boot errors of server 'server-three' on host 'master', see log for details\nServer 'server-two' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-two' on host 'master'",
        'wrongLogs': "JBoss Host Controller 'master' has been launched, host state is 'running'\nWarning: Cannot find JBoss log file '/tmp/wrong/host-controller.log'\nNo boot errors of host controller 'master'\nServer 'server-one' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-one' on host 'master'\nServer 'server-three' on host 'master' has status 'DISABLED'\nServer 'server-two' on host 'master' has status 'STARTED'\nNo boot errors of server 'server-two' on host 'master'",
        'wrongHost': "Error: JBoss Host Controller 'wrong' is not started \\(or not connected to Master\\)\nList of host controllers within Domain: master\nPlease refer to JBoss logs on file system for more information",
        'wrongSlaveHost': "Error: JBoss Host Controller 'wrong' is not started \\(or not connected to Master\\)\nList of host controllers within Domain: jbossslave1, master\nPlease refer to JBoss logs on file system for more information",
        'wrongMaster': "JBoss Host Controller 'jbossslave1' is not started \\(or not connected to Master\\)\nList of host controllers within Domain: master\nPlease refer to JBoss logs on file system for more information"
    ]

    @Shared
    def jobLogs = [
        'default': ["Checking via Master CLI whether JBoss Host Controller 'master' is already started", '-c controller=jboss:9990  --user=\'admin\'  --password=\\*\\*\\* --command=":read-attribute\\(name=launch-type\\)"', "JBoss Master is not started yet \\(cannot connect to Master CLI\\)", "\"${startupScripts.default}\" -b 0.0.0.0 -bmanagement 0.0.0.0'", "Server 'server-one' on host 'master' has status 'STARTED'"],
        'defaultSlave': ["Checking via Master CLI whether JBoss Host Controller 'jbossslave1' is already started", '-c controller=jboss:9990  --user=\'admin\'  --password=\\*\\*\\* --command=":read-attribute\\(name=launch-type\\)"', "JBoss Host Controller 'jbossslave1' is not started", "\"${startupScripts.default}\" --host-config=\"host-slave.xml\"  -Djboss.domain.master.address='jboss' -b 0.0.0.0 -bmanagement 0.0.0.0'", "Server 'server-one' on host 'jbossslave1' has status 'STARTED'"],
        'customLogs': ["Checking via Master CLI whether JBoss Host Controller 'master' is already started", '-c controller=jboss:9990  --user=\'admin\'  --password=\\*\\*\\* --command=":read-attribute\\(name=launch-type\\)"', "JBoss Master is not started yet \\(cannot connect to Master CLI\\)", "\"${startupScripts.default}\" -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.domain.log.dir=/tmp/qa", "Server 'server-one' on host 'master' has status 'STARTED'"],
        "emptyHost": ["INFO: JBoss Host Controller has been launched, but verification that it is started is not performed \\(due to 'jbossHostName' parameter is not provided\\).", 'List of host controllers within Domain: master', "Please refer to JBoss logs on file system for more information"],
        'bootErrors': ["One or more services were unable to start due to one or more indirect dependencies not being available", "Address already in use"],
    	'error': ["Error: Failed to connect to Master CLI for verication of Host Controller 'master' state", "Please refer to JBoss logs on file system for more information"],
    	'wrongLogs': ["Warning: Cannot find JBoss log file '/tmp/wrong/host-controller.log'"],
    	'wrongHost': ["Checking via Master CLI whether JBoss Host Controller 'wrong' is already started", "Error: JBoss Host Controller 'wrong' is not started"],
    	'wrongMaster': ['Error: JBoss Host Controller \'jbossslave1\' is not started \\(or not connected to Master\\)', 'List of host controllers within Domain: master'],
    ]

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()
        resSlaveName = createJBossResource('slave')

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : EnvPropertiesHelper.getTopology() == 'master' ? resName : resSlaveName,
                procName: procName,
                params  : [
                    additionalOptions: '',
                    domainConfig: '',
                    hostConfig: '',
                    jbossHostName: '',
                    logFileLocation: '',
                    serverconfig: '',
                    startupScript: '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createJbossConfigurationWithBootErrors(resName)
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
                additionalOptions: additionalOption,
                domainConfig: domainConfig,
                hostConfig: hostConfig,
                jbossHostName: jbossHostName,
                logFileLocation: logFileLocation,
                serverconfig: serverconfig,
                startupScript: startupScript,
        ]
        setup:
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.shutDownHostDomain(jbossHostNames.'master'))
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        for (log in logs){
            assert procedureLogs =~ log
        }


        where:
        testCaseId                  | serverconfig      | domainConfig            | hostConfig               | jbossHostName             | startupScript             | logFileLocation            | additionalOption                                          | jobExpectedStatus | summary                             | logs
        testCases.systemTest1.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'docker'                                | "success"         | summaries.'default'                 | jobLogs.'default'
        testCases.systemTest2.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'custom'  | additionalOptions.'docker'+additionalOptions.'customLogs' | "success"         | summaries.'default'                 | jobLogs.'customLogs'
    }

    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "StartHostController - start jboss: success and warning, boot errors"() {
        def runParams = [
            additionalOptions: additionalOption,
            domainConfig: domainConfig,
            hostConfig: hostConfig,
            jbossHostName: jbossHostName,
            logFileLocation: logFileLocation,
            serverconfig: serverconfig,
            startupScript: startupScript,
        ]
        setup:
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.shutDownHostDomain(jbossHostNames.'master'))
        when: 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        for (log in logs){
            assert procedureLogs =~ log
        }


        where:
        testCaseId                  | serverconfig      | domainConfig            | hostConfig               | jbossHostName             | startupScript             | logFileLocation            | additionalOption                                          | jobExpectedStatus | summary                             | logs
        testCases.systemTest1.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'docker'                                | "success"         | summaries.'default'                 | jobLogs.'default'
        testCases.systemTest2.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'custom'  | additionalOptions.'docker'+additionalOptions.'customLogs' | "success"         | summaries.'default'                 | jobLogs.'customLogs'
        testCases.systemTest3.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'empty'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'docker'                                | "warning"         | summaries.'emptyHost'               | jobLogs.'emptyHost'
        testCases.systemTest4.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'bootErrors' | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'docker'                                | "warning"         | summaries.'bootErrors'              | jobLogs.'bootErrors'
        testCases.systemTest8.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'bootErrors' | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'custom'  | additionalOptions.'docker'                                | "warning"         | summaries.'bootErrors'              | jobLogs.'bootErrors'
        testCases.systemTest9.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'bootErrors' | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'wrong'   | additionalOptions.'docker'+additionalOptions.'customLogs' | "warning"         | summaries.'wrongLogsAndBootErrors'  | jobLogs.'wrongLogs'
        testCases.systemTest10.name | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'wrong'   | additionalOptions.'docker'+additionalOptions.'customLogs' | "warning"         | summaries.'wrongLogs'               | jobLogs.'wrongLogs'
        testCases.systemTest11.name | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'wrong'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'docker'                                | "error"           | summaries.'wrongHost'               | jobLogs.'wrongHost'
    }

    @Requires({ env.JBOSS_TOPOLOGY == 'master_slave' })
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Start Slave - positive"() {
        def runParams = [
            additionalOptions: additionalOption,
            domainConfig: domainConfig,
            hostConfig: hostConfig,
            jbossHostName: jbossHostName,
            logFileLocation: logFileLocation,
            serverconfig: serverconfig,
            startupScript: startupScript,
        ]
        setup:
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.shutDownHostDomain(jbossHostNames.'slave'))
        when: 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        for (log in logs){
            assert procedureLogs =~ log
        }
        cleanup: 
        waitUntilServerIsUp(jbossHostNames.'slave')
        where:
        testCaseId                  | serverconfig      | domainConfig            | hostConfig               | jbossHostName             | startupScript             | logFileLocation            | additionalOption             | jobExpectedStatus | summary                   | logs
        testCases.systemTest5.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'slave'      | jbossHostNames.'slave'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'slave'    | "success"         | summaries.'defaultSlave'  | jobLogs.'defaultSlave'
        testCases.systemTest6.name  | defaultConfigName | domainConfigs.'default' | hostConfigs.'slave'      | jbossHostNames.'empty'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'slave'    | "warning"         | summaries.'emptyHost'     | jobLogs.'emptyHost'
        testCases.systemTest12.name | defaultConfigName | domainConfigs.'default' | hostConfigs.'slave'      | jbossHostNames.'wrong'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'slave'    | "error"           | summaries.'wrongSlaveHost'| jobLogs.'wrongHost'
    }

    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "StartHostController - errors"() {
        def runParams = [
            additionalOptions: additionalOption,
            domainConfig: domainConfig,
            hostConfig: hostConfig,
            jbossHostName: jbossHostName,
            logFileLocation: logFileLocation,
            serverconfig: serverconfig,
            startupScript: startupScript,
        ]
        setup:
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.shutDownHostDomain(jbossHostNames.'master'))
        when: 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        for (log in logs){
            assert procedureLogs =~ log
        }

        cleanup:
        runParams = [
            additionalOptions: additionalOptions.'docker',
            domainConfig: domainConfigs.'default',
            hostConfig: hostConfigs.'default',
            jbossHostName: jbossHostNames.'master',
            logFileLocation: logFileLocations.'empty',
            serverconfig: defaultConfigName,
            startupScript: startupScripts.'default',
        ]
        runProcedureJob = runProcedureUnderTest(runParams)

        where:
        testCaseId                 | serverconfig      | domainConfig            | hostConfig               | jbossHostName             | startupScript             | logFileLocation            | additionalOption            | jobExpectedStatus | summary              | logs
        testCases.systemTest7.name | defaultConfigName | domainConfigs.'default' | hostConfigs.'default'    | jbossHostNames.'master'   | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'wrong'   | "error"           | summaries.'error'    | jobLogs.'error'
    }

    @Requires({ env.JBOSS_TOPOLOGY == 'master_slave' })
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Start Slave - errors"() {
        def runParams = [
            additionalOptions: additionalOption,
            domainConfig: domainConfig,
            hostConfig: hostConfig,
            jbossHostName: jbossHostName,
            logFileLocation: logFileLocation,
            serverconfig: serverconfig,
            startupScript: startupScript,
        ]
        setup:
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.shutDownHostDomain(jbossHostNames.'slave'))
        when: 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        for (log in logs){
            assert procedureLogs =~ log
        }

        cleanup:
        runParams = [
            additionalOptions: additionalOptions.'slave',
            domainConfig: domainConfigs.'default',
            hostConfig: hostConfigs.'slave',
            jbossHostName: jbossHostNames.'slave',
            logFileLocation: logFileLocations.'empty',
            serverconfig: defaultConfigName,
            startupScript: startupScripts.'default',
        ]
        runProcedureJob = runProcedureUnderTest(runParams)

        where:
        testCaseId                  | serverconfig      | domainConfig            | hostConfig               | jbossHostName             | startupScript             | logFileLocation            | additionalOption                | jobExpectedStatus | summary                   | logs
        testCases.systemTest13.name | defaultConfigName | domainConfigs.'default' | hostConfigs.'slave'      | jbossHostNames.'slave'    | startupScripts.'default'  | logFileLocations.'empty'   | additionalOptions.'wrongMaster' | "error"           | summaries.'wrongMaster'   | jobLogs.'wrongMaster'
    }


    def createJbossConfigurationWithBootErrors(def resName){
        runCustomShellCommand("cp /opt/jboss/domain/configuration/host.xml /opt/jboss/domain/configuration/host_boot_errors.xml", resName)
        runCustomShellCommand("sed -i \\\'s/auto-start=\"false\"/auto-start=\"true\"/g\\\' /opt/jboss/domain/configuration/host_boot_errors.xml", resName)
        runCustomShellCommand("sed -i \\\'s/port-offset=\"150\"/port-offset=\"250\"/g\\\' /opt/jboss/domain/configuration/host_boot_errors.xml", resName)
    }


    def waitUntilServerIsUp(def serverName){
        def isHostControllerRunning = true
        def attemptNumber = 0
        def attemptTotalCount = 10
        def jbossDomainPath = EnvPropertiesHelper.getJbossDomainPath();
        while(isHostControllerRunning){
	        try {
	            if (attemptNumber == attemptTotalCount){
	                break
	            }
	            attemptNumber += 1 
	            if (runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getHostStatus(serverName)).result == 'running') {
	                isHostControllerRunning = false
	            }       
            }
 			catch (Exception e){
				sleep(5000) 
			}
        }

    }

}