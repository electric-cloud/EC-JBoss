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
            mariadb: 'java:jboss/MariaDBXADS'
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
        String testCaseId = "C278471"
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
        // if(!(EnvPropertiesHelper.getVersion() ==~ '6.[0,1,2,3]')) {
        //     addModuleXADatasource(defaultProfile, jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")
        // }
        def dataSourceName = runParams.dataSourceName
        addXADatasource(defaultProfile, dataSourceName, jndiName.mysql, jdbcDriverName, 'com.mysql.jdbc.jdbc2.optional.MysqlXADataSource')
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been removed successfully"
        checkCreateXADataSource(dataSourceName, defaultProfile)

    }

    void checkCreateXADataSource(String nameDatasource, String profile) {
        try {
            print("QA ED")
            def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)) 
        }
        catch(Exception e) {
            print("QA ED ================= $e")
        }
        
    }

    void addXADatasource(String profile, String name, String jndiName, String driverName, String xaDatasourceClass){
        runCliCommand(CliCommandsGeneratorHelper.addXADatasource(profile, name, jndiName, driverName, xaDatasourceClass))
    }
    
    void addModuleXADatasource(String profile, String driver, String DSclass){
        runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
    }

}