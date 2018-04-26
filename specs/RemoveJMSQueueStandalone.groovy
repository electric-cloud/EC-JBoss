import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class RemoveJMSQueueStandalone extends PluginTestHelper {

    @Shared
    String procName = 'RemoveJMSQueue'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultJndiNames = 'queue/test,java:jboss/exported/jms/queue/test'


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
                        profile          : '',
                        queueName        : '',
                        serverconfig     : '',
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
    def "Remove JMS Queue without 'Queue Name' (C278427)"() {
        String testCaseId = "C278427"

        def runParams = [
                profile          : '',
                queueName        : '',
                serverconfig     : defaultConfigName
        ]

        setup:
        String queueName = "testQueue-$testCaseId"
        addJMSQueueDefaultStandalone(queueName, defaultJndiNames)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'queueName' is not provided"

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)

    }

    @Unroll
    def "Remove JMS Queue, non existing Queue Name (C278428)"() {
        String testCaseId = "C278428"

        def runParams = [
                profile          : '',
                queueName        : "testQueue-$testCaseId",
                serverconfig     : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' not found"
    }


    void addJMSQueueDefaultStandalone(String queueName, String jndiName) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueueDefaultStandalone(queueName, jndiName))
    }

    void removeJMSQueue(String queueName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSQueueStandalone(queueName))
    }

}