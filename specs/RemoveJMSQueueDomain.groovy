import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class RemoveJMSQueueDomain extends PluginTestHelper {

    @Shared
    String procName = 'RemoveJMSQueue'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultJndiNames = 'queue/test,java:jboss/exported/jms/queue/test'
    @Shared
    String defaultProfile = 'full'

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
    def "Remove JMS Queue without 'Queue Name' (C278433)"() {
        String testCaseId = "C278433"

        def runParams = [
                profile          : defaultProfile,
                queueName        : '',
                serverconfig     : defaultConfigName
        ]

        setup:
        String queueName = "testQueue-$testCaseId"
        addJMSQueueDefaultDomain(queueName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'queueName' is not provided"

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Remove JMS Queue, non existing Queue Name (C278434)"() {
        String testCaseId = "C278434"

        def runParams = [
                profile          : defaultProfile,
                queueName        : "testQueue-$testCaseId",
                serverconfig     : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' not found"
    }

    @Unroll
    def "Remove JMS Queue, without 'Profile' (C278430)"() {
        String testCaseId = "C278430"

        def runParams = [
                profile          : '',
                queueName        : "testQueue-$testCaseId",
                serverconfig     : defaultConfigName
        ]

        setup:
        String queueName = "testQueue-$testCaseId"
        addJMSQueueDefaultDomain(queueName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    void addJMSQueueDefaultDomain(String queueName, String jndiName, String domain) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueueDefaultDomain(queueName, jndiName, domain))
    }

    void removeJMSQueue(String queueName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSQueueDomain(queueName, profile))
    }

}