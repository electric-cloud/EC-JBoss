import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class RemoveXADataSourceStandalone extends PluginTestHelper {

	@Shared
    String procName = 'RemoveXADataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
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
        deleteProject(projectName)
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
    
    @Unroll
    def "RemoveXADataSource, MySQL  C289614"() {
        String testCaseId = "   C289614"
        String jdbcDriverName = "mysql"
        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'MysqlXADS',
        ]
        setup:
        addJDBCMySQL("mysql")
        def dataSourceName = runParams.dataSourceName
        addXADatasource(dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource() == null

        cleanup:
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone("mysql"))
    }

    @IgnoreIf({EnvPropertiesHelper.getVersion() == '6.0'})
    @Unroll
    def "RemoveXADataSource, PostgreSQL C289614"() {
        String testCaseId = "C289614"
        String jdbcDriverName = "postgresql"
        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'PostgresXADS',
        ]
        setup:
        String path = getPathToMain("postgresql", "org")
        createDir(getPath("postgresql", "org"))
        createDir(path)
        downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
        downloadArtifact(xml.postgresql, path+"/module.xml")
        addModuleXADatasource(jdbcDriverName, "org.postgresql.xa.PGXADataSource")
        def dataSourceName = runParams.dataSourceName
        addXADatasource(dataSourceName, jndiName.postgresql, jdbcDriverName, 'org.postgresql.xa.PGXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource() == null

        cleanup:
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone("postgresql"))
    }

    @Unroll
    def "RemoveXADataSource, H2 C290242"() {
        String testCaseId = "C290242"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'H2XADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(dataSourceName, jndiName.h2, jdbcDriverName, 'org.h2.jdbcx.JdbcDataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource() == null

        cleanup:
        reloadServer()
    }

    @Unroll
    def "RemoveXADataSource, not existing 'Configuration name' C289624"() {
        String testCaseId = "C289624"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : '',
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
    def "RemoveXADataSource, with 'Profile' C290245"() {
        String testCaseId = "C290245"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : 'profile_name',
                serverconfig     : defaultConfigName,
                dataSourceName   : 'H2XADS',
        ]
        setup:
        def dataSourceName = runParams.dataSourceName
        addXADatasource(dataSourceName, jndiName.h2, jdbcDriverName, 'org.h2.jdbcx.JdbcDataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        assert getListOfXADataSource() == null

        cleanup:
        reloadServer()
    }


    @Unroll
    def "RemoveXADataSource, not existing 'Data Source Name' C289625"() {
        String testCaseId = "C289625"
        String jdbcDriverName = "h2"
        def runParams = [
                profile          : '',
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

    def getListOfXADataSource() {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getListOfXADatasourceInStandalone()).result."xa-data-source"
        return result
    }
       
    def reloadServer(){
        try {
            // I used here try catch because "reload" command doesn't any json
            runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.reloadStandalone())
        }
        catch (Exception e){
                println e.getMessage()
            }
        def cond = true
        while(cond){
            try {
                sleep(3000)
                if (runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getStandaloneStatus()).result == 'running') {
                    cond = false
                }
            }
            catch (Exception e){
                println e.getMessage()
            }
        }
    }     

    void addJDBCMySQL(String jdbcDriverName){
        String path = getPathToMain("mysql", "com")
        createDir(getPath("mysql", "com"))
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        // if(!(EnvPropertiesHelper.getVersion() ==~ '6.[0,1,2,3]')) {
        addModuleXADatasource(jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")
        // }
    }

    void addJDBCPostgres(String jdbcDriverName){
        String path = getPathToMain("postgresql", "org")
        createDir(getPath("postgresql", "org"))
        createDir(path)
        downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
        downloadArtifact(xml.postgresql, path+"/module.xml")
        addModuleXADatasource(jdbcDriverName, "org.postgresql.xa.PGXADataSource")
    }

    void addModuleXADatasource(String driver, String DSclass){
        if (EnvPropertiesHelper.getVersion() in ['6.0','6.1','6.2','6.3']) {
            // https://issues.jboss.org/browse/JBPAPP6-944
            reloadServer()
            runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasourceStandalone(driver, DSclass))
        } 
        else {
            runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasourceStandalone(driver, DSclass))
        }
    }

    void addXADatasource(String name, String jndiName, String driverName, String xaDatasourceClass){
        runCliCommand(CliCommandsGeneratorHelper.addXADatasource(name, jndiName, driverName, xaDatasourceClass))
    }

}