import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class RemoveXADataSourceDomain extends PluginTestHelper {

	@Shared
    String procName = 'RemoveXADataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
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

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        // deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    static String getPathToMain(String path, String domain) {
        String pathForJar = ''
        if(EnvPropertiesHelper.getVersion() == "6.0"){
            pathForJar = "/opt/jboss/modules/$domain/$path/main"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\opt\\\\jboss\\\\modules\\\\$domain\\\\$path\\\\main" : pathForJar
        } else {
            pathForJar = "/opt/jboss/modules/system/layers/base/$domain/$path/main"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\opt\\\\jboss\\\\modules\\\\system\\\\layers\\\\base\\\\$domain\\\\$path\\\\main" : pathForJar
        }
        return  pathForJar
    }

    static String getPath(String path, String domain) {
        String pathForJar = ''
        if(EnvPropertiesHelper.getVersion() == "6.0"){
            pathForJar = "/opt/jboss/modules/$domain/$path"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\opt\\\\jboss\\\\modules\\\\$domain\\\\$path" : pathForJar
        } else {
            pathForJar = "/opt/jboss/modules/system/layers/base/$domain/$path"
            EnvPropertiesHelper.getOS() == "WINDOWS" ? pathForJar = "C:\\\\opt\\\\jboss\\\\modules\\\\system\\\\layers\\\\base\\\\$domain\\\\$path" : pathForJar
        }
        return  pathForJar
    }
    
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
        String path = getPathToMain("mysql", "com")
        createDir(getPath("mysql", "com"))
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        if(!(EnvPropertiesHelper.getVersion() ==~ '6.[0,1,2,3]')) {
            addModuleXADatasource(defaultProfile, jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")
        }
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        restartServer('master')
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, "mysql"))
    }

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
        String path = getPathToMain("postgresql", "org")
        createDir(getPath("postgresql", "org"))
        createDir(path)
        downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
        downloadArtifact(xml.postgresql, path+"/module.xml")
        addModuleXADatasource(defaultProfile, jdbcDriverName, "org.postgresql.xa.PGXADataSource")
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.postgresql, jdbcDriverName, 'org.postgresql.xa.PGXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource(defaultProfile) == null

        cleanup:
        restartServer('master')
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, "postgresql"))
    }

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
        restartServer('master')
    }

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
        assert runProcedureJob.getUpperStepSummary() =~ "Configuration no_name doesn't exist."
        assert runProcedureJob.getLogs() =~ "Configuration no_name doesn't exist."
    }

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

    void restartServer(String host){
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

    void addXADatasource(String profile, String name, String jndiName, String driverName, String xaDatasourceClass){
        runCliCommand(CliCommandsGeneratorHelper.addXADatasource(profile, name, jndiName, driverName, xaDatasourceClass))
    }
    
    void addModuleXADatasource(String profile, String driver, String DSclass){
        runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
    }

}