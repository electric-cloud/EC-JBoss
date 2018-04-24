import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
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
            mysql: '"DatabaseName"=>"mysqlDB","ServerName"=>"localhost","Port"=>"3306"',
            postgresql: '"DatabaseName"=>"postgresdb","ServerName"=>"servername","Port=>5432"'
            // oracle: 'url=jdbc:oracle:oci8:@tc',
            // sqlserver: 'DatabaseName=mssqldb,ServerName=localhost,SelectMethod=cursor',
            // ibmdb2: 'DatabaseName=ibmdb2db,ServerName=localhost,PortNumber=446',
            // sybase: 'DatabaseName=mydatabase,ServerName=localhost,PortNumber=4100,NetworkProtocol=Tds',
            // mariadb: 'DatabaseName=mariadbdb,ServerName=localhost'
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
        String xaDataSourceName = dataSourceName.mysql+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
        cleanup: 
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName)
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, minimum parameters, verify xaDataSourceProperties (C289502-1)"() {
        String testCaseId = "C289502"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
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
        addJDBCMySQL(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "0",
                defaultPassword, defaultUserName)
        checkXADataSourceProperties(runParams.xaDataSourceProperties,  runParams.dataSourceName, defaultProfile)
        cleanup: 
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @IgnoreIf({EnvPropertiesHelper.getVersion() == '6.0'})
    @Unroll
    def "CreateorUpdateXADataSource, PostgreSQL, minimum parameters (C289503)"() {
        String testCaseId = "C289503"
        String jdbcDriverName = "postgresql"
        String xaDataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCPostgres(jdbcDriverName)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.postgresql, jdbcDriverName,
                "1", defaultPassword, defaultUserName)
        cleanup: 
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name, MySQL (C289512)"() {
        String testCaseId = "C289512"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289512" 
        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '1',
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
        checkCreateXADataSource(xaDataSourceName, defaultProfile, newJNDI, jdbcDriverName,
                "1", defaultPassword, defaultUserName)

        cleanup:
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name, MySQL, enabled=false (C289516)"() {
        String testCaseId = "C289516"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289516" 

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
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
        checkCreateXADataSource(xaDataSourceName, defaultProfile, newJNDI, jdbcDriverName,
                "0", defaultPassword, defaultUserName)

        cleanup:
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update Password, MySQL (C289513)"() {
        String testCaseId = "C289513"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289513" 
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentialsSecond',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentialsSecond',
                userName: 'root',
                password: 'root'
        ]
        setup:
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, newPassword)
        when:
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)

        then:
        def expectedStatus = ["success", "warning"]
        assert runProcedureJob1.getStatus() in expectedStatus
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new password."
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName,
                "0", newPassword, defaultUserName)

        cleanup:
        reloadServer('master')
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, defaultPassword)
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, update JNDI Name and Password, MySQL (C289514)"() {
        String testCaseId = "C289514"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = "MysqlXADSC289514"
        String newPassword = 'root' //change password
        String newUserName = 'root' //change userName

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentialsSecond',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
                profile                         : defaultProfile,
                serverconfig                    : defaultConfigName,
                xaDataSourceProperties          : xaDataSourceProperties.mysql,
        ]
        def credential = [
                credentialName: 'dataSourceConnectionCredentialsSecond',
                userName: 'root',
                password: 'root'
        ]
        setup:
        addJDBCMySQL(jdbcDriverName)
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        modifyCredential(projectName, "dataSourceConnectionCredentials", newUserName, newPassword)
        when:
        RunProcedureJob runProcedureJob1 = runProcedureUnderTest(runParams, credential)

        then:
        def expectedStatus = ["success", "warning"]
        assert runProcedureJob1.getStatus() in expectedStatus
        assert runProcedureJob1.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been updated successfully by new user name, password"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName,
                "0", newPassword, newUserName)

        cleanup:
        reloadServer('master')
        modifyCredential(projectName, "dataSourceConnectionCredentials", defaultUserName, defaultPassword)
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Enabled=false (C289510)"() {
        String testCaseId = "C289510"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.mysql+testCaseId

        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : '0',
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName,
                "0", defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, create data source on other profile with the same parameters (C289518)"() {
        String testCaseId = "C289518"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADSC289518"


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName, runParams.profile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSource(xaDataSourceName, 'full-ha', jndiName.mysql, jdbcDriverName, "1",
                defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
        // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain('full-ha', jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, update data source with the same parameters (C289545)"() {
        String testCaseId = "C289545"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADSC289545" //XADataSource from first test


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName, runParams.profile)
        RunProcedureJob runProcedureJob0 = runProcedureUnderTest(runParams, credential)
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' is up-to-date"
        checkCreateXADataSource(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "1",
                 defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--min-pool-size' (C289515)"() {
        String testCaseId = "C289515"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--min-pool-size=10',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "0",
                'min-pool-size', 10,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--max-pool-size' (C290066)"() {
        String testCaseId = "C290066"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--max-pool-size=25',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "1",
                'max-pool-size', 25,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--check-valid-connection-sql' (C290071)"() {
        String testCaseId = "C290071"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--check-valid-connection-sql="INSERT INTO jboss_table VALUES (34, \\\'qa34\\\');"',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : jndiName.mysql,
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, defaultProfile, jndiName.mysql, jdbcDriverName, "1",
                'check-valid-connection-sql', 'INSERT INTO jboss_table VALUES (34, \'qa34\');',  defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options '--pad-xid' (C290075)"() {
        String testCaseId = "C290075"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--pad-xid=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, defaultProfile, jndiName.postgresql, jdbcDriverName, "1",
                'pad-xid', true,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, Additional Options 'interleaving' (C290076)"() {
        String testCaseId = "C290076"
        String jdbcDriverName = "mysql"
        String xaDataSourceName = dataSourceName.postgresql+testCaseId

        def runParams = [
                additionalOptions               : '--interleaving=true',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        addJDBCMySQL(jdbcDriverName)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "XA data source '$xaDataSourceName' has been added successfully"
        checkCreateXADataSourceAdditionalOptions(xaDataSourceName, defaultProfile, jndiName.postgresql, jdbcDriverName, "1",
                'interleaving', true,  defaultPassword, defaultUserName)
        cleanup:
        reloadServer('master')
          // remove XA datasource
        removeXADatasource(dataSourceName.profile, xaDataSourceName) 
        reloadServer('master')
        runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInDomain(defaultProfile, jdbcDriverName))
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, without 'Configuration name' (C289527)"() {
        String testCaseId = "C289527"
        String jdbcDriverName = "mysql"
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : '',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
                enabled                         : defaultEnabledDataSource,
                jdbcDriverName                  : jdbcDriverName,
                jndiName                        : 'MysqlXADS',
                profile                         : defaultProfile,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        String xaDataSourceName ="MysqlXADS"+testCaseId


        def runParams = [
                additionalOptions               : '--some-wrong-option',
                dataSourceConnectionCredentials : 'dataSourceConnectionCredentials',
                dataSourceName                  : xaDataSourceName,
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
        if (EnvPropertiesHelper.getVersion() ==~ '6.0') {
            // https://issues.jboss.org/browse/JBPAPP6-944
            reloadServer('master')
            runCliCommandAnyResult(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
        } 
        else {
            runCliCommand(CliCommandsGeneratorHelper.addModuleXADatasource(profile, driver, DSclass))
        }
    }

    void checkCreateXADataSourceAdditionalOptions(String nameDatasource, String profile, String jndiNames, String jdbcDriverName,
        String enabled, String additionaOptionsParameter, def additionaOptionsValue, String password, String userName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
        if(additionaOptionsParameter != ''){
            assert result[additionaOptionsParameter] == additionaOptionsValue
        }
    }

    void checkXADataSourceProperties(def properties, def xaDataSourceName, def profile){
        properties.split(',').each {
            def property = it.split("=>")
            def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceProperties(property[0][1..-2], xaDataSourceName, profile)).result
            assert property[1][1..-2] == result.toString()
        }
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

    void checkCreateXADataSource(String nameDatasource, String profile, String jndiNames, String jdbcDriverName,
                                 String enabled, String password, String userName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
        assert result.'jndi-name' =~ jndiNames
        assert result.'driver-name' == jdbcDriverName
        assert result.'password' == password
        assert result.'user-name' == userName
        assert result.'enabled' == (enabled == "1" ? true : false)
    }


    void reloadServer(String host) {
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
        // runCliCommand(CliCommandsGeneratorHelper.stopServerCmd("server-one", hostName))
        // runCliCommand(CliCommandsGeneratorHelper.startServerCmd("server-one", hostName))
    }

    def removeXADatasource(def profile, def xaDataSourceName){
        if (EnvPropertiesHelper.getVersion() == "6.3") {
            runCliCommandAndGetJBossReply("/profile=$profile/subsystem=datasources/xa-data-source=$xaDataSourceName:disable()")
            reloadServer('master')
        }
        runCliCommandAnyResult(CliCommandsGeneratorHelper.removeXADatasource(profile, xaDataSourceName)) 
    }

}