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

        def envInfoPattern = /"outcome" => "success",.*/ +
                /"result" => \{.*/ +
                /"product-name" => .*/ +
                /"deployment" => .*/ +
                /"extension" => \{.*/ +
                /"subsystem" => \{.*/ +
                /"datasources" => .*/
        assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
        assert envInfo =~ /(?s)/ + envInfoPattern
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

        def envInfoPattern = /"outcome" => "success",.*/ +
                /"result" => \{.*/ +
                /"ExampleDS" => \{.*/ +
                /"driver-name" => "h2",.*/ +
                /"password" => "\*\*\*",.*/ +
                /"user-name" => "sa",.*/
        assert runProcedureJob.getLogs() =~ /(?s)/ + /Requested Environment Information.*/ + envInfoPattern
        assert envInfo =~ /(?s)/ + envInfoPattern
    }


}