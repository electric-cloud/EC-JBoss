import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
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
    String defaultEnabledDataSource = '0'
    @Shared
    String dataSourceConnectionCredentials = 'admin,changeme'
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
                        serverconfig                    : '',
                        dataSourceName                  : '',
                        jndiName                        : '',
                        jdbcDriverName                  : '',
                        xaDataSourceProperties          : '',
                        dataSourceConnectionCredentials : '',
                        enabled                         : '',
                        profile                         : '',
                        additionaOptions                : '',
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

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, minimum parameters (C289502)"() {
        String testCaseId = "C289502"
        String jdbcDriverName = "mysql"
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
    def "CreateorUpdateXADataSource, PostgreSQL, minimum parameters (C289503)"() {
        String testCaseId = "C289503"
        String jdbcDriverName = "postgresql"
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




    void checkUpdateXADataSource(String nameDatasource, String profile, String jndiNames, String dataSourceConnectionCredentials){
        checkCreateXADataSource(nameDatasource, profile, jndiNames, "", "", dataSourceConnectionCredentials, "", "")
    }

    void checkCreateXADataSource(String nameDatasource, String profile, String jndiNames, String xaDataSourceProperties, String jdbcDriverName,
                                          String dataSourceConnectionCredentials, String enabled, String additionaOptions) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getXADatasourceInfoDomain(nameDatasource, profile)).result
        assert result.'jndiName' =~ jndiNames
        assert result.'jdbcDriverName' == jdbcDriverName
        assert result.'xaDataSourceProperties' == xaDataSourceProperties
        assert result.'dataSourceConnectionCredentials' == dataSourceConnectionCredentials
        assert result.'enabled' == enabled
        assert result.'additionaOptions' == additionaOptions
    }

}