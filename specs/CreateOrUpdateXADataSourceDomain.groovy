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

        def runParams = [
                serverconfig                    : defaultConfigName,
                dataSourceName                  : "MysqlXADS",
                jndiName                        : "java:/MysqlXADS",
                jdbcDriverName                  : "mysql",
                xaDataSourceProperties          : '',
                dataSourceConnectionCredentials : '',
                enabled                         : '',
                profile                         : '',
                additionaOptions                : ''
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
    }


}