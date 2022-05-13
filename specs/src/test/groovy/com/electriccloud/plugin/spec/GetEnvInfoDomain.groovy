package com.electriccloud.plugin.spec

import spock.lang.*
import com.electriccloud.plugins.annotations.NewFeature
import com.electriccloud.plugins.annotations.Sanity

@Requires({ env.JBOSS_TOPOLOGY == 'master' })
class GetEnvInfoDomain extends PluginTestHelper {

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
                prefix + /"profile" => \{.*"full-ha" => .*/
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
    def "GetEnvInfo, Domain, systemDump with minimum params (systemDump 1)"() {
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
                prefix + /"profile" => \{.*"full-ha" => .*/
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
    def "GetEnvInfo, Domain, systemDump context ignored (systemDump 2)"() {
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
                prefix + /"profile" => \{.*"full-ha" => .*/
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
    def "GetEnvInfo, Domain, systemDump additional options (systemDump 3)"() {
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
                prefix + /"profile" => \{.*"full-ha" => .*/,
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
    def "Negaitve. GetEnvInfo, Domain, systemDump wrong additional options (systemDump 4)"() {
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
    def "GetEnvInfo, Domain, profiles with minimum params (profiles 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeProfiles,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"default" => \{.*"logging" => .*/,
                prefix + /"default" => \{.*"datasources" => .*/,
                prefix + /"full" => \{.*"logging" => .*/,
                prefix + /"full" => \{.*"datasources" => .*/,
                prefix + /"full-ha" => \{.*"logging" => .*/,
                prefix + /"full-ha" => \{.*"datasources" => .*/,
                prefix + /"ha" => \{.*"logging" => .*/,
                prefix + /"ha" => \{.*"datasources" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, profiles context ignored (profiles 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeProfiles,
                informationTypeContext: 'someContext',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"default" => \{.*"logging" => .*/,
                prefix + /"default" => \{.*"datasources" => .*/,
                prefix + /"full" => \{.*"logging" => .*/,
                prefix + /"full" => \{.*"datasources" => .*/,
                prefix + /"full-ha" => \{.*"logging" => .*/,
                prefix + /"full-ha" => \{.*"datasources" => .*/,
                prefix + /"ha" => \{.*"logging" => .*/,
                prefix + /"ha" => \{.*"datasources" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, profiles additional options (profiles 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeProfiles,
                informationTypeContext: '',
                additionalOptions     : 'include-runtime=true,include-defaults=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)
        def envInfo = getJobProperty('/myJob/jobSteps/GetEnvInfo/envInfo', runProcedureJob.getJobId())

        then:
        assert runProcedureJob.getStatus() == "success"

        String prefix = /"outcome" => "success",.*"result" => \{.*/
        String[] envInfoPatterns = [
                prefix + /"default" => \{.*"logging" => .*/,
                prefix + /"default" => \{.*"datasources" => .*/,
                prefix + /"full" => \{.*"logging" => .*/,
                prefix + /"full" => \{.*"datasources" => .*/,
                prefix + /"full-ha" => \{.*"logging" => .*/,
                prefix + /"full-ha" => \{.*"datasources" => .*/,
                prefix + /"ha" => \{.*"logging" => .*/,
                prefix + /"ha" => \{.*"datasources" => .*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Domain, profiles wrong additional options (profiles 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeProfiles,
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
    def "GetEnvInfo, Domain, dataSources with minimum params on full profile (dataSources 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: 'full',
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
    def "GetEnvInfo, Domain, dataSources additional options on full profile (dataSources 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: 'full',
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
    def "Negaitve. GetEnvInfo, Domain, dataSources wrong additional options on full profile (dataSources 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: 'full',
                additionalOptions     : 'wrong-option=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /'wrong-option' is not found among the supported properties/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Domain, dataSources wrong profile (dataSources 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeDataSources,
                informationTypeContext: 'wrong-profile',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
//        assert runProcedureJob.getUpperStepSummary() =~ /todo/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, dataSources with minimum params on all profiles (dataSources 5)"() {
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
                /Profile 'full': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'full': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'full': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'full-ha': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'full-ha': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'full-ha': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'ha': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'ha': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'ha': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'default': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'default': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'default': .*/ + prefix + /"user-name" => "sa",.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, dataSources additional options on all profiles (dataSources 6)"() {
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
                /Profile 'full': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'full': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'full': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'full-ha': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'full-ha': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'full-ha': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'ha': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'ha': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'ha': .*/ + prefix + /"user-name" => "sa",.*/,
                /Profile 'default': .*/ + prefix + /"driver-name" => "h2",.*/,
                /Profile 'default': .*/ + prefix + /"password" => "\*\*\*",.*/,
                /Profile 'default': .*/ + prefix + /"user-name" => "sa",.*/
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Domain, dataSources wrong additional options on all profiles (dataSources 7)"() {
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
    def "GetEnvInfo, Domain, xaDataSources with minimum params on full profile (xaDataSources 1)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: 'full',
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
    def "GetEnvInfo, Domain, xaDataSources additional options on full profile (xaDataSources 2)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: 'full',
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
    def "Negaitve. GetEnvInfo, Domain, xaDataSources wrong additional options on full profile (xaDataSources 3)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: 'full',
                additionalOptions     : 'wrong-option=true'
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
        assert runProcedureJob.getUpperStepSummary() =~ /'wrong-option' is not found among the supported properties/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Domain, xaDataSources wrong profile (xaDataSources 4)"() {
        when:
        def runParams = [
                      config          : defaultConfigName,
                informationType       : informationTypeXaDataSources,
                informationTypeContext: 'wrong-profile',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
//        assert runProcedureJob.getUpperStepSummary() =~ /todo/
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, xaDataSources with minimum params on all profiles (xaDataSources 5)"() {
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

        //todo
        String prefix = /"outcome" => "success",.*"result" => \{\}.*/
        String[] envInfoPatterns = [
                /Profile 'full': .*/ + prefix,
                /Profile 'full-ha': .*/ + prefix,
                /Profile 'ha': .*/ + prefix,
                /Profile 'default': .*/ + prefix
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "GetEnvInfo, Domain, xaDataSources additional options on all profiles (xaDataSources 6)"() {
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

        //todo
        String prefix = /"outcome" => "success",.*"result" => \{\}.*/
        String[] envInfoPatterns = [
                /Profile 'full': .*/ + prefix,
                /Profile 'full-ha': .*/ + prefix,
                /Profile 'ha': .*/ + prefix,
                /Profile 'default': .*/ + prefix
        ]

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @NewFeature(pluginVersion = "2.6.0")
    @Unroll
    def "Negaitve. GetEnvInfo, Domain, xaDataSources wrong additional options on all profiles (xaDataSources 7)"() {
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