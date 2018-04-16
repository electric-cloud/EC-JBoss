import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

class DeleteConfiguration extends PluginTestHelper {

    @Shared
    String procName = 'DeleteConfiguration'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        config            : '',
                ]
        ]

        createHelperProject(resName, '')
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Unroll
    def "DeleteConfiguration (C289709)"() {
        String testCaseId = "C289709"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : config_name
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
    }

    @Unroll
    def "DeleteConfiguration, without 'Configuration name' (C289713)"() {
        String testCaseId = "C289713"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : ''
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "DeleteConfiguration, with not existing 'Configuration name' (C289714)"() {
        String testCaseId = "C289714"
        String config_name = "jboss_conf_not_existing"+testCaseId

        def runParams = [
                config              : config_name
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

}