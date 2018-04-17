import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Ignore
class CreateOrUpdateJMSTopicStandalone extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateJMSTopic'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultJndiNames = 'topic/test,java:jboss/exported/jms/topic/test'
    @Shared
    String expectedJndiNames = 'topic/test, java:jboss/exported/jms/topic/test'


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

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
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
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames)

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
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
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
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'topicName' is not provided"
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
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'jndiNames' is not provided"

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
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.0' })
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
        String command = 'add'
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized argument ${runParams.additionalOptions} for command '$command'."

    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "Create JMS Topic with additional option --legacy-entries (C278556)"() {
        String testCaseId = "C278556"

        def runParams = [
                additionalOptions: '--legacy-entries=java:/test,java:/test2',
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
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, "java:/test, java:/test2")

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
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been updated successfully by new jndi names*(reload-required|restart)*"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    void checkCreateOrUpdateJMSTopic(String topicName, String jndiNames, String legacy) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSTopicInfoStandalone(topicName)).result
        assert result.'entries' =~ jndiNames //need rewrite after changing run custom command from json to raw text
        assert result.'legacy-entries' =~ legacy
    }

    void checkCreateOrUpdateJMSTopic(String topicName, String jndiNames) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSTopicInfoStandalone(topicName)).result
        assert result.'entries' =~ jndiNames //need rewrite after changing run custom command from json to raw text
    }

    void removeJMSTopic(String topicName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSTopicStandalone(topicName))
    }

    void addJMSTopicDefaultDomain(String topicName, String jndiName) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSTopicStandalone(topicName, jndiName))
    }
}