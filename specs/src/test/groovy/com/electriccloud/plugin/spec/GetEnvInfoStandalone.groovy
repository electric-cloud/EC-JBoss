package com.electriccloud.plugin.spec

import com.electriccloud.plugin.spec.Services.CliCommandsGeneratorHelper
import com.electriccloud.plugin.spec.Utils.EnvPropertiesHelper
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class GetEnvInfoStandalone extends PluginTestHelper {

    @Shared
    String procName = 'GetEnvInfo'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = "specConfig-${procName}"

    @Shared
    String informationTypeSystemDump = 'systemDump'
    @Shared
    String informationTypeProfiles = 'profiles'
    @Shared
    String informationTypeDataSources = 'dataSources'
    @Shared
    String informationTypeXaDataSources = 'xaDataSources'


    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()
        logger.info("Hello World! doSetupSpec")

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                              config          : '',
                        informationType       : '',
                        informationTypeContext: '',
                        additionalOptions     : '',
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
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeSystemDump,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"product-name" => .*/,
                prefix + /"deployment" => .*/,
                prefix + /"extension" => \{.*/,
                prefix + /"subsystem" => \{.*"datasources" => .*/
        ]
        String[] envInfoPatternsNotExixsting = [
                prefix + /"launch-type" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }

        for (String envInfoPattern: envInfoPatternsNotExixsting) {
            assert !(runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern)
            assert !(envInfo =~ /(?s)/ + envInfoPattern)
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, systemDump with minimum params (systemDump 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeSystemDump,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"product-name" => .*/,
                prefix + /"deployment" => .*/,
                prefix + /"extension" => \{.*/,
                prefix + /"subsystem" => \{.*"datasources" => .*/
        ]
        String[] envInfoPatternsNotExixsting = [
                prefix + /"launch-type" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }

        for (String envInfoPattern: envInfoPatternsNotExixsting) {
            assert !(runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern)
            assert !(envInfo =~ /(?s)/ + envInfoPattern)
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, systemDump context ignored (systemDump 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeSystemDump,
                informationTypeContext: 'someContext',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"product-name" => .*/,
                prefix + /"deployment" => .*/,
                prefix + /"extension" => \{.*/,
                prefix + /"subsystem" => \{.*"datasources" => .*/
        ]
        String[] envInfoPatternsNotExixsting = [
                prefix + /"launch-type" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }

        for (String envInfoPattern: envInfoPatternsNotExixsting) {
            assert !(runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern)
            assert !(envInfo =~ /(?s)/ + envInfoPattern)
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, systemDump additional options (systemDump 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeSystemDump,
                informationTypeContext: '',
                additionalOptions     : 'include-runtime=true,include-defaults=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"product-name" => .*/,
                prefix + /"deployment" => .*/,
                prefix + /"extension" => \{.*/,
                prefix + /"subsystem" => \{.*"datasources" => .*/,
                prefix + /"launch-type" => .*/
        ]
        String[] envInfoPatternsNotExixsting = []

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }

        for (String envInfoPattern: envInfoPatternsNotExixsting) {
            assert !(runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern)
            assert !(envInfo =~ /(?s)/ + envInfoPattern)
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Standalone, systemDump wrong additional options (systemDump 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeSystemDump,
                informationTypeContext: '',
                additionalOptions     : 'wrong-option=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /'wrong-option' is not found among the supported properties/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, requesting profiles when standalone (profiles 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeProfiles,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /No known child type named profile/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, dataSources with minimum params (dataSources 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*"ExampleDS" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"driver-name" => "h2",.*/,
                prefix + /"password" => "\*\*\*",.*/,
                prefix + /"user-name" => "sa",.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, dataSources context ignored (dataSources 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: 'someContext',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*"ExampleDS" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"driver-name" => "h2",.*/,
                prefix + /"password" => "\*\*\*",.*/,
                prefix + /"user-name" => "sa",.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, dataSources additional options (dataSources 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: '',
                additionalOptions     : 'include-runtime=true,include-defaults=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*"ExampleDS" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"driver-name" => "h2",.*/,
                prefix + /"password" => "\*\*\*",.*/,
                prefix + /"user-name" => "sa",.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Standalone, dataSources wrong additional options (dataSources 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: '',
                additionalOptions     : 'wrong-option=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /'wrong-option' is not found among the supported properties/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, xaDataSources with minimum params (xaDataSources 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        //todo:
        String[] envInfoPatterns = [
                /"outcome" => "success",.*"result" => \{\}.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, xaDataSources context ignored (xaDataSources 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: 'someContext',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        //todo:
        String[] envInfoPatterns = [
                /"outcome" => "success",.*"result" => \{\}.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Standalone, xaDataSources additional options (xaDataSources 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: '',
                additionalOptions     : 'include-runtime=true,include-defaults=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        //todo:
        String[] envInfoPatterns = [
                /"outcome" => "success",.*"result" => \{\}.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Standalone, xaDataSources wrong additional options (xaDataSources 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: '',
                additionalOptions     : 'wrong-option=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /'wrong-option' is not found among the supported properties/
    }


}