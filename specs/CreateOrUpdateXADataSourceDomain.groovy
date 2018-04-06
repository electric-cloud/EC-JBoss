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
    String defaultPassword = 'changeme'
    @Shared
    String defaultUserName = "admin"
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
        createCredential(projectName, "dataSourceConnectionCredentials",defaultUserName, defaultPassword)
        attachCredential(projectName, "dataSourceConnectionCredentials", procName)

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters, def credential) {
        return runProcedureDsl(projectName, procName, parameters, credential)
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
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        String path = getPathToMain("mysql", "com")
        createDir(getPath("mysql", "com"))
        createDir(path)
        downloadArtifact(link.mysql, path+"/mysql-connector-java-5.1.36.jar")
        downloadArtifact(xml.mysql, path+"/module.xml")
        if(EnvPropertiesHelper.getVersion() != "6.0") {
            addModuleXADatasource(defaultProfile, jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
        }

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSource(dataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
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
                   userName: defaultUserName,
                   password: defaultPassword
           ]

           setup:
           String path = getPathToMain("postgresq", "org")
           createDir(getPath("postgresq", "org"))
           createDir(path)
           downloadArtifact(link.postgresql, path+"/postgresql-42.2.2.jar")
           downloadArtifact(xml.postgresql, path+"/module.xml")
           addModuleXADatasource(defaultProfile, jdbcDriverName, "org.postgresql.xa.PGXADataSource")

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

           then:
           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
           checkCreateXADataSource(dataSourceName, defaultProfile, jndiName.postgresql, jdbcDriverName,
                   "1", defaultPassword, defaultUserName)
       }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name, MySQL (C289512)"() {
        String testCaseId = "C289512"
        String jdbcDriverName = "mysql"
        String dataSourceName = "MysqlXADSC289502" //XADataSource from first test
        String newJNDI = jndiName.mysql+testCaseId //change JNDI (add case number)

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : newJNDI,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been updated successfully by new jndi name."
        checkCreateXADataSource(dataSourceName, defaultProfile, newJNDI, jdbcDriverName,
                "1", defaultPassword, defaultUserName)

        cleanup:
        reloadServer("master")
    }

    @Unroll
    @Ignore // need run after fix JIRA  ECPAPPSERVERJBOSS-659
    def "CreateorUpdateXADataSource, update JNDI Name, MySQL, enabled=false (C289516)"() {
        String testCaseId = "C289516"
        String jdbcDriverName = "mysql"
        String dataSourceName = "MysqlXADSC289502" //XADataSource from first test
        String newJNDI = jndiName.mysql+testCaseId //change JNDI (add case number)

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : '0',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : newJNDI,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been updated successfully by new jndi name."
        checkCreateXADataSource(dataSourceName, defaultProfile, newJNDI, jdbcDriverName,
                "0", defaultPassword, defaultUserName)

        cleanup:
        reloadServer("master")
    }


    @Unroll
    @Ignore //cant change cred
    def "CreateorUpdateXADataSource, update Password, MySQL (C289513)"() {
        String testCaseId = "C289513"
        String jdbcDriverName = "mysql"
        String dataSourceName = "MysqlXADSC289502" //XADataSource from first test
        String JNDI = jndiName.mysql+"C289554" //new JNDI from test C289554
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentialsSecond',
                dataSourceName                  : dataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : JNDI,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentialsSecond',
                userName: 'root',
                password: 'root'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been updated successfully by new password."
        checkCreateXADataSource(dataSourceName, defaultProfile, JNDI, jdbcDriverName,
                "1", newPassword, newUserName)

        cleanup:
        reloadServer("master")
    }


    @Unroll
    @Ignore //cant change cred
    def "CreateorUpdateXADataSource, update JNDI Name and Password, MySQL (C289514)"() {
        String testCaseId = "C289514"
        String jdbcDriverName = "mysql"
        String dataSourceName = "MysqlXADSC289502" //XADataSource from first test
        String newJNDI = jndiName.mysql+testCaseId //new JNDI
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentialsSecond',
                dataSourceName                  : dataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : newJNDI,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentialsSecond',
                userName: 'root',
                password: 'root'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been updated successfully by new password."
        checkCreateXADataSource(dataSourceName, defaultProfile, newJNDI, jdbcDriverName,
                "1", newPassword, newUserName)

        cleanup:
        reloadServer("master")
    }

    @Unroll
    @Ignore // need run after fix JIRA  ECPAPPSERVERJBOSS-659
    def "CreateorUpdateXADataSource, MySQL, Enabled=false (C289510)"() {
        String testCaseId = "C289510"
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
        checkCreateXADataSource(dataSourceName, 'default', jndiName.mysql, jdbcDriverName,
                "0", defaultPassword, defaultUserName)
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, create data source on other profile with the same parameters (C289518)"() {
        String testCaseId = "C289518"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADSC289502" //XADataSource from first test


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'full-ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('full-ha', jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSource(dataSourceName, 'full-ha', jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, update data source with the same parameters (C289545)"() {
        String testCaseId = "C289545"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADSC289502" //XADataSource from first test


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'full-ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' is up-to-date"
        checkCreateXADataSource(dataSourceName, 'full-ha', jndiName.mysql, jdbcDriverName, "1",
                 defaultPassword, defaultUserName)
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--min-pool-size' (C289515)"() {
        String testCaseId = "C289515"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--min-pool-size=10',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'default',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('default', jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(dataSourceName, 'default', jndiName.mysql, jdbcDriverName, "1",
                'min-pool-size', 10,  defaultPassword, defaultUserName)
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--max-pool-size' (C290066)"() {
        String testCaseId = "C290066"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--max-pool-size=25',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('ha', jdbcDriverName, "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(dataSourceName, 'ha', jndiName.mysql, jdbcDriverName, "1",
                'max-pool-size', 25,  defaultPassword, defaultUserName)
    }


    @Unroll
    @Ignore //need run after fix ECPAPPSERVERJBOSS-660
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--check-valid-connection-sql' (C290071)"() {
        String testCaseId = "C290071"
        String jdbcDriverName = "postgresql"
        String dataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--check-valid-connection-sql="Select 1"',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.postgresql,
                profile                         : 'default',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.postgresql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('default', jdbcDriverName, "org.postgresql.xa.PGXADataSource")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(dataSourceName, 'default', jndiName.mysql, jdbcDriverName, "1",
                'check-valid-connection-sql', '"Select 1"',  defaultPassword, defaultUserName)
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--pad-xid' (C290075)"() {
        String testCaseId = "C290075"
        String jdbcDriverName = "postgresql"
        String dataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--pad-xid=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.postgresql,
                profile                         : 'full-ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.postgresql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('full-ha', jdbcDriverName, "org.postgresql.xa.PGXADataSource")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(dataSourceName, 'full-ha', jndiName.postgresql, jdbcDriverName, "1",
                '--pad-xid', true,  defaultPassword, defaultUserName)
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options 'interleaving' (C290076)"() {
        String testCaseId = "C290076"
        String jdbcDriverName = "postgresql"
        String dataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--interleaving=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.postgresql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.postgresql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]

        setup:
        addModuleXADatasource('ha', jdbcDriverName, "org.postgresql.xa.PGXADataSource")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$dataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(dataSourceName, 'ha', jndiName.postgresql, jdbcDriverName, "1",
                '--interleaving', true,  defaultPassword, defaultUserName)
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Configuration name' (C289527)"() {
        String testCaseId = "C289527"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : '',
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Data Source Name' (C289528)"() {
        String testCaseId = "C289528"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : '',
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'dataSourceName' is not provided"
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'JNDI Name' (C289529)"() {
        String testCaseId = "C289529"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : '',
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jndiName' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'JDBC Driver Name' (C289530)"() {
        String testCaseId = "C289530"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : '',
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jdbcDriverName' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'XA Data Source Properties' (C289532)"() {
        String testCaseId = "C289532"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : '',
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'xaDataSourceProperties' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Datasource Connection Credentials' (C289533)"() {
        String testCaseId = "C289533"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : '',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'dataSourceConnectionCredentials' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Profile' (C289534)"() {
        String testCaseId = "C289534"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : '',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Configuration name' (C289535)"() {
        String testCaseId = "C289535"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : 'jboss_conf_not_existing',
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Configuration jboss_conf_not_existing doesn't exist."
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Data Source Name' (C289536)"() {
        String testCaseId = "C289536"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : 'Mysql XA@DS',
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'JNDI Name' (C289537)"() {
        String testCaseId = "C289536"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : 'MysqlXADS',
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Profile' (C289541)"() {
        String testCaseId = "C289541"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'full_not_existing',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Management resource.*full_not_existing.*not found"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Additional Options' (C289542)"() {
        String testCaseId = "C289542"
        String jdbcDriverName = "mysql"
        String dataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--some-wrong-option',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : dataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'ha',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized arguments: \\[--some-wrong-option\\]"
    }


    void addModuleXADatasource(String profile, String driver, String DSclass){
        runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
    }

       void checkCreateXADataSourceAdditionalOptions(String nameDatasource, String profile, String jndiNames, String jdbcDriverName,
                                             String enabled, String additionaOptionsParameter, def additionaOptionsValue, String password, String userName) {
           def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
           assert result.'jndi-name' =~ jndiNames
           assert result.'driver-name' == jdbcDriverName
           assert result.'password' == password
           assert result.'user-name' == userName
           assert result.'enabled' == (enabled == "1" ? true : false)
           if(additionaOptionsParameter == 'min-pool-size'){
               assert result.'min-pool-size' == additionaOptionsValue
           }

       }


    void checkCreateXADataSource(String nameDatasource, String profile, String jndiNames, String jdbcDriverName,
                                 String enabled, String password, String userName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
    }


    void reloadServer(String hostName) {
        runCliCommand(CliCommandsGeneratorHelper.stopServerCmd("server-one", hostName))
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd("server-one", hostName))
    }

}