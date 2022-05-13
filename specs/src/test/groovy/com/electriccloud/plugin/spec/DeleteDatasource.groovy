package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.IgnoreRest
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Unroll

class DeleteDatasource extends PluginTestHelper {

    @Shared
    String procName = 'DeleteDatasource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultCliPath = ''
    @Shared
    String dataSourceConnectionCredentials = "dataSourceConnection_credential"
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
                procName: 'DeleteDataSource',
                params  : [
                        scriptphysicalpath  : '',
                        datasource_name     : '',
                        profile             : '',
                              config        : '',
                ]
        ]

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: 'CreateOrUpdateDataSource',
                params  : [
			            additionalOptions               : '',
			            connectionUrl                   : '',
			            dataSourceConnection_credential : '',
			            dataSourceName                  : '',
			            enabled                         : '',
			            jdbcDriverName                  : '',
			            jndiName                        : '',
			            profile                         : '',
			                  config                    : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createCredential(projectName, dataSourceConnectionCredentials, userNames.defaultUserName, passwords.defaultPassword)
        attachCredential(projectName, dataSourceConnectionCredentials, 'CreateOrUpdateDataSource')
        addJDBCMySQL(drivers.mysql)
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

        def dataSourceName = generateRandomDataSourceName()

        def runParams = [
                scriptphysicalpath  : defaultCliPath,
                datasource_name     : dataSourceName,
                profile             : '',
                      config        : defaultConfigName,
        ]

        def runParamsCreate = [
                additionalOptions               : '',
                connectionUrl                   : 'connectionUrl',
                dataSourceConnection_credential : dataSourceConnectionCredentials,
                dataSourceName                  : dataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : 'h2',
                jndiName                        : "java:/${dataSourceName}",
                profile                         : '',
                      config                    : defaultConfigName

        ]
        def credential = [
                credentialName: dataSourceConnectionCredentials,
                userName: 'admin',
                password: 'changeme'
        ]

        setup:

        // Jboss 6.1 removes driver after its reboot
        if (jbossVersion == '6.1' && testCaseId == testCases.systemTest4.name) {
            addJDBCMySQL(drivers.mysql)
        }
        RunProcedureJob runProcedureDsl = runProcedureDsl(projectName, 'CreateOrUpdateDataSource', runParamsCreate, credential)
        // we should reboot Jboss after adding of DS in version 6.1
        // Jboss 6.1 has next logic: after a creation all DS's have status "is enabled-false", after Jboss was rebooted, all DS's  have status "is enabled-true"
        if (jbossVersion == '6.1') {
            reloadServer()
        }

        when:

        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runCliCommandAnyResult(CliCommandsGeneratorHelper.getDatasourceInfo(dataSourceName, '')).isStatusError()

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