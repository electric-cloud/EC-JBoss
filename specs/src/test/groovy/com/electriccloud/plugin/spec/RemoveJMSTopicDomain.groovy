package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import spock.lang.*
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class RemoveJMSTopicDomain extends PluginTestHelper {

    @Shared
    String procName = 'RemoveJMSTopic'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultProfile = 'full'
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
                        profile      : '',
                        serverconfig : '',
                        topicName    : '',
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

    @Sanity
    @Unroll
    def "Sanity"() {
        String testCaseId = "C278471"

        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : '',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultDomain(topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'topicName' is not provided"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }


    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Remove JMS Topic without 'Topic Name' (C278471)"() {
        String testCaseId = "C278471"

        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : '',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultDomain(topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'topicName' is not provided"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Remove JMS Topic, non existing Topic Name (C278472)"() {
        String testCaseId = "C278472"

        def runParams = [
                profile          : defaultProfile,
                serverconfig     : defaultConfigName,
                topicName        : 'testTopic-non-existing',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultDomain(topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' not found"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
    }


    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Remove JMS Topic, without 'Profile' (C278469)"() {
        String testCaseId = "C278469"

        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : "testTopic-$testCaseId",
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultDomain(topicName, defaultJndiNames, defaultProfile)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'profile' is not provided \\(parameter required for JBoss domain\\)"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName, defaultProfile)
        reloadServerGroupDomain() //for right next suit
    }

    void removeJMSTopic(String topicName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSTopicDomain(topicName, profile))
    }

    void addJMSTopicDefaultDomain(String topicName, String jndiName, String profile) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSTopicDomain(topicName, jndiName, "--profile=$profile"))
    }

    void reloadServerGroupDomain() {
        runCliCommand(CliCommandsGeneratorHelper.reloadServerGroupDomain("main-server-group"))
        runCliCommand(CliCommandsGeneratorHelper.reloadServerGroupDomain("other-server-group"))
    }

}