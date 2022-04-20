package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import spock.lang.*
import static org.junit.Assume.*
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity

class CreateOrUpdateDataSource extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateDataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String dataSourceConnectionCredentials = "dataSourceConnectionCredentials"
    @Shared
    def passwords = [
        defaultPassword: 'changeme',
        empty: ''
        ]
    @Shared
    def userNames = [
        defaultUserName: 'admin',
        empty: ''
        ]

    @Shared
    def jbossVersion = EnvPropertiesHelper.getVersion()

    //https://ecflow.testrail.net/index.php?/suites/view/27&group_by=cases:section_id&group_order=asc&group_id=82570
    @Shared
    def testCases = [
        // for standalone
        systemTest1: [
            name: 'C323725',
            description: 'C323725, C323734, C323736, C323738, C323740, C323742, C323743'],
        systemTest2: [
            name: 'C323737',
            description: 'C323737'],
        systemTest3: [
            name: 'C323735',
            description: 'C323735'],
        systemTest4: [
            name: 'C323741',
            description: 'C323741'],
        systemTest5: [
            name: 'C323818',
            description: 'C323818'],
        systemTest6: [
            name: 'C323744',
            description: 'C323744'],
        systemTest7: [
            name: 'C323745',
            description: 'C323745'],
        systemTest8: [
            name: 'C323755',
            description: 'C323755'],
        systemTest9: [
            name: 'C323756',
            description: 'C323756'],
        systemTest10: [
            name: 'C323757',
            description: 'C323757'],
        systemTest11: [
            name: 'C323758',
            description: 'C323758'],
        systemTest12: [
            name: 'C323759',
            description: 'C323759'],
        systemTest13: [            
            name: 'C323761',
            description: 'C323761'],
        systemTest14: [            
            name: 'C323762',
            description: 'C323762'],     
        systemTest15: [            
            name: 'C323820',
            description: 'C323820'],
        systemTest16: [            
            name: 'C323763',
            description: 'C323763'],
        systemTest17: [            
            name: 'C323764',
            description: 'C323764'],
        systemTest18: [            
            name: 'C323819',
            description: 'C323819'],
        systemTest19: [            
            name: 'C323739',
            description: 'C323739'],
        systemTest20: [            
            name: 'C323765',
            description: 'C323765'],
        systemTest21: [            
            name: 'C323766',
            description: 'C323766'],
        systemTest22: [            
            name: 'C323768',
            description: 'C323768'],
        systemTest23: [            
            name: 'C323769',
            description: 'C323769'],
        systemTest24: [            
            name: 'C323770',
            description: 'C323770'],
        // for domain
        systemTest25: [            
            name: 'C323774',
            description: 'C323774, C323777, C323815, C323775, C323779, C323780, C323782, C323783'],
        systemTest26: [            
            name: 'C323778',
            description: 'C323778'],
        systemTest27: [            
            name: 'C323776',
            description: 'C323776'],
        systemTest28: [            
            name: 'C323781',
            description: 'C323781'],
        systemTest29: [            
            name: 'C323821',
            description: 'C323821'],
        systemTest30: [            
            name: 'C323784',
            description: 'C323784'],
        systemTest31: [            
            name: 'C323785',
            description: 'C323785'],
        systemTest32: [            
            name: 'C323787',
            description: 'C323787'],
        systemTest33: [            
            name: 'C323788',
            description: 'C323788'],                                                             
        systemTest34: [            
            name: 'C323789',
            description: 'C323789'],
        systemTest35: [            
            name: 'C323790',
            description: 'C323790'],
        systemTest36: [            
            name: 'C323791',
            description: 'C323791'],          
        systemTest37: [            
            name: 'C323792',
            description: 'C323792'],  
        systemTest38: [            
            name: 'C323793',
            description: 'C323793'],  
        systemTest39: [            
            name: 'C323794',
            description: 'C323794'],
        systemTest40: [            
            name: 'C323806',
            description: 'C323806'],
        systemTest41: [            
            name: 'C323825',
            description: 'C323825'],
        systemTest42: [            
            name: 'C323805',
            description: 'C323805'],                                                             
        systemTest43: [            
            name: 'C323817',
            description: 'C323817'],
        systemTest44: [            
            name: 'C323807',
            description: 'C323807'],
        systemTest45: [            
            name: 'C323808',
            description: 'C323808'],          
        systemTest46: [            
            name: 'C323810',
            description: 'C323810'],  
        systemTest47: [            
            name: 'C323811',
            description: 'C323811'],  
    ] 

    @Shared
    def summaries = [
        'default': "Data source 'dsName1' has been added successfully",
        jndiUpdate: "Data source 'dsName1' has been updated successfully by new jndi name",
        nameUpdate: "Data source 'dsName1' has been updated successfully by new user name, password.",
        nameAndJndiUpdate: "Data source 'dsName1' has been updated successfully by new jndi name, user name, password.",
        notUpdate: "Data source 'dsName1' is up-to-date",
        emptyConfig: null,
        emptyProfile: "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)",
        emptyDSName: "Required parameter 'dataSourceName' is not provided",
        emptyJNDIName: "Required parameter 'jndiName' is not provided",
        emptyDriver: "Required parameter 'jdbcDriverName' is not provided",
        emptyUrl: jbossVersion == '7.1' ? "Unable to start the ds because it generated more than one cf" : 'Required parameter \'connectionUrl\' is not provided \\(parameter required for JBoss EAP 6.X and 7.0\\)',
        wrongJNDI: "Jndi name have to start with java:/ or java:jboss/",
        wrongConfig: "Configuration WrongConfig doesn't exist.",
        wrongDriver: (jbossVersion == '6.0' && EnvPropertiesHelper.getMode() == 'standalone') ? 'Driver named "h2_wrong" is not installed' : (jbossVersion in  ['6.0', '6.1', '6.2', '6.3', '6.4'] && EnvPropertiesHelper.getMode() == 'domain' ) ? "Operation failed or was rolled back on all servers" : jbossVersion != '7.1' ? "is missing \\[jboss.jdbc-driver.h2_wrong\\]" : "Required services that are not installed",
        wrongOptions: jbossVersion != '7.1' ? "Unrecognized argument --min-poolzzz for command" : "Unrecognized arguments: [--min-poolzzz]",
        wrongProfile: /porfile.*not found/,   
    ]

    @Shared
    def jobLogs = [
        'default': "Data source 'dsName1' does not exist - to be created",
        jndiUpdate: "JNDI name differs and to be updated: current 'jndiName1' VS specified in parameters 'jndiName2",
        nameUpdate: "User name differs and to be updated: current 'Name1' VS specified in parameters 'Name2'",
        notUpdate: "Updatable attributes match - no updates will be performed",
        emptyConfig: null,
        emptyProfile: "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)",
        emptyDSName: "Setting property 'summary' = 'Required parameter 'dataSourceName' is not provided",
        emptyJNDIName: "Setting property 'summary' = 'Required parameter 'jndiName' is not provided",
        emptyDriver: "Setting property 'summary' = 'Required parameter 'jdbcDriverName' is not provided",
        emptyUrl: jbossVersion == '7.1' ? "Unable to start the ds because it generated more than one cf" : 'Required parameter \'connectionUrl\' is not provided \\(parameter required for JBoss EAP 6.X and 7.0\\)',
        wrongJNDI: "Jndi name have to start with java:/ or java:jboss/",
        wrongConfig: "Configuration WrongConfig doesn't exist.",
        wrongDriver: (jbossVersion == '6.0' && EnvPropertiesHelper.getMode() == 'standalone') ? 'Driver named "h2_wrong" is not installed' : (jbossVersion in  ['6.0', '6.1', '6.2', '6.3', '6.4'] && EnvPropertiesHelper.getMode() == 'domain' ) ? "Operation failed or was rolled back on all servers" : jbossVersion != '7.1' ? "is missing \\[jboss.jdbc-driver.h2_wrong\\]" : "Required services that are not installed",
        wrongOptions: jbossVersion != '7.1' ? "Unrecognized argument --min-poolzzz for command" : "Unrecognized arguments: [--min-poolzzz]",
        wrongProfile: /porfile.*not found/,       
    ]

    @Shared
    def dataSourceNames = [
        'default': 'testDS',
        'escape': EnvPropertiesHelper.getOS() == "WINDOWS" ? 'test\\\\\\\\ DS' : 'test\\\\ DS'
    ]

    @Shared
    def jndiNames = [
        'default': 'java:/testDS',
        'wrong': 'wrong_testDS'
    ]

    @Shared
    def statusOfEnabled = [
        'true': '1',
        'false': '0'
        ]

    @Shared
    def drivers = [
        h2: 'h2',
        mariadb: 'mariadb',
        mysql: 'mysql',
        wrong: 'h2_wrong',
    ]

    @Shared
    def link = [
        /**
         * Required
         */
        empty: '',
        mysql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/mysql/mysql-connector-java-5.1.36.jar",
        postgresql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/postgresql/postgresql-42.2.2.jar",
    ]

    @Shared
    def xml = [
        /**
         * Required
         */
        empty: '',
        mysql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/mysql/module.xml",
        postgresql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/postgresql/module.xml",
    ]

    @Shared
    def urls = [
        'default': 'connectionUrl',
        'empty': ''
    ]

    @Shared
    def additionalOptions = [
        'empty': '',
        'url': '--connection-url=testUrlAddOptions',
        'min size': '--min-pool-size=5',
        'minmax size': '--min-pool-size=5 --max-pool-size=10',
        'simple sql': '--check-valid-connection-sql="select 1, 2"',
        'complex sql': '--check-valid-connection-sql="INSERT INTO jboss_table VALUES (34, \\\'qa34\\\');"',
        "wrong": "--min-poolzzz" 
    ]

    @Shared
    def profiles = [
        'empty': "",
        'full': "full",
        'wrong': "porfile123",
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
			            connectionUrl: '',
			            dataSourceConnectionCredentials: '',
			            dataSourceName: '',
			            enabled: '',
			            jdbcDriverName: '',
			            jndiName: '',
			            profile: '',
			            serverconfig: '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createCredential(projectName, dataSourceConnectionCredentials, userNames.defaultUserName, passwords.defaultPassword)
        attachCredential(projectName, dataSourceConnectionCredentials, procName)
        addJDBCMySQL(drivers.mysql)
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        conditionallyDeleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }


    RunProcedureJob runProcedureUnderTest(def parameters, def credential) {
        return runProcedureDsl(projectName, procName, parameters, credential)
    }

    @Sanity
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "Sanity"() {
        // should be test ignored ?
        // if "shouldBeIgnored" is true, test will be skipped
        assumeFalse(shouldBeIgnored)

        // modify credentialds for tests: C323744, C323745
        if (testCaseId in [testCases.systemTest6.name, testCases.systemTest7.name]){
            modifyCredential(projectName, creds, userName, password)
        }

        def runParams = [
                additionalOptions: additionalOption,
                connectionUrl: url,
                dataSourceConnectionCredentials: creds,
                dataSourceName: dsName,
                enabled: enabled,
                jdbcDriverName: jdbcDriverName,
                jndiName: jndiName,
                profile: profile,
                serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        // Jboss 6.1 removes driver after its reboot
        if (jbossVersion == '6.1' && testCaseId == testCases.systemTest4.name) {
            addJDBCMySQL(drivers.mysql)
        }
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // we should reboot Jboss after adding of DS in version 6.1
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true"
        if (jbossVersion == '6.1') {
            reloadServer()
        }
        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "success"
        if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
            jobExpectedStatus = "warning"
        }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summaries.'default'.replace("dsName1", dsName)
        assert procedureLogs =~ jobLogs.'default'.replace("dsName1", dsName)
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption)

        cleanup:
        // put credentialds in the proper state after execution of tests: C323744, C323745
        if (testCaseId in [testCases.systemTest6.name, testCases.systemTest7.name]){
            modifyCredential(projectName, creds, userNames.defaultUserName, passwords.defaultPassword)
        }

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jdbcDriverName  | url            | creds                            | userName                  | password                  | enabled                 | profile         | additionalOption                 | shouldBeIgnored
        testCases.systemTest1.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | false
        testCases.systemTest2.name      | defaultConfigName  | dataSourceNames.'escape'+testCaseId    | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | !(jbossVersion in ['7.1', '7.0'])
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "CreateorUpdateXADataSource - standalone - create DS - positive"() {
        // should be test ignored ? 
        // if "shouldBeIgnored" is true, test will be skipped
        assumeFalse(shouldBeIgnored)

        // modify credentialds for tests: C323744, C323745
        if (testCaseId in [testCases.systemTest6.name, testCases.systemTest7.name]){
            modifyCredential(projectName, creds, userName, password)
        }

        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        // Jboss 6.1 removes driver after its reboot
        if (jbossVersion == '6.1' && testCaseId == testCases.systemTest4.name) {
                addJDBCMySQL(drivers.mysql) 
            } 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // we should reboot Jboss after adding of DS in version 6.1 
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true" 
        if (jbossVersion == '6.1') {
                reloadServer()
            }
        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "success"
        if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
            jobExpectedStatus = "warning"
        }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summaries.'default'.replace("dsName1", dsName)
        assert procedureLogs =~ jobLogs.'default'.replace("dsName1", dsName)
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption)

        cleanup:
        // put credentialds in the proper state after execution of tests: C323744, C323745  
        if (testCaseId in [testCases.systemTest6.name, testCases.systemTest7.name]){
            modifyCredential(projectName, creds, userNames.defaultUserName, passwords.defaultPassword)
        }

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jdbcDriverName  | url            | creds                            | userName                  | password                  | enabled                 | profile         | additionalOption                 | shouldBeIgnored
        testCases.systemTest1.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | false     
        testCases.systemTest2.name      | defaultConfigName  | dataSourceNames.'escape'+testCaseId    | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | !(jbossVersion in ['7.1', '7.0'])
        testCases.systemTest3.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'false' | profiles.empty  | additionalOptions.'empty'        | jbossVersion == '6.1'
        testCases.systemTest4.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.mysql   | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | false
        testCases.systemTest5.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'empty'   | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'url'          | jbossVersion != '7.1' 
        testCases.systemTest6.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.empty           | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | false
        testCases.systemTest7.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.empty           | passwords.empty           | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'        | false
        testCases.systemTest8.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.full   | additionalOptions.'empty'        | false
        testCases.systemTest9.name      | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'min size'     | false
        testCases.systemTest10.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'minmax size'  | false     
        testCases.systemTest11.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'simple sql'   | false 
        testCases.systemTest12.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'complex sql'  | jbossVersion in ['7.0', '6.4']     
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'domain' })
    @Unroll
    def "CreateorUpdateXADataSource - domain - create DS - positive"() {
        // should be test ignored ? 
        // if "shouldBeIgnored" is true, test will be skipped
        assumeFalse(shouldBeIgnored)
        // modify credentialds for tests: C323784, C323785
        if (testCaseId in [testCases.systemTest30.name, testCases.systemTest31.name]){
            modifyCredential(projectName, creds, userName, password)
        }

        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        // Jboss 6.1 removes driver after its reboot
        if (jbossVersion == '6.1' && testCaseId == testCases.systemTest28.name) {
                addJDBCMySQL(drivers.mysql) 
            } 
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // we should reboot Jboss after adding of DS in version 6.1 
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true" 
        if (jbossVersion == '6.1') {
                reloadServer('master')
            }
        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "success"
        if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
            jobExpectedStatus = "warning"
        }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summaries.'default'.replace("dsName1", dsName)
        assert procedureLogs =~ jobLogs.'default'.replace("dsName1", dsName)
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption, profile)

        cleanup:
        // put credentialds in the proper state after execution of tests: C323784, C323785  
        if (testCaseId in [testCases.systemTest30.name, testCases.systemTest31.name]){
            modifyCredential(projectName, creds, userNames.defaultUserName, passwords.defaultPassword)
        }

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jdbcDriverName  | url            | creds                            | userName                  | password                  | enabled                 | profile         | additionalOption                   | shouldBeIgnored
        testCases.systemTest24.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'          | false
        testCases.systemTest26.name     | defaultConfigName  | dataSourceNames.'escape'+testCaseId    | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'          | !(jbossVersion in ['7.1', '7.0'])
        testCases.systemTest27.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'false' | profiles.'full' | additionalOptions.'empty'          | jbossVersion == '6.1'
        testCases.systemTest28.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.mysql   | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'          | false
        testCases.systemTest29.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'empty'   | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'url'            | jbossVersion != '7.1'
        testCases.systemTest30.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.empty           | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'          | false
        testCases.systemTest31.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.empty           | passwords.empty           | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'          | false
        testCases.systemTest32.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'min size'       | false
        testCases.systemTest33.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'minmax size'    | false
        testCases.systemTest34.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'simple sql'     | false
        testCases.systemTest35.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'complex sql'    | jbossVersion in ['7.0', '6.4']
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "CreateorUpdateXADataSource - standalone - Update DS - positive"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // update values:
        jndiName = jndiNameNew
        runParams['jndiName'] = jndiName
        // modify credentialds for tests: C323761, C323762
        if (testCaseId in [testCases.systemTest14.name, testCases.systemTest15.name]){
            userName = userNameNew
            password = passwordNew
            modifyCredential(projectName, creds, userName, password)
        }

        runProcedureJob = runProcedureUnderTest(runParams, credential)
        // we should reboot Jboss after adding of DS in version 6.1 
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true" 
        if (jbossVersion == '6.1') {
                reloadServer()
            }
        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "success"
        if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
            jobExpectedStatus = "warning"
        }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption)

        cleanup:
        // put credentialds in the proper state after execution of tests: C323761, C323762  
        if (testCaseId in [testCases.systemTest14.name, testCases.systemTest15.name]){
            modifyCredential(projectName, creds, userNames.defaultUserName, passwords.defaultPassword)
        }

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jndiNameNew                     | jdbcDriverName  | url            | creds                            | userName                  | userNameNew       | password                  | passwordNew       | enabled                 | profile         | additionalOption            |   logs                                                                                | summary
        testCases.systemTest13.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiName+"new"                  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'   | jobLogs.jndiUpdate.replace("jndiName1", jndiName).replace("jndiName2", jndiNameNew)   | summaries.jndiUpdate.replace("dsName1", dsName)
        testCases.systemTest14.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'default'+testCaseId  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName+"new"    | passwords.defaultPassword | password+"new"    | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'   | jobLogs.nameUpdate.replace("Name1", userName).replace("Name2", userNameNew)           | summaries.nameUpdate.replace("dsName1", dsName) 
        testCases.systemTest15.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiName+"new"                  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName+"new"    | passwords.defaultPassword | password+"new"    | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'   | jobLogs.jndiUpdate.replace("jndiName1", jndiName).replace("jndiName2", jndiNameNew)   | summaries.nameAndJndiUpdate.replace("dsName1", dsName) 
        testCases.systemTest16.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'default'+testCaseId  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'   | jobLogs.notUpdate                                                                     | summaries.notUpdate.replace("dsName1", dsName)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'domain' })
    @Unroll
    def "CreateorUpdateXADataSource - domain - Update DS - positive"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // update values:
        jndiName = jndiNameNew
        runParams['jndiName'] = jndiName
        // modify credentialds for tests: C323792, C323793
        if (testCaseId in [testCases.systemTest37.name, testCases.systemTest38.name]){
            userName = userNameNew
            password = passwordNew
            modifyCredential(projectName, creds, userName, password)
        }

        runProcedureJob = runProcedureUnderTest(runParams, credential)
        // we should reboot Jboss after adding of DS in version 6.1 
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true" 
        if (jbossVersion == '6.1') {
                reloadServer('master')
            }
        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "success"
        if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
            jobExpectedStatus = "warning"
        }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption, profile)

        cleanup:
        // put credentialds in the proper state after execution of tests: C323792, C323793  
        if (testCaseId in [testCases.systemTest37.name, testCases.systemTest38.name]){
            modifyCredential(projectName, creds, userNames.defaultUserName, passwords.defaultPassword)
        }

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jndiNameNew                     | jdbcDriverName  | url            | creds                            | userName                  | userNameNew       | password                  | passwordNew       | enabled                 | profile         | additionalOption            |   logs                                                                                | summary
        testCases.systemTest36.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiName+"new"                  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'   | jobLogs.jndiUpdate.replace("jndiName1", jndiName).replace("jndiName2", jndiNameNew)   | summaries.jndiUpdate.replace("dsName1", dsName)
        testCases.systemTest37.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'default'+testCaseId  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName+"new"    | passwords.defaultPassword | password+"new"    | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'   | jobLogs.nameUpdate.replace("Name1", userName).replace("Name2", userNameNew)           | summaries.nameUpdate.replace("dsName1", dsName) 
        testCases.systemTest38.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiName+"new"                  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName+"new"    | passwords.defaultPassword | password+"new"    | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'   | jobLogs.jndiUpdate.replace("jndiName1", jndiName).replace("jndiName2", jndiNameNew)   | summaries.nameAndJndiUpdate.replace("dsName1", dsName) 
        testCases.systemTest39.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'default'+testCaseId  | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'   | jobLogs.notUpdate                                                                     | summaries.notUpdate.replace("dsName1", dsName)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "CreateorUpdateXADataSource - standalone - create DS - negative"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName
        ]

        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "error"

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jdbcDriverName  | url            | creds                            | userName                  | password                  | enabled                 | profile         | additionalOption           | logs                  | summary
        testCases.systemTest17.name     | ""                 | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.emptyConfig   | summaries.emptyConfig
        testCases.systemTest17.name     | defaultConfigName  | ""                                     | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.emptyDSName   | summaries.emptyDSName
        testCases.systemTest17.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | ""                               | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.emptyJNDIName | summaries.emptyJNDIName
        testCases.systemTest17.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | ""              | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.emptyDriver   | summaries.emptyDriver
        testCases.systemTest18.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | ""             | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.emptyUrl      | summaries.emptyUrl
        testCases.systemTest19.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'wrong'+testCaseId     | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.wrongJNDI     | summaries.wrongJNDI
        testCases.systemTest20.name     | "WrongConfig"      | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.wrongConfig   | summaries.wrongConfig
        testCases.systemTest21.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.'wrong' | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'empty'  | jobLogs.wrongDriver   | summaries.wrongDriver
        testCases.systemTest23.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.empty  | additionalOptions.'wrong'  | jobLogs.wrongOptions  | summaries.wrongOptions
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'domain' })
    @Unroll
    def "CreateorUpdateXADataSource - domain - create DS - negative"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName
        ]

        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "error"

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jdbcDriverName  | url            | creds                            | userName                  | password                  | enabled                 | profile         | additionalOption           | logs                  | summary
        testCases.systemTest40.name     | ""                 | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.emptyConfig   | summaries.emptyConfig
        testCases.systemTest40.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'empty'| additionalOptions.'empty'  | jobLogs.emptyProfile  | summaries.emptyProfile
        testCases.systemTest40.name     | defaultConfigName  | ""                                     | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.emptyDSName   | summaries.emptyDSName
        testCases.systemTest40.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | ""                               | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.emptyJNDIName | summaries.emptyJNDIName
        testCases.systemTest40.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | ""              | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.emptyDriver   | summaries.emptyDriver
        testCases.systemTest41.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | ""             | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.emptyUrl      | summaries.emptyUrl
        testCases.systemTest42.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'wrong'+testCaseId     | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.wrongJNDI     | summaries.wrongJNDI
        testCases.systemTest43.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'wrong'| additionalOptions.'empty'  | jobLogs.wrongProfile  | summaries.wrongProfile
        testCases.systemTest44.name     | "WrongConfig"      | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.wrongConfig   | summaries.wrongConfig
        testCases.systemTest45.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.'wrong' | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'empty'  | jobLogs.wrongDriver   | summaries.wrongDriver
        testCases.systemTest46.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | passwords.defaultPassword | statusOfEnabled.'true'  | profiles.'full' | additionalOptions.'wrong'  | jobLogs.wrongOptions  | summaries.wrongOptions
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'standalone' })
    @Unroll
    def "CreateorUpdateXADataSource - standalone - Update DS - negative"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // update values:
        runParams['jndiName'] = jndiNameNew
        runProcedureJob = runProcedureUnderTest(runParams, credential)

        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "error"

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs
        // verify that ds is not updated with wrong values
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption)

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jndiNameNew                     | jdbcDriverName  | url            | creds                            | userName                  | userNameNew       | password                  | passwordNew       | enabled                  | profile         | additionalOption            |   logs              | summary
        testCases.systemTest24.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'wrong'               | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'false'  | profiles.empty  | additionalOptions.'empty'   | jobLogs.wrongJNDI   | summaries.wrongJNDI
   }

    @NewFeature(pluginVersion = "2.6.0")
    @Requires({ env.JBOSS_MODE == 'domain' })
    @Unroll
    def "CreateorUpdateXADataSource - domain - Update DS - negative"() {
        def runParams = [
            additionalOptions: additionalOption,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // update values:
        runParams['jndiName'] = jndiNameNew
        runProcedureJob = runProcedureUnderTest(runParams, credential)

        def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def procedureLogs = runProcedureJob.getLogs()
        def jobExpectedStatus = "error"

        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        assert jobUpperStepSummary =~ summary
        assert procedureLogs =~ logs
        // verify that ds is not updated with wrong values
        checkCreateDataSource(dsName, jndiName, jdbcDriverName, enabled, password, userName, url, additionalOption, profile)

        where: 'The following params will be: '
        testCaseId                      | configName         | dsName                                 | jndiName                         | jndiNameNew                     | jdbcDriverName  | url            | creds                            | userName                  | userNameNew       | password                  | passwordNew       | enabled                  | profile         | additionalOption            |   logs              | summary
        testCases.systemTest47.name     | defaultConfigName  | dataSourceNames.'default'+testCaseId   | jndiNames.'default'+testCaseId   | jndiNames.'wrong'               | drivers.h2      | urls.'default' | dataSourceConnectionCredentials  | userNames.defaultUserName | userName          | passwords.defaultPassword | password          | statusOfEnabled.'false'  | profiles.'full' | additionalOptions.'empty'   | jobLogs.wrongJNDI   | summaries.wrongJNDI
   }

    void checkCreateDataSource(def nameDatasource, def jndiNames, def jdbcDriverName, def enabled, def password, def userName, def url, def additionalOption, def profile=null){
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDatasourceInfo(nameDatasource, profile)).result
        // if we send more than one additinonal options we will verify them in loop
        if (additionalOption.count("=") > 1){
            for(addOptions in additionalOption.tokenize(' ')) {
                if(addOptions != ''){
                    def addOption = addOptions.tokenize('=')
                    assert result[addOption[0][2..-1]].toString() == addOption[1]
                }
            }
        }
        else if (additionalOption.count("=") == 1) {
            def addOptions = additionalOption.tokenize('=')
            if (addOptions[0].contains('check-valid-connection-sql')) {
                // option is check-valid-connection-sql, we remove double quotes
                addOptions[1] = addOptions[1][1..-2]
                //removed slashes from complex sql
                addOptions[1] = addOptions[1].replace("\\", "")
            }
            assert result[addOptions[0][2..-1]].toString() == addOptions[1]
            // if additionalOption "--connection-url=testUrlAddOptions"
            if (additionalOption.contains('connection-url')) {
            // then url = testUrlAddOptions
                url = addOptions[1]
            }
        }

        if (password=="") {password = null}
        if (userName=="") {userName = null}

        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
        assert result.'connection-url' == url



    }

    static String getPathToMain(String path, String domain) {
        String pathForJar = ''
        if(EnvPropertiesHelper.getVersion() == "6.0"){
            pathForJar = "/opt/jboss/modules/$domain/$path/main"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\tmp\\\\jboss\\\\modules\\\\$domain\\\\$path\\\\main" : pathForJar
        } else {
            pathForJar = "/opt/jboss/modules/system/layers/base/$domain/$path/main"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\tmp\\\\jboss\\\\modules\\\\system\\\\layers\\\\base\\\\$domain\\\\$path\\\\main" : pathForJar
        }
        return  pathForJar
    }

    static String getPath(String path, String domain) {
        String pathForJar = ''
        if(EnvPropertiesHelper.getVersion() == "6.0"){
            pathForJar = "/opt/jboss/modules/$domain/$path"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\tmp\\\\jboss\\\\modules\\\\$domain\\\\$path" : pathForJar
        } else {
            pathForJar = "/opt/jboss/modules/system/layers/base/$domain/$path"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\tmp\\\\jboss\\\\modules\\\\system\\\\layers\\\\base\\\\$domain\\\\$path" : pathForJar
        }
        return  pathForJar
    }

    void addJDBCMySQL(String jdbcDriverName){
        String path = getPathToMain("mysql", "com")
        createDir(getPath("mysql", "com"))
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        addModuleXADatasource(jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")
    }

    void addModuleXADatasource(String driver, String DSclass){
        if (EnvPropertiesHelper.getMode() == 'standalone') {
            if (jbossVersion in ['6.0','6.1','6.2','6.3']) {
                // https://issues.jboss.org/browse/JBPAPP6-944
                reloadServer()
            }
            runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasourceStandalone(driver, DSclass))
        }
        else {
            if (jbossVersion in ['6.0','6.1','6.2','6.3']) {
                // https://issues.jboss.org/browse/JBPAPP6-944
                reloadServer('master')
            } 
            runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasource(profiles.'full', driver, DSclass))
        }
    }

    void reloadServer(host=null) {
        def reloadCommand
        def getStatusCommand
        if (host){
            reloadCommand = CliCommandsGeneratorHelper.reloadHostDomain(host)
            getStatusCommand = CliCommandsGeneratorHelper.getHostStatus(host)
        }
        else{
            reloadCommand = CliCommandsGeneratorHelper.reloadStandalone()
            getStatusCommand = CliCommandsGeneratorHelper.getStandaloneStatus()
        }
        runCliCommandAnyResult(reloadCommand)
        def cond = true
        while(cond){
            try {
                sleep(3000)
                if (runCliCommandAndGetJBossReply(getStatusCommand).result == 'running') {
                    cond = false
                }
            }
            catch (Exception e){
                println e.getMessage()
            }
        }
    }

}