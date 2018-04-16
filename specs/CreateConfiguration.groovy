import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

class CreateConfiguration extends PluginTestHelper {

    @Shared
    String procName = 'CreateConfiguration'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = EnvPropertiesHelper.getJbossCliPath()
    @Shared
    String defaultJbossURL = EnvPropertiesHelper.getJbossControllerUrl()


    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        config              : '',
                        credential          : '',
                        java_opts           : '',
                        jboss_url           : '',
                        log_level           : '',
                        scriptphysicalpath  : '',
                ]
        ]

        createHelperProject(resName, '')
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters, def credential) {
        return runProcedureDsl(projectName, procName, parameters, credential)
    }

    static String getJava_opts(){
        String java_opts = "-Xmx1024M"
        EnvPropertiesHelper.getOS() == "WINDOWS" ? java_opts = "Xmx1024M" : java_opts
        return  java_opts
    }

    @Unroll
    def "Create Configuration, minimum parameters (C111844)"() {
        String testCaseId = "C289654"
        String config_name = "config-"+testCaseId

        def runParams = [
                config              : config_name,
                java_opts           : '',
                jboss_url           : defaultJbossURL,
                log_level           : '',
                scriptphysicalpath  : ''
        ]
        def credential = [
                credentialName: 'credential',
                userName: '',
                password: ''
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "Create Configuration, all fields are filled, Log Level=DEBUG (C289655)"() {
        String testCaseId = "C289655"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : config_name,
                java_opts           : getJava_opts(),
                jboss_url           : defaultJbossURL,
                log_level           : '4',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "Create Configuration, all fields are filled, Log Level=INFO (C289656)"() {
        String testCaseId = "C289656"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : config_name,
                java_opts           : getJava_opts(),
                jboss_url           : defaultJbossURL,
                log_level           : '1',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "Create Configuration, all fields are filled, Log Level=WARNING (C289658)"() {
        String testCaseId = "C289658"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : config_name,
                java_opts           : getJava_opts(),
                jboss_url           : defaultJbossURL,
                log_level           : '2',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "Create Configuration, all fields are filled, Log Level=ERROR (C289659)"() {
        String testCaseId = "C289659"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : config_name,
                java_opts           : getJava_opts(),
                jboss_url           : defaultJbossURL,
                log_level           : '3',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "success"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }

    @Unroll
    def "Create Configuration, without 'Configuration name' (C289667)"() {
        String testCaseId = "C289667"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : '',
                java_opts           : '',
                jboss_url           : defaultJbossURL,
                log_level           : '4',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"

    }

    @Unroll
    @Ignore //need run after fix bug ECPAPPSERVERJBOSS-650
    def "Create Configuration, without 'JBoss controller location' (C289668)"() {
        String testCaseId = "C289668"
        String config_name = "config-"+testCaseId


        def runParams = [
                config              : config_name,
                java_opts           : '',
                jboss_url           : '',
                log_level           : '4',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"

    }

    @Unroll
    def "Create Configuration, configuration with the same name already exist (C289704)"() {
        String testCaseId = "C289704"
        String config_name = "jboss_exist"


        def runParams = [
                config              : config_name,
                java_opts           : '',
                jboss_url           : defaultJbossURL,
                log_level           : '4',
                scriptphysicalpath  : defaultCliPath
        ]
        def credential = [
                credentialName: 'credential',
                userName: 'admin',
                password: 'changeme'
        ]

        setup:
        createDefaultConfiguration(config_name)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)

        then:
        assert runProcedureJob.getStatus() == "error"

        cleanup:
        deleteConfiguration("EC-JBoss", config_name)
    }
}