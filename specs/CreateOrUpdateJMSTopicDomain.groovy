import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class CreateOrUpdateJMSTopicDomain extends PluginTestHelper {

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
    def "Create JMS Topic with minimum parameters (C278451)"() {
        String testCaseId = "C278451"

        def runParams = [
                additionalOptions: '',
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }

/*    @Unroll
    def "Create JMS Topic with 'Profile' and 'Profile' in additional options (C278498)"() {
        String testCaseId = "C278498"

        def runParams = [
                additionalOptions: "--profile=$defaultProfile",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }


    @Unroll
    def "Create JMS Topic, on other profile with the same parameters (C278452)"() {
        String testCaseId = "C278452"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : 'full-ha',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        addJMSTopicDefaultDomain(runParams.topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile)
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, "full-ha")

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
        removeJMSTopic(topicName, "full-ha")
    }



    @Unroll
    def "Update JMS Topic with the same parameters (C278499)"() {
        String testCaseId = "C278499"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        addJMSTopicDefaultDomain(runParams.topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' is up-to-date"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }


    @Unroll
    def "Negative. Create JMS Topic without 'Topic Name' (C278458)"() {
        String testCaseId = "C278458"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
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
    def "Negative. Create JMS Topic without 'JNDI Names' (C278459)"() {
        String testCaseId = "C278459"

        def runParams = [
                additionalOptions: "",
                jndiNames        : '',
                profile          : defaultProfile,
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
    def "Negative. Create JMS Topic, with non-existing 'Configuration Name' (C278462)"() {
        String testCaseId = "C278462"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
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
    def "Negative. Create JMS Topic with wrong additional option (C278463)"() {
        String testCaseId = "C278463"

        def runParams = [
                additionalOptions: "--some-wrong-option",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        String command = 'add'
        if(env.JBOSS_VERSION =~ '6.0'){
            command = "org.jboss.as.cli.handlers.GenericTypeOperationHandler"
        }
        assert runProcedureJob.getUpperStepSummary() =~ "Unrecognized argument ${runParams.additionalOptions} for command '$command'."

    }

    @Unroll
    def "Negative. Create JMS Topic without 'Profile' (C278465)"() {
        String testCaseId = "C278465"

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
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"

    }

    @Unroll
    def "Negative. Create JMS Topic with wronge 'Profile' (C278466)"() {
        String testCaseId = "C278466"

        def runParams = [
                additionalOptions: '',
                jndiNames        : defaultJndiNames,
                profile          : 'non-existing-profile',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Management resource .*${runParams.profile}.* not found"

    }

    @Unroll
    def "Negative. Update JMS Topic without 'Profile' (C278519)"() {
        String testCaseId = "C278519"

        def runParams = [
                additionalOptions: '',
                jndiNames        : defaultJndiNames,
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",

                ]
        setup:
        addJMSTopicDefaultDomain(runParams.topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"

        cleanup:
        String topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }

    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "Create JMS Topic with additional option --legacy-entries (C278556)"() {
        String testCaseId = "C278556"

        def runParams = [
                additionalOptions: '--legacy-entries=java:/test,java:/test2',
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been added successfully"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile, "java:/test, java:/test2")

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }

    @Unroll
    def "Update JMS Topic, change 'JNDI Names' (C278453)"() {
        String testCaseId = "C278453"

        def runParams = [
                additionalOptions: "",
                jndiNames        : defaultJndiNames,
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        addJMSTopicDefaultDomain(runParams.topicName, "topic/test3,java:jboss/exported/jms/topic/test3", defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' has been updated successfully by new jndi names*(reload-required|restart)*"

        String topicName = "testTopic-$testCaseId"
        checkCreateOrUpdateJMSTopic(topicName, expectedJndiNames, defaultProfile)

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }*/

    void checkCreateOrUpdateJMSTopic(String topicName, String jndiNames, String profile) {
        logger.debug("env "+EnvPropertiesHelper.getVersion())
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSTopicInfoDomain(topicName, profile)).result
        assert result.'entries' =~ jndiNames //need rewrite after changing run custom command from json to raw text
    }

    void checkCreateOrUpdateJMSTopic(String topicName, String jndiNames, String profile, String legacy) {
        logger.debug("env "+EnvPropertiesHelper.getVersion())
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getJMSTopicInfoDomain(topicName, profile)).result
        assert result.'entries' =~ jndiNames //need rewrite after changing run custom command from json to raw text
        assert result.'legacy-entries' =~ legacy
    }

    void removeJMSTopic(String topicName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSTopicDomain(topicName, profile))
    }

    void addJMSTopicDefaultDomain(String topicName, String jndiName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSTopicDomain(topicName, jndiName, " --profile=$profile"))
    }


}
