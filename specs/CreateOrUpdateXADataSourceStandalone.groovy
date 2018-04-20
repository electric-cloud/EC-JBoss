import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class CreateOrUpdateXADataSourceStandalone extends PluginTestHelper {

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
        createCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, defaultPassword)
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
    def "CreateorUpdateXADataSource, MySQL, minimum parameters (C289546)"() {
        String testCaseId = "C289546"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
        cleanup: 
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @IgnoreIf({EnvPropertiesHelper.getVersion() == '6.0'})
    @Unroll
    def "CreateorUpdateXADataSource, PostgreSQL, minimum parameters (C289547)"() {
        String testCaseId = "C289547"
        String jdbcDriverName = "postgresql"
        String xaDataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.postgresql,
                profile                         : '',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.postgresql,
            ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
            ]
        setup:
        addJDBCPostgres(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, jndiName.postgresql, jdbcDriverName,
                "1", defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
       }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name, MySQL (C289554)"() {
        String testCaseId = "C289554"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289554" 
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : '',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: 'admin',
                password: 'changeme'
        ]
        // create xa datasource
        setup:
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        when:
        //change JNDI 
        def newJNDI = jndiName.mysql+"_new" 
        runParams.jndiName = newJNDI
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob1.getStatus() == "warning"
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new jndi name."
        checkCreateXADataSource(xaDataSourceName, newJNDI, jdbcDriverName,
                "1", defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, enabled=false (C289553)"() {
        String testCaseId = "C289553"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289553" 
        String newJNDI = jndiName.mysql+testCaseId 
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, newJNDI, jdbcDriverName,
                "0", defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update Password, MySQL (C289555)"() {
        String testCaseId = "C289555"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289555"
        String JNDI = jndiName.mysql+"C289555"
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : JNDI,
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
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, newPassword)
        when:
        println("QA $runParams")
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)
        then:
        def expectedStatus = (EnvPropertiesHelper.getVersion() == '6.0') ? "success" : "warning"
        assert runProcedureJob1.getStatus() == expectedStatus
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new password."
        checkCreateXADataSource(xaDataSourceName, JNDI, jdbcDriverName,
                "0", newPassword, defaultUserName)

        cleanup:
        reloadServer()
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, defaultPassword)
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name and Password, MySQL (C289556)"() {
        String testCaseId = "C289556"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289556"
        String JNDI = jndiName.mysql+"C289556"
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

                def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '1',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : JNDI,
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
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        modifyCredential(projectName, "dataSourceConnectionCredentials", newUserName, newPassword)
        when:
        println("QA $runParams")
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)
        then:
        def expectedStatus = (EnvPropertiesHelper.getVersion() == '6.0') ? "success" : "warning"
        assert runProcedureJob1.getStatus() == expectedStatus
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new user name, password."
        checkCreateXADataSource(xaDataSourceName, JNDI, jdbcDriverName,
                "1", newPassword, newUserName)

        cleanup:
        reloadServer()
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, defaultPassword)
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll 
    def "CreateorUpdateXADataSource, MySQL, update JNDI Name Enabled=false (C289558)"() {
        String testCaseId = "C289558"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        String jndi_new = jndiName.mysql+"_new"
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
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
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        when:
        runParams.jndiName = jndi_new
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob1.getStatus() == "warning"
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new jndi name"
        checkCreateXADataSource(xaDataSourceName, jndi_new, jdbcDriverName,
                "0", defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, with ignored 'Profile'  (C289559)"() {
        String testCaseId = "C289559"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : 'full',
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentials',
                userName: defaultUserName,
                password: defaultPassword
        ]
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
        cleanup: 
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, update data source with the same parameters (C289560)"() {
        String testCaseId = "C289560"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
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
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        when:
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob1.getStatus() == "success"
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' is up-to-date"
        checkCreateXADataSource(xaDataSourceName, jndiName.mysql, jdbcDriverName,
                "0", defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--min-pool-size' (C289557)"() {
        String testCaseId = "C289557"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--min-pool-size=10',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                'min-pool-size', 10,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--max-pool-size' (C289557)"() {
        String testCaseId = "C289557"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId
        def runParams = [
                additionalOptions               : '--max-pool-size=25',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                'max-pool-size', 25,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options --check-valid-connection-sql' (C289557)"() {
        String testCaseId = "C289557"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId
        def runParams = [
                additionalOptions               : '--check-valid-connection-sql="INSERT INTO jboss_table VALUES (34, \\\'qa34\\\');"',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                'check-valid-connection-sql', 'INSERT INTO jboss_table VALUES (34, \'qa34\');',  defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--pad-xid' (C289557)"() {
        String testCaseId = "C289557"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId

        def runParams = [
                additionalOptions               : '--pad-xid=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                'pad-xid', true,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options 'interleaving' (C289557)"() {
        String testCaseId = "C289557"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId

        def runParams = [
                additionalOptions               : '--interleaving=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
                'interleaving', true,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Configuration name' ( C289561)"() {
        String testCaseId = " C289561"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : '',
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
    def "CreateorUpdateXADataSource, MySQL, without 'Data Source Name'  ( C289562)"() {
        String testCaseId = " C289562"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : '',
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'dataSourceName' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'JNDI Name'  ( C289563)"() {
        String testCaseId = " C289563"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : '',
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jndiName' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'JDBC Driver Name'  ( C289564)"() {
        String testCaseId = " C289564"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : '',
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jdbcDriverName' is not provided"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'xaDataSourceProperties'  ( C289566)"() {
        String testCaseId = " C289566"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : '',
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
    def "CreateorUpdateXADataSource, MySQL, without 'Datasource Connection Credentials'  ( C289567)"() {
        String testCaseId = " C289567"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : '',
                dataSourceName                  : xaDataSourceName,
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'dataSourceConnectionCredentials' is not provided"
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Configuration name'  ( C289569)"() {
        String testCaseId = " C289569"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : '',
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
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Data Source Name' ( C289570)"() {
        String testCaseId = " C289570"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : 'Mysql XA@DS',
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
        assert runProcedureJob.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
    }


    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'JNDI Name' ( C289571)"() {
        String testCaseId = " C289571"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : 'MysqlXADS',
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
        assert runProcedureJob.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value  'JDBC Driver Name' ( C289572)"() {
        String testCaseId = " C289572"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : 'wrong_driver',
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
        assert runProcedureJob.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Additional Options' ( C289576)"() {
        String testCaseId = " C289576"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '--some-wrong-option',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized arguments: \\[--some-wrong-option\\]"
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, incorrect value 'Additional Options' ( C289577)"() {
        String testCaseId = " C289577"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        when:
        runParams.jndiName = 'MysqlXADS'
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob1.getStatus() == "error"
        assert runProcedureJob1.getUpperStepSummary() =~ "The batch failed with the following error \\(you are remaining in the batch editing mode to have a chance to correct the error\\)"
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, empty value 'Additional Options' (C289578)"() {
        String testCaseId = " C289578"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        setup:
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        when:
        runParams.jndiName = ''
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob1.getStatus() == "error"
        assert runProcedureJob1.getUpperStepSummary() =~ "Required parameter 'jndiName' is not provided"
        cleanup:
        reloadServer()
        // remove XA datasource
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(runParams.dataSourceName)) 
        reloadServer()
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))
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
        if (EnvPropertiesHelper.getVersion() ==~ '6.0') {
            // https://issues.jboss.org/browse/JBPAPP6-944
            reloadServer()
            runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasourceStandalone(driver, DSclass))
        } 
        else {
            runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasourceStandalone(driver, DSclass))
        }
    }

    void checkCreateXADataSourceAdditionalOptions(String nameDatasource, String jndiNames, String jdbcDriverName,
                                             String enabled, String additionaOptionsParameter, def additionaOptionsValue, String password, String userName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoStandalone(nameDatasource)).result
        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
        if(additionaOptionsParameter != ''){
            assert result[additionaOptionsParameter] == additionaOptionsValue
           }

       }


    void checkCreateXADataSource(String nameDatasource, String jndiNames, String jdbcDriverName,
                                 String enabled, String password, String userName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoStandalone(nameDatasource)).result
        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
    }


    void reloadServer() {
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

}