package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

class DeleteConfiguration extends PluginTestHelper {

    @Shared
    String procName = 'DeleteConfiguration'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        config            : '',
                ]
        ]

        createHelperProject(resName, '')
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        conditionallyDeleteProject(projectName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

    @Sanity
    @Unroll
    def "Sanity"() {
        String testCaseId = "C289709"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : config_name
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "DeleteConfiguration (C289709)"() {
        String testCaseId = "C289709"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : config_name
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "success"
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "DeleteConfiguration, without 'Configuration name' (C289713)"() {
        String testCaseId = "C289713"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : ''
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "DeleteConfiguration, with not existing 'Configuration name' (C289714)"() {
        String testCaseId = "C289714"
        String config_name = "jboss_conf_not_existing"+testCaseId

        def runParams = [
                config              : config_name
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

}