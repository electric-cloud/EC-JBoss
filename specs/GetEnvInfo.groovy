import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

class GetEnvInfo extends PluginTestHelper {

    @Shared
    String procName = 'GetEnvInfo'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'

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
                        serverconfig          : '',
                        informationType       : '',
                        informationTypeContext: '',
                        additionalOptions     : '',
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
    @Requires({ env.JBOSS_MODE == 'standalone' })
    def "GetEnvInfo, Standalone, systemDump with minimum params (systemDump1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @Unroll
    @Requires({ env.JBOSS_MODE == 'standalone' })
    def "Negaitve. GetEnvInfo, requesting profiles when standalone (testsdkfj)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
                informationType       : informationTypeProfiles,
                informationTypeContext: '',
                additionalOptions     : ''
        ]
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        assert runProcedureJob.getStatus() == "error"
    }

    @Unroll
    @Requires({ env.JBOSS_MODE == 'standalone' })
    def "GetEnvInfo, Standalone, dataSources with minimum params - check default data source (dataSources1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

    @Unroll
    @Requires({ env.JBOSS_MODE == 'standalone' })
    def "GetEnvInfo, Standalone, dataSources with minimum params - check default data source (xaDataSources1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

    @Unroll
    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    def "GetEnvInfo, Domain, systemDump with minimum params (systemDump1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

        for (String envInfoPattern: envInfoPatterns) {
            assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
            assert envInfo =~ /(?s)/ + envInfoPattern
        }
    }

    @Unroll
    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    def "GetEnvInfo, Domain, profiles with minimum params (ksmdkm)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

    @Unroll
    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    def "GetEnvInfo, Domain, dataSources with minimum params - check default data source on full profile (dataSources1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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

    @Unroll
    @Requires({ env.JBOSS_TOPOLOGY == 'master' })
    def "GetEnvInfo, Domain, dataSources with minimum params - check default data source on all profiles (dataSources1)"() {
        when:
        def runParams = [
                serverconfig          : defaultConfigName,
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


}