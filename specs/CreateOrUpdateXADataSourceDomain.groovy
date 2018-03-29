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
    def "Create JMS Queue with minimum parameters (C278706)"() {
        String testCaseId = "C278706"

    }


}