package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import spock.lang.*
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class RemoveXADataSourceDomain extends PluginTestHelper {

	@Shared
    String procName = 'RemoveXADataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
    @Shared
    String defaultProfile = 'full'
    @Shared
    def jndiName = [
            /**
             * Required
             */
            empty: '',
            mysql: 'java:/MysqlXADS',
            postgresql: 'java:/PostgresXADS',
            oracle: 'java:/XAOracleDS',
            sqlserver: 'java:/MSSQLXADS',
            ibmdb2: 'java:/DB2XADS',
            sybase: 'java:/SybaseXADS',
            mariadb: 'java:jboss/MariaDBXADS',
            h2: 'java:/H2XADS'
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
                        profile      : '',
                        serverconfig : '',
                        dataSourceName:    '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        addJDBCMySQL("mysql")
        addJDBCPostgres("postgresql")

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        // conditionallyDeleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
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

    @Sanity
    @Unroll
    def "Sanity"() {
        String testCaseId = "C289593"
        String jdbcDriverName = "mysql"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'MysqlXADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        reloadServer('master')
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, MySQL C289593"() {
        String testCaseId = "C289593"
        String jdbcDriverName = "mysql"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'MysqlXADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        reloadServer('master')
    }

    @Ignore
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource Enabled XA dataSource, MySQL C289593"() {
        String testCaseId = "C289593"
        String jdbcDriverName = "mysql"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'MysqlXADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource', true)
        reloadServer('master')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        reloadServer('master')
    }

    @IgnoreIf({EnvPropertiesHelper.getVersion() in ['6.0', '6.1', '6.2', '6.3']})
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, PostgreSQL C289594"() {
        String testCaseId = "C289594"
        String jdbcDriverName = "postgresql"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'PostgresXADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.postgresql, jdbcDriverName, 'org.postgresql.xa.PGXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        reloadServer('master')
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, H2 C290242"() {
        String testCaseId = "C290242"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'H2XADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.h2, jdbcDriverName, 'org.h2.jdbcx.JdbcDataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        reloadServer('master')
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, not existing 'Configuration name' C289610"() {
        String testCaseId = "C289610"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : 'no_name',
                dataSourceName   : 'H2XADS',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "error"
        //        TODO: uncomment on fix https://cloudbees.atlassian.net/browse/BEE-18013
//        assert runProcedureJob.getUpperStepSummary() =~ "Configuration no_name doesn't exist."
//        assert runProcedureJob.getLogs() =~ "Configuration no_name doesn't exist."
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, without 'Profile' C289609"() {
        String testCaseId = "C289609"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'H2XADS',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided"
        assert runProcedureJob.getLogs() =~ "Required parameter 'profile' is not provided"
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "   RemoveXADataSource, not existing 'Data Source Name' C289611"() {
        String testCaseId = "C289611"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                dataSourceName   : 'no_name',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source 'no_name' not found"
        assert runProcedureJob.getLogs() =~ "XA data source 'no_name' not found"
    }

    // Bug P3 http://jira/browse/ECPAPPSERVERJBOSS-667
    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "RemoveXADataSource, not existing 'full' C289612"() {
        String testCaseId = "C289612"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : 'not_full',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'H2XADS',
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary().contains("'[(\\\"profile\\\" => \\\"not_full\\\")]'")
        assert runProcedureJob.getLogs().contains("'[(\\\"profile\\\" => \\\"not_full\\\")]'")
    }

    String getListOfXADataSource(String profile) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getListOfXADatasourceInDomain(profile)).result."xa-data-source"
        return result
    }

    void reloadServer(String host){
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.reloadHostDomain(host))
        def cond = true
        while(cond){
            try {
                sleep(3000)
                if (runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getHostStatus(host)).result == 'running') {
                    cond = false
                }
            }
            catch (Exception e){
                println e.getMessage()
            }
        }
    }        

    void addXADatasource(String profile, String name, String jndiName, String driverName, String xaDatasourceClass, def enabled=false){
        runCliCommand(CliCommandsGeneratorHelper.addXADatasource(profile, name, jndiName, driverName, xaDatasourceClass, enabled))
    }
    

    void addJDBCMySQL(String jdbcDriverName, String profile=defaultProfile){
        String path = getPathToMain("mysql", "com")
        createDir(getPath("mysql", "com"))
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        addModuleXADatasource(profile, jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")
    }

    void addJDBCPostgres(String jdbcDriverName){
        String path = getPathToMain("postgresql", "org")
        createDir(getPath("postgresql", "org"))
        createDir(path)
        downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
        downloadArtifact(xml.postgresql, path+"/module.xml")
        addModuleXADatasource(defaultProfile, jdbcDriverName, "org.postgresql.xa.PGXADataSource")
    }

    void addModuleXADatasource(String profile, String driver, String DSclass){
        if (EnvPropertiesHelper.getVersion() in ['6.0','6.1','6.2','6.3']) {
            // https://issues.jboss.org/browse/JBPAPP6-944
            reloadServer('master')
        } 
        runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
    }

}