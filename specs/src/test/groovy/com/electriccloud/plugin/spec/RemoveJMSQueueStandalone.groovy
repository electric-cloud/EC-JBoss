package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class RemoveJMSQueueStandalone extends PluginTestHelper {

    @Shared
    String procName = 'RemoveJMSQueue'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"
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
        conditionallyDeleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Sanity
    @Unroll
    def "Sanity"() {
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
        assert  runProcedureJob.logs =~ "Parameter 'queueName' of procedure 'RemoveJMSQueue' is marked as required, but it does not have a value. Aborting with fatal error."
        //assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'queueName' is not provided"

        cleanup:
        queueName = "testQueue-$testCaseId"
        removeJMSQueue(queueName)

    }

    @NewFeature(pluginVersion = "2.6.0")
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

    @NewFeature(pluginVersion = "2.6.0")
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