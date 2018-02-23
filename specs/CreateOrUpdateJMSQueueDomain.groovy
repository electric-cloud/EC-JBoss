import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*


@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class CreateOrUpdateJMSQueueDomain extends PluginTestHelper {

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
    @Shared
    String defaultProfile = 'full'
    @Shared
    String defaultServerGroup = 'main-server-group'

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
    def "Create JMS Queue with minimum parameters (C278381)"() {
        String testCaseId = "C278381"

        def runParams = [
                additionalOptions    : '',
                durable              : '',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Create JMS Queue with 'Durable=true' (C278382)"() {
        String testCaseId = "C278382"

        def runParams = [
                additionalOptions    : '',
                durable              : '1',  //true
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Create JMS Queue with message selector (C278385)"() {
        String testCaseId = "C278385"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "FILTER_EXPRESSION", jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Create JMS Queue, --durable=true in additional option (C278390)"() {
        String testCaseId = "C278390"

        def runParams = [
                additionalOptions    : '--durable=true',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Create JMS Queue with all completed field (C278391)"() {
        String testCaseId = "C278391"

        def runParams = [
                additionalOptions    : '',
                durable              : '1',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, "true", "FILTER_EXPRESSION", jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }



    @Unroll
    def "Update JMS Queue, ignored change 'Durable' (C278387)"() {
        String testCaseId = "C278387"

        def runParams = [
                additionalOptions    : '',
                durable              : '1', //durable=true
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultDomain(runParams.queueName, defaultJndiNames, defaultProfile) //durable=false

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Update JMS Queue, ignored change 'Message Selector' (C278388)"() {
        String testCaseId = "C278388"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultDomain(runParams.queueName, defaultJndiNames, defaultProfile) //without message selector

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    @Unroll
    def "Update JMS Queue, on other profile with the same parameters (C278409)"() {
        String testCaseId = "C278409"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : 'full-ha',
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]

        setup:
        addJMSQueueDefaultDomain(runParams.queueName, "queue/test, java:jboss/exported/jms/queue/test", defaultProfile) //without message selector, durable=false and wrong jndi

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been added successfully"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, "full-ha")

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
        removeJMSQueue(queueName, "full-ha")
    }

    @Unroll
    def "Create JMS Queue with 'message selector' with whitespace (C278436)"() {
        String testCaseId = "C278436"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : "Any whitespace filter",
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "Any whitespace filter", jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    @Unroll
    def "Update JMS Queue, ignored change 'Message Selector' on another (C278438)"() {
        String testCaseId = "C278438"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : "filterTwo",
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueue(runParams.queueName, defaultJndiNames, "--durable=false", " --selector=filterOne", " --profile=$defaultProfile")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' is up-to-date"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, "filterOne", jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    @Unroll
    def "Negative. Create JMS Queue without 'Queue Name' (C278396)"() {
        String testCaseId = "C278396"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
                queueName            : '',
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'queueName' is not provided"
    }

    @Unroll
    def "Negative. Create JMS Queue without 'JNDI Names' (C278397)"() {
        String testCaseId = "C278397"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : '',
                messageSelector      : '',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jndiNames' is not provided"
    }

    @Unroll
    def "Negative. Create JMS Queue, with non-existing 'Configuration Name' (C278370)"() {
        String testCaseId = "C278370"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
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
    def "Create JMS Queue, with 'Durable=true' and --durable=true in additional options (C278401)"() {
        String testCaseId = "C278401"

        def runParams = [
                additionalOptions    : '--durable=true',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
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
        checkCreateOrUpdateJMSQueue(queueName, "true", defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Negative. Create JMS Queue with wrong additional option (C278402)"() {
        String testCaseId = "C278402"

        def runParams = [
                additionalOptions    : '--some-wrong-option',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized argument ${runParams.additionalOptions} for command 'add'."
    }

    @Unroll
    def "Negative. Create JMS Queue without 'Profile' (C278412)"() {
        String testCaseId = "C278412"

        def runParams = [
                additionalOptions    : '',
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"
    }

    @Unroll
    def "Negative. Update JMS Queue without 'Profile' (C278413)"() {
        String testCaseId = "C278413"

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
        addJMSQueue(runParams.queueName, defaultJndiNames, "--durable=false", "", " --profile=$defaultProfile")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"

        cleanup:
        String queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }

    @Unroll
    def "Create JMS Queue with additional option --legacy-entries (C278381)"() {
        String testCaseId = "C278381"

        def runParams = [
                additionalOptions: '--legacy-entries=java:/test,java:/test2',
                durable          : '0',
                jndiNames        : defaultJndiNames,
                messageSelector  : '',
                profile          : defaultProfile,
                queueName        : "testQueue-$testCaseId",
                serverconfig     : defaultConfigName
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been added successfully"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile,"test=java:/test, test2=java:/test2")

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    @Unroll
    def "Update JMS Queue, change 'JNDI Names' (C278386)"() { //need manual check with app
        String testCaseId = "C278386"

        def runParams = [
                additionalOptions    : '',
                durable              : '0',
                jndiNames            : defaultJndiNames,
                messageSelector      : '',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultDomain(runParams.queueName, "queue/test3, java:jboss/exported/jms/queue/test3", defaultProfile) //wrong jndi name

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been updated successfully by new jndi names*(reload-required|restart)*"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }


    @Unroll
    def "Update JMS Queue with all completed field, change JNDI, ignored other fileds (C278392)"() {
        String testCaseId = "C278392"

        def runParams = [
                additionalOptions    : '',
                durable              : '1', //durable=true
                jndiNames            : defaultJndiNames,
                messageSelector      : 'FILTER_EXPRESSION',
                profile              : defaultProfile,
                queueName            : "testQueue-$testCaseId",
                serverconfig         : defaultConfigName
        ]
        setup:
        addJMSQueueDefaultDomain(runParams.queueName, "queue/test4, java:jboss/exported/jms/queue/test4", defaultProfile) //without message selector, durable=false and wrong jndi

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS queue '${runParams.queueName}' has been updated successfully by new jndi names*(reload-required|restart)*"

        String queueName = "testQueue-$testCaseId"
        String jndiName = "java:jboss/exported/jms/queue/test, queue/test"
        checkCreateOrUpdateJMSQueue(queueName, defaultDurable, defaultMessageSelector, jndiName, defaultProfile)

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName, defaultProfile)
    }



    void checkCreateOrUpdateJMSQueue(String queueName, String durable, String messageSelector, String jndiNames, String profile, String legacy) { //check with legacy
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSQueueInfoDomain(queueName, profile)).result
        String entries = result.'entries'
        assert entries.replaceAll("=\\{", "/").replaceAll("\\}", "") =~ jndiNames //need rewrite after changing run custom command from json to raw text
        assert result.'durable' == durable
        assert result.'selector' == messageSelector
        String legacyActual = result.'legacy-entries'
        assert legacyActual.replaceAll("=\\{", "/").replaceAll("\\}", "") =~ legacy
    }

    void checkCreateOrUpdateJMSQueue(String queueName, String durable, String messageSelector, String jndiNames, String profile) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSQueueInfoDomain(queueName, profile)).result
        String entries = result.'entries'
        assert entries.replaceAll("=\\{", "/").replaceAll("\\}", "") =~ jndiNames //need rewrite after changing run custom command from json to raw text
        assert result.'durable' == durable
        assert result.'selector' == messageSelector
    }

    void removeJMSQueue(String queueName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSQueueDomain(queueName, profile))
    }

    void reloadServerGroupDomain() {
        runCliCommand(CliCommandsGeneratorHelper.reloadServerGroupDomain(defaultServerGroup))
    }

    void addJMSQueueDefaultDomain(String queueName, String jndiName, String domain) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueueDefaultDomain(queueName, jndiName, domain))
    }

    void addJMSQueue(String queueName, String jndiName, String durable, String messageSelector, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSQueue(queueName, jndiName, durable, messageSelector, profile))
    }
}