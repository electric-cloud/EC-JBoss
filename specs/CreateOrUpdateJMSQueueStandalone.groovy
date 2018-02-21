import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*


@IgnoreIf({ env.JBOSS_MODE == 'domain' })
class CreateOrUpdateJMSQueueStandalone extends PluginTestHelper {

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
    String defaultMessageSelector = ''


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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Create JMS Queue with message selector (C278353)"() {
        String testCaseId = "C278353"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "FILTER_EXPRESSION", jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Create JMS Queue, --durable=true in additional option (C278367)"() {
        String testCaseId = "C278367"

        def runParams = [
                additionalOptions    : '--durable=true',
                durable              : '0',
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Create JMS Queue with all completed field (C278376)"() {
        String testCaseId = "C278376"

        def runParams = [
                additionalOptions    : '',
                durable              : '1',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, "true", "FILTER_EXPRESSION", jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Create JMS Queue, ignored profile (C278351)"() {
        String testCaseId = "C278351"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : 'non-existing-profile',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been added successfully"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Update JMS Queue, change 'JNDI Names' (C278362)"() { //need manual check with app
        String testCaseId = "C278362"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultStandalone(runParams.queueName, "queue/test, java:jboss/exported/jms/queue/test2") //wrong jndi name

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been updated successfully by new jndi names"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }

    @Unroll
    def "Update JMS Queue, ignored change 'Durable' (C278363)"() {
        String testCaseId = "C278363"

        def runParams = [
                additionalOptions    : '',
                durable              : '1', //durable=true
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultStandalone(runParams.queueName, defaultJndiNames) //durable=false

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }

    @Unroll
    def "Update JMS Queue, ignored change 'Message Selector' (C278364)"() {
        String testCaseId = "C278364"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultStandalone(runParams.queueName, defaultJndiNames) //without message selector

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }


    @Unroll
    def "Update JMS Queue with all completed field, change JNDI, ignored other fileds (C278377)"() {
        String testCaseId = "C278377"

        def runParams = [
                additionalOptions    : '',
                durable              : '1', //durable=true
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultStandalone(runParams.queueName, "queue/test, java:jboss/exported/jms/queue/test2") //without message selector, durable=false and wrong jndi

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been updated successfully by new jndi names"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }


    @Unroll
    def "Create JMS Queue with 'message selector' with whitespace (C278435)"() {
        String testCaseId = "C278435"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : "\"Any whitespace filter\"",
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "\"Any whitespace filter\"", jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }


    @Unroll
    def "Update JMS Queue, ignored change 'Message Selector' on another (C278440)"() {
        String testCaseId = "C278440"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : "filterTwo",
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueue(runParams.queueName, defaultJndiNames, "--durable=false", " --selector=filterOne", "")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "filterOne", jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
        reloadStandalone()
    }

    @Unroll
    def "Negative. Create JMS Queue without 'Queue Name' (C278355)"() {
        String testCaseId = "C278355"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : '',
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "entries may not be null"
    }

    @Unroll
    def "Negative. Create JMS Queue without 'JNDI Names' (C278356)"() {
        String testCaseId = "C278356"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : '',
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Wrong type for entries. Expected \\[EXPRESSION, STRING\\] but was PROPERTY"
    }

    @Unroll
    def "Negative. Create JMS Queue, with non-existing 'Configuration Name' (C278370)"() {
        String testCaseId = "C278370"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : 'conf_non-existing'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Configuration ${runParams.serverconfig} doesn't exist."
    }

    @Unroll
    def "Create JMS Queue, with 'Durable=true' and --durable=true in additional options (C278374)"() {
        String testCaseId = "C278374"

        def runParams = [
                additionalOptions    : '--durable=true',
                durable              : '0',
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
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)
    }

    @Unroll
    def "Negative. Create JMS Queue with wrong additional option (C278375)"() {
        String testCaseId = "C278375"

        def runParams = [
                additionalOptions    : '--some-wrong-option',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : '',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized argument ${runParams.additionalOptions} for command 'add'."
    }


    void checkCreateOrUpdateJMSQueue(String queueName, String durable, String messageSelector, String jndiNames) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSQueueInfoStandalone(queueName)).result
        String entries = result.'entries'
        assert entries.replaceAll("=\\{", "/").replaceAll("\\}", "") =~ jndiNames //need rewrite after changing run custom command from json to raw text
        assert result.'durable' == durable
        assert result.'selector' == messageSelector
    }

    void removeJMSQueue(String queueName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSQueueStandalone(queueName))
    }

    void reloadStandalone() {
        runCliCommand(CliCommandsGeneratorHelper.reloadStandalone())
    }

    void addJMSQueueDefaultStandalone(String queueName, String jndiName) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueueDefaultStandalone(queueName, jndiName))
    }

    void addJMSQueue(String queueName, String jndiName, String durable, String messageSelector, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueue(queueName, jndiName, durable, messageSelector, profile))
    }

    }