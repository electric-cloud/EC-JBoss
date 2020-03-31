import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

class StartStandaloneServer extends PluginTestHelper {

    @Shared
    String procName = 'StartStandaloneServer'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'

    @Shared
    def jbossVersion = EnvPropertiesHelper.getVersion()

    // https://ecflow.testrail.net/index.php?/suites/view/27&group_by=cases:section_id&group_order=asc&group_id=62921
    @Shared
    def testCases = [
        systemTest1: [
            name: 'C232864',
            description: 'C232864, C323890'],
        systemTest2: [
            name: 'C232867',
            description: 'C232867'],
        systemTest3: [
            name: 'C233661',
            description: 'C233661'],
        systemTest4: [
            name: 'C323891',
            description: 'C323891'],
        systemTest5: [
            name: 'C232868',
            description: 'C232868'],
        systemTest6: [
            name: 'C323893',
            description: 'C323893'],
        systemTest7: [
            name: 'C323912',
            description: 'C323912, C323888'],
        systemTest8: [
            name: 'C323896',
            description: 'C323896'],
        systemTest9: [
            name: 'C323913',
            description: 'C323913'],    
    ]

    @Shared
    def scriptPaths = [
        'default': (EnvPropertiesHelper.getOS()=='UNIX') ? '/opt/jboss/bin/standalone.sh' : 'C:/tmp/jboss/bin/standalone.bat',
        'wrong': (EnvPropertiesHelper.getOS()=='UNIX') ? '/opt/jboss/bin/wrong.sh' : 'C:/tmp/jboss/bin/wrong.bat',
    ]

    @Shared
    def standaloneConfigs = [
        'empty': '',
        'default': 'standalone.xml',
        'full': 'standalone-full.xml'
    ]

    @Shared
    def additionalOptions = [
        'empty': '',
        'docker': '-b 0.0.0.0 -bmanagement 0.0.0.0',
        'custom path': '-Djboss.server.log.dir=/tmp/qa -b 0.0.0.0 -bmanagement 0.0.0.0',
        'error': '-b 0.0.0.0 -bmanagement error',
        'error and custom path': "-Djboss.server.log.dir=/tmp/qa -b 0.0.0.0 -bmanagement error"
    ]

    @Shared
    def logFileLocations = [
        'empty': '',
        'custom': (EnvPropertiesHelper.getOS()=='UNIX') ? '/tmp/qa/server.log' : 'C:/tmp/qa/server.log',
        'wrong': (EnvPropertiesHelper.getOS()=='UNIX') ? '/tmp/wrong/server.log' : 'C:/tmp/wrong/server.log',
    ]

    @Shared
    def summaries = [
        'default': jbossVersion in ['7.1', '7.0', '6.4'] ? "JBoss Standalone has been launched, server state is 'running'\nNo boot errors detected via CLI" : "JBoss Standalone has been launched, server state is 'running'",
        'started': "JBoss is already started in expected operating mode 'standalone'",
        'emptyConfig': "Configuration WrongConfig doesn't exist",
        'error': "Failed to connect to CLI for verication of server state",
        'wrongScript': "Failed to connect to CLI for verication of server state\nPlease refer to JBoss logs on file system for more information",
        'wrongLogs': jbossVersion in ['7.1', '7.0', '6.4'] ? "JBoss Standalone has been launched, server state is 'running'\nWarning: Cannot find JBoss log file '${logFileLocations.wrong}'\nNo boot errors detected via CLI" : "JBoss Standalone has been launched, server state is 'running'\nWarning: Cannot find JBoss log file '${logFileLocations.wrong}'",
    ]

    @Shared
    def jobLogs = [
        'default': ["JBoss Standalone has been launched, server state is 'running'", jbossVersion in ['7.1', '7.0', '6.4'] ? "No boot errors detected via CLI" : '', jbossVersion in ['7.1', '7.0', '6.4'] ? "Admin console listening on http://0.0.0.0:9990" : "", "\"${scriptPaths.'default'}\" -b 0.0.0.0 -bmanagement 0.0.0.0"],
        'started': ["JBoss is already started in expected operating mode 'standalone'"],
        'full config': ["JBoss Standalone has been launched, server state is 'running'", jbossVersion in ['7.1', '7.0', '6.4'] ? "No boot errors detected via CLI" : '', jbossVersion in ['7.1', '7.0', '6.4'] ? "Admin console listening on http://0.0.0.0:9990" : "", "\"${scriptPaths.'default'}\" --server-config=\"standalone-full.xml\"  -b 0.0.0.0 -bmanagement 0.0.0.0'"], 
    	'custom logs': ["JBoss Standalone has been launched, server state is 'running'", jbossVersion in ['7.1', '7.0', '6.4'] ? "No boot errors detected via CLI" : '', jbossVersion in ['7.1', '7.0', '6.4'] ? "Admin console listening on http://0.0.0.0:9990" : "", "\"${scriptPaths.'default'}\" -Djboss.server.log.dir=/tmp/qa -b 0.0.0.0 -bmanagement 0.0.0.0"],
    	'emptyConfig': ["Configuration WrongConfig doesn't exist"],
    	'error': ["Failed to connect to CLI for verication of server state", "\"${scriptPaths.'default'}\" -b 0.0.0.0 -bmanagement error"],
    	'error and custom path': ["Failed to connect to CLI for verication of server state", "\"${scriptPaths.'default'}\" -Djboss.server.log.dir=/tmp/qa -b 0.0.0.0 -bmanagement error"],
        'wrongScript': ["\"${scriptPaths.'wrong'}\" -b 0.0.0.0 -bmanagement 0.0.0.0", "Failed to connect to CLI for verication of server state", "Please refer to JBoss logs on file system for more information"],
        'wrongScript custom logs': ["\"${scriptPaths.'wrong'}\" -Djboss.server.log.dir=/tmp/qa -b 0.0.0.0 -bmanagement 0.0.0.0", "Failed to connect to CLI for verication of server state", "Please refer to JBoss logs on file system for more information"],
        'wrongLogs': ["Warning: Cannot find JBoss log file '${logFileLocations.wrong}'"],
    ]

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
        //deleteProject(projectName)
        //deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "StartStandaloneServer - positive"() {
        if (jbossShouldBeStopped) {
            runCliCommand(CliCommandsGeneratorHelper.shutDownStandalone())
        }

        def runParams = [
            additionalOptions: additionalOption,
            alternatejbossconfig: standaloneConfig,
            scriptphysicalpath: scriptPath,
            serverconfig: configName,
            logFileLocation: logFileLocation,

        ]
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

        if (jbossVersion in ['7.1', '7.0', '6.4']){
            assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getStandaloneStatus()).result == "running"
        }
        where: 'The following params will be: '
        testCaseId                 | configName         | scriptPath             | standaloneConfig             | additionalOption          	      | jbossShouldBeStopped | logs      			    | summary 				| jobExpectedStatus  | logFileLocation
        testCases.systemTest1.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'docker'	      | true                 | jobLogs.'default'		| summaries.'default'	|	"success"        | logFileLocations.'empty'
        testCases.systemTest2.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'docker'	      | false                | jobLogs.'started'		| summaries.'started'	|	"warning"        | logFileLocations.'empty'
        testCases.systemTest3.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'full'     | additionalOptions.'docker'	      | true                 | jobLogs.'full config'	| summaries.'default'	|	"success"        | logFileLocations.'empty'
        testCases.systemTest4.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'custom path'     | true                 | jobLogs.'custom logs'	| summaries.'default'	|	"success"        | logFileLocations.'empty'
        testCases.systemTest4.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'custom path'     | true                 | jobLogs.'custom logs'    | summaries.'default'   |   "success"        | logFileLocations.'custom'
        testCases.systemTest4.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'custom path'     | true                 | jobLogs.'wrongLogs'      | summaries.'wrongLogs' |   "warning"        | logFileLocations.'wrong'
    }

    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "StartStandaloneServer - negative"() {
        if (jbossShouldBeStopped) {
            runCliCommand(CliCommandsGeneratorHelper.shutDownStandalone())
        }

        def runParams = [
            additionalOptions: additionalOption,
            alternatejbossconfig: standaloneConfig,
            scriptphysicalpath: scriptPath,
            serverconfig: configName
        ]
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
        if (jbossShouldBeStarted){
            runParams = [
                additionalOptions: additionalOptions.'docker',
                alternatejbossconfig: standaloneConfigs.'empty',
                scriptphysicalpath: scriptPaths.'default',
                serverconfig: defaultConfigName,
                logFileLocation: logFileLocation,
            ] 
            runProcedureJob = runProcedureUnderTest(runParams)
            if (jbossVersion in ['7.1', '7.0', '6.4']){
                assert runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getStandaloneStatus()).result == "running"
            }
        }
        where: 'The following params will be: '
        testCaseId                 | configName         | scriptPath             | standaloneConfig             | additionalOption          	 			| jbossShouldBeStopped | jbossShouldBeStarted | logs      						  | summary 				| jobExpectedStatus  | logFileLocation
        testCases.systemTest5.name | "WrongConfig"	    | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'docker'	 			| true                 | false                |jobLogs.'emptyConfig'			  | summaries.'emptyConfig' | "error"            | logFileLocations.'empty'
        testCases.systemTest6.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'error'	     			| false                | false                |jobLogs.'error'				      | summaries.'error' 	    | "error"            | logFileLocations.'empty'
        testCases.systemTest7.name | defaultConfigName  | scriptPaths.'default'  | standaloneConfigs.'empty'    | additionalOptions.'error and custom path'	| false                | false                |jobLogs.'error and custom path'    | summaries.'error' 	    | "error"            | logFileLocations.'empty'
        testCases.systemTest8.name | defaultConfigName  | scriptPaths.'wrong'    | standaloneConfigs.'empty'    | additionalOptions.'docker'                | false                | false                |jobLogs.'wrongScript'              | summaries.'wrongScript' | "error"            | logFileLocations.'empty'
        testCases.systemTest9.name | defaultConfigName  | scriptPaths.'wrong'    | standaloneConfigs.'empty'    | additionalOptions.'custom path'           | false                | true                 |jobLogs.'wrongScript custom logs'  | summaries.'wrongScript' | "error"            | logFileLocations.'empty'
    }

}