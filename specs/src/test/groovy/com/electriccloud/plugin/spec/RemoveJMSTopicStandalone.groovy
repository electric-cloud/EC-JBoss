package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class RemoveJMSTopicStandalone extends PluginTestHelper {

    @Shared
    String procName = 'RemoveJMSTopic'
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
                        profile     : '',
                        serverconfig: '',
                        topicName   : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)

    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        conditionallyDeleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Sanity
    @Unroll
    def "Sanity"() {
        String testCaseId = "C278481"

        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : '',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultStandalone(topicName, defaultJndiNames)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getLogs() =~ "Parameter 'topicName' of procedure 'RemoveJMSTopic' is marked as required, but it does not have a value. Aborting with fatal error."
        //assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'topicName' is not provided"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Remove JMS Topic without 'Topic Name' (C278481)"() {
        String testCaseId = "C278481"

        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : '',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultStandalone(topicName, defaultJndiNames)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'topicName' is not provided"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Remove JMS Topic, non existing Topic Name (C278482)"() {
        String testCaseId = "C278482"

        def runParams = [
                profile          : '',
                serverconfig     : defaultConfigName,
                topicName        : 'testTopic-non-existing',
        ]

        setup:
        String topicName = "testTopic-$testCaseId"
        addJMSTopicDefaultStandalone(topicName, defaultJndiNames)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "warning"
        assert runProcedureJob.getUpperStepSummary() =~ "JMS topic '${runParams.topicName}' not found"

        cleanup:
        topicName = "testTopic-$testCaseId"
        removeJMSTopic(topicName)
    }

    void removeJMSTopic(String topicName) {
        runCliCommand(CliCommandsGeneratorHelper.removeJMSTopicStandalone(topicName))
    }

    void addJMSTopicDefaultStandalone(String topicName, String jndiName) {
        runCliCommand(CliCommandsGeneratorHelper.addJMSTopicStandalone(topicName, jndiName))
    }



}