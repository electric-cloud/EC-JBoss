import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
@Stepwise
class CreateOrUpdateXADataSourceDomain extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateXADataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String defaultProfile = 'full'
    @Shared
    String defaultEnabledDataSource = '1'
    @Shared
    String dataSourceConnectionCredentials = "dataSourceConnectionCredentials"
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
            oracle: 'java:/XAOracleDS',
            sqlserver: 'java:/MSSQLXADS',
            ibmdb2: 'java:/DB2XADS',
            sybase: 'java:/SybaseXADS',
            mariadb: 'java:jboss/MariaDBXADS'
    ]

    @Shared
    def xml = [
            /**
             * Required
             */
            empty: '',
            mysql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/mysql/module.xml",
            postgresql: "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/XADatasources/postgresql/module.xml",
            oracle: 'java:/XAOracleDS',
            sqlserver: 'java:/MSSQLXADS',
            ibmdb2: 'java:/DB2XADS',
            sybase: 'java:/SybaseXADS',
            mariadb: 'java:jboss/MariaDBXADS'
    ]

    @Shared
    def xaDataSourceProperties = [
            /**
             * Required
             */
            empty: '',
            mysql: 'DatabaseName=mysqlDB,ServerName=localhost,Port=3306',
            postgresql: 'DatabaseName=postgresdb,ServerName=servername,Port=5432',
            oracle: 'url=jdbc:oracle:oci8:@tc',
            sqlserver: 'DatabaseName=mssqldb,ServerName=localhost,SelectMethod=cursor',
            ibmdb2: 'DatabaseName=ibmdb2db,ServerName=localhost,PortNumber=446',
            sybase: 'DatabaseName=mydatabase,ServerName=localhost,PortNumber=4100,NetworkProtocol=Tds',
            mariadb: 'DatabaseName=mariadbdb,ServerName=localhost'
    ]

    @Shared
    def dataSourceName = [
            /**
             * Required
             */
            empty: '',
            mysql: 'MysqlXADS',
            postgresql: 'PostgresXADS',
            oracle: 'XAOracleDS',
            sqlserver: 'MSSQLXADS',
            ibmdb2: 'DB2XADS',
            sybase: 'SybaseXADS',
            mariadb: 'MariaDBXADS'
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
                        additionalOptions               : '',
                        dataSourceConnectionCredentials : '',
                        dataSourceName                  : '',
                        enabled                         : '',
                        jdbcDriverName                  : '',
                        jndiName                        : '',
                        profile                         : '',
                        serverconfig                    : '',
                        xaDataSourceProperties          : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createCredential(projectName, "dataSourceConnectionCredentials")
        attachCredential(projectName, "dataSourceConnectionCredentials", procName)

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
//        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters, def credential) {
        return runProcedureDsl(projectName, procName, parameters, credential)
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, minimum parameters (C289502)"() {
        String testCaseId = "C289502"
        String jdbcDriverName = "mysql"
        String dataSourceName = dataSourceName.mysql+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: 'admin',
                password: 'changeme'
        ]

        setup:
        String path = "/opt/jboss/modules/system/layers/base/com/mysql/main"
        createDir("/opt/jboss/modules/system/layers/base/com/mysql")
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        addModuleXADatasource(defaultProfile, jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSource(dataSourceName, defaultProfile, jndiName.mysql,
                xaDataSourceProperties.mysql, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
    }

       @Unroll
       def "CreateorUpdateXADataSource, PostgreSQL, minimum parameters (C289503)"() {
           String testCaseId = "C289503"
           String jdbcDriverName = "postgresql"
           String dataSourceName = dataSourceName.postgresql+testCaseId

           def runParams = [
                   additionalOptions               : '',
                   dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                   dataSourceName                  : dataSourceName,
                   enabled                         : defaultEnabledDataSource,
                   jdbcDriverName                  : jdbcDriverName,
                   jndiName                        : jndiName.postgresql,
                   profile                         : defaultProfile,
                   serverconfig                    : defaultConfigName,
                   xaDataSourceProperties          : xaDataSourceProperties.postgresql,
           ]
           def credential = [
                   credentialName: 'dataSourceConnectionCredentials',
                   userName: 'admin',
                   password: 'changeme'
           ]

           setup:
           String path = "/opt/jboss/modules/system/layers/base/org/postgresql/main"
           createDir("/opt/jboss/modules/system/layers/base/org/postgresql")
           createDir(path)
           downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
           downloadArtifact(xml.postgresql, path+"/module.xml")
           addModuleXADatasource(defaultProfile, jdbcDriverName, "org.postgresql.xa.PGXADataSource")

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
           checkCreateXADataSource(dataSourceName, defaultProfile, jndiName.postgresql,
                   xaDataSourceProperties.postgresql, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, minimum parameters (C289553)"() {
        String testCaseId = "C289553"
        String jdbcDriverName = "mysql"
        String dataSourceName = dataSourceName.mysql+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : '0',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'default',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: 'admin',
                password: 'changeme'
        ]
        setup:
        addModuleXADatasource('default', jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSource(dataSourceName, 'default', jndiName.mysql,
                xaDataSourceProperties.mysql, jdbcDriverName, dataSourceConnectionCredentials, "0", "")
    }


  /*     @Unroll
       def "CreateorUpdateXADataSource, Oracle, minimum parameters (C289504)"() {
           String testCaseId = "C289504"
           String jdbcDriverName = "oracle"
           String dataSourceName = dataSourceName.jdbcDriverName+testCaseId

           def runParams = [
                   serverconfig                    : defaultConfigName,
                   dataSourceName                  : dataSourceName,
                   jndiName                        : jndiName.jdbcDriverName,
                   jdbcDriverName                  : jdbcDriverName,
                   xaDataSourceProperties          : xaDataSourceProperties.jdbcDriverName,
                   dataSourceConnectionCredentials : dataSourceConnectionCredentials, //need re-write
                   enabled                         : '1',
                   profile                         : defaultProfile,
                   additionaOptions                : ''
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "TBD"
           checkCreateXADataSource(dataSourceName.jdbcDriverName, defaultProfile, jndiName.jdbcDriverName,
                   xaDataSourceProperties.jdbcDriverName, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }

       @Unroll
       def "CreateorUpdateXADataSource, Microsoft SQL, minimum parameters (C289505)"() {
           String testCaseId = "C289505"
           String jdbcDriverName = "sqlserver"
           String dataSourceName = dataSourceName.jdbcDriverName+testCaseId

           def runParams = [
                   serverconfig                    : defaultConfigName,
                   dataSourceName                  : dataSourceName,
                   jndiName                        : jndiName.jdbcDriverName,
                   jdbcDriverName                  : jdbcDriverName,
                   xaDataSourceProperties          : xaDataSourceProperties.jdbcDriverName,
                   dataSourceConnectionCredentials : dataSourceConnectionCredentials, //need re-write
                   enabled                         : '1',
                   profile                         : defaultProfile,
                   additionaOptions                : ''
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "TBD"
           checkCreateXADataSource(dataSourceName.jdbcDriverName, defaultProfile, jndiName.jdbcDriverName,
                   xaDataSourceProperties.jdbcDriverName, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }

       @Unroll
       def "CreateorUpdateXADataSource, IBM DB2, minimum parameters (C289507)"() {
           String testCaseId = "C289507"
           String jdbcDriverName = "ibmdb2"
           String dataSourceName = dataSourceName.jdbcDriverName+testCaseId

           def runParams = [
                   serverconfig                    : defaultConfigName,
                   dataSourceName                  : dataSourceName,
                   jndiName                        : jndiName.jdbcDriverName,
                   jdbcDriverName                  : jdbcDriverName,
                   xaDataSourceProperties          : xaDataSourceProperties.jdbcDriverName,
                   dataSourceConnectionCredentials : dataSourceConnectionCredentials, //need re-write
                   enabled                         : '1',
                   profile                         : defaultProfile,
                   additionaOptions                : ''
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "TBD"
           checkCreateXADataSource(dataSourceName.jdbcDriverName, defaultProfile, jndiName.jdbcDriverName,
                   xaDataSourceProperties.jdbcDriverName, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }

       @Unroll
       def "CreateorUpdateXADataSource, Sybase, minimum parameters (C289508)"() {
           String testCaseId = "C289508"
           String jdbcDriverName = "sybase"
           String dataSourceName = dataSourceName.jdbcDriverName+testCaseId

           def runParams = [
                   serverconfig                    : defaultConfigName,
                   dataSourceName                  : dataSourceName,
                   jndiName                        : jndiName.jdbcDriverName,
                   jdbcDriverName                  : jdbcDriverName,
                   xaDataSourceProperties          : xaDataSourceProperties.jdbcDriverName,
                   dataSourceConnectionCredentials : dataSourceConnectionCredentials, //need re-write
                   enabled                         : '1',
                   profile                         : defaultProfile,
                   additionaOptions                : ''
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "TBD"
           checkCreateXADataSource(dataSourceName.jdbcDriverName, defaultProfile, jndiName.jdbcDriverName,
                   xaDataSourceProperties.jdbcDriverName, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }


       @Unroll
       def "CreateorUpdateXADataSource, Sybase, minimum parameters (C289509)"() {
           String testCaseId = "C289509"
           String jdbcDriverName = "mariadb"
           String dataSourceName = dataSourceName.jdbcDriverName+testCaseId

           def runParams = [
                   serverconfig                    : defaultConfigName,
                   dataSourceName                  : dataSourceName,
                   jndiName                        : jndiName.jdbcDriverName,
                   jdbcDriverName                  : jdbcDriverName,
                   xaDataSourceProperties          : xaDataSourceProperties.jdbcDriverName,
                   dataSourceConnectionCredentials : dataSourceConnectionCredentials, //need re-write
                   enabled                         : '1',
                   profile                         : defaultProfile,
                   additionaOptions                : ''
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "TBD"
           checkCreateXADataSource(dataSourceName.jdbcDriverName, defaultProfile, jndiName.jdbcDriverName,
                   xaDataSourceProperties.jdbcDriverName, jdbcDriverName, dataSourceConnectionCredentials, "1", "")
       }
*/

/*    void createDir(){
        //for mysql
        createDir("/opt/jboss/modules/system/layers/base/com/mysql")
        createDir("/opt/jboss/modules/system/layers/base/com/mysql/main")
    }

    void downloadArtifact(){
        //for mysql
        downloadArtifact(link, "/opt/jboss/modules/system/layers/base/com/mysql/main/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml, "/opt/jboss/modules/system/layers/base/com/mysql/main/module.xml")
    }*/

    void addModuleXADatasource(String profile, String driver, String DSclass){
        runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
    }

       void checkUpdateXADataSource(String nameDatasource, String profile, String jndiNames, String dataSourceConnectionCredentials){
           checkCreateXADataSource(nameDatasource, profile, jndiNames, "", "", dataSourceConnectionCredentials, "", "")
       }

       void checkCreateXADataSource(String nameDatasource, String profile, String jndiNames, String xaDataSourceProperties, String jdbcDriverName,
                                             String dataSourceConnectionCredentials, String enabled, String additionaOptions) {
           def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
           assert result.'jndi-name' =~ jndiNames
           assert result.'driver-name' == jdbcDriverName
           assert result.'password' == 'changeme'
           assert result.'user-name' == 'admin'
           assert result.'enabled' == (enabled == "1" ? true : false)
//           assert result.'additionaOptions' == additionaOptions
       }


    void shutdownHost(String hostName) {
        runCliCommand(CliCommandsGeneratorHelper.reloadHostDomain(hostName))
    }

}