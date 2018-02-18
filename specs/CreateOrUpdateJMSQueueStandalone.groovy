import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*


@IgnoreIf({ env.JBOSS_MODE == 'domain' })
class CreateOrUpdateJMSQueue extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateJMSQueue'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String defaultDurable = 'false'
    @Shared
    String defaultJndiNames = 'queue/test,java:jboss/exported/jms/queue/test'
    @Shared
    String defaultMessageSelector = 'undefined'


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
                        additionalOptions    : '',
                        durable              : '',
                        jndiNames            : '',
                        messageSelector      : '',
                        profile              : '',
                        queueName            : '',
                        serverconfig         : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
//        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Unroll
    def "Create JMS Queue with minimum parameters (C278344)"() {
        String testCaseId = "C278344"

        def runParams = [
                additionalOptions    : '',
                durable              : '',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been added successfully"

        String queueName = "testQueue-$testCaseId"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, defaultJndiNames)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }

    @Unroll
    def "Create JMS Queue with 'Durable=true' (C278349)"() {
        String testCaseId = "C278349"

        def runParams = [
                additionalOptions    : '',
                durable              : '1',  //true
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been added successfully"

        String queueName = "testQueue-$testCaseId"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, defaultJndiNames)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }

    void checkCreateOrUpdateJMSQueue(String queueName, String durable, String messageSelector, String jndiNames) {
        RunProcedureJob runProcedureJob = runCliCommandAndGetJbossReply(CliCommandsGeneratorHelper.getJMSQueueInfo(queueName)).result
        assert runProcedureJob.getLogs()  =~ ".*$queueName.*"
    }

    void removeJMSQueue(String queueName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSQueue(queueName))
    }

    void reloadStandalone() {
        runCliCommand(CliCommandsGeneratorHelper.reloadStandalone())
    }

    }