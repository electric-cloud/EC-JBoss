import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*


@IgnoreIf({ env.JBOSS_MODE == 'domain' })
class CreateOrUpdateJMSTopicStandalone extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateJMSTopic'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultJndiNames = 'topic/test,java:jboss/exported/jms/topic/test'


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
                        additionalOptions: '',
                        jndiNames        : '',
                        profile          : '',
                        serverconfig     : '',
                        topicName        : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)

    }


    @Unroll
    def "Create JMS Topic with minimum parameters (C278441)"() {
        String testCaseId = "C278441"

        def runParams = [
                additionalOptions: '',
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        String jndiName = 'java:jboss/exported/jms/topic/test, topic/test'
        checkCreateOrUpdateJMSTopic(topicName, jndiName)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    @Unroll
    def "Create JMS Topic, ignored profile (C278442)"() {
        String testCaseId = "C278442"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : 'full',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        String jndiName = 'java:jboss/exported/jms/topic/test, topic/test'
        checkCreateOrUpdateJMSTopic(topicName, jndiName)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    @Unroll
    def "Update JMS Topic, change 'JNDI Names' (C278443)"() {
        String testCaseId = "C278443"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        addJMSTopicDefaultDomain(runParams.topicName, "topic/test2,java:jboss/exported/jms/topic/test2")

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been updated successfully by new jndi names"

        String topicName = "testTopic-$testCaseId"
        String jndiName = 'java:jboss/exported/jms/topic/test, topic/test'
        checkCreateOrUpdateJMSTopic(topicName, jndiName)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
        reloadStandalone()
    }

    @Unroll
    def "Update JMS Topic with the same parameters (C278501)"() {
        String testCaseId = "C278501"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        addJMSTopicDefaultDomain(runParams.topicName, defaultJndiNames)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' is up-to-date"

        String topicName = "testTopic-$testCaseId"
        String jndiName = 'java:jboss/exported/jms/topic/test, topic/test'
        checkCreateOrUpdateJMSTopic(topicName, jndiName)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
        reloadStandalone()
    }

    @Unroll
    def "Negative. Create JMS Topic without 'Topic Name' (C278445)"() {
        String testCaseId = "C278445"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : '',
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "entries may not be null"
    }

    @Unroll
    def "Negative. Create JMS Topic without 'JNDI Names' (C278446)"() {
        String testCaseId = "C278446"

        def runParams = [
                additionalOptions: "",
                jndiNames        : '',
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Failed to handle 'jms-topic add  --topic-address=${runParams.topicName} --entries= ': newValue is null"

    }

    @Unroll
    def "Negative. Create JMS Topic, with non-existing 'Configuration Name' (C278449)"() {
        String testCaseId = "C278449"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : "jboss_conf_non-existing",
                topicName        : "testTopic-$testCaseId",
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Configuration ${runParams.serverconfig} doesn't exist."

    }

    @Unroll
    def "Negative. Create JMS Topic with wrong additional option (C278450)"() {
        String testCaseId = "C278450"

        def runParams = [
                additionalOptions: "--some-wrong-option",
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized argument ${runParams.additionalOptions} for command 'add'."

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
//        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }


    void checkCreateOrUpdateJMSTopic(String topicName, String jndiNames) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSTopicInfoStandalone(topicName)).result
        String entries = result.'entries'
        assert entries.replaceAll("=\\{", "/").replaceAll("\\}", "") =~ jndiNames //need rewrite after changing run custom command from json to raw text
    }

    void removeJMSTopic(String topicName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSTopicStandalone(topicName))
    }

    void reloadStandalone() {
        runCliCommand(CliCommandsGeneratorHelper.reloadStandalone())
    }

    void addJMSTopicDefaultDomain(String topicName, String jndiName) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSTopicStandalone(topicName, jndiName))
    }
}