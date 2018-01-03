import Utils.EnvPropertiesHelper
import spock.lang.*
import com.electriccloud.spec.*

class PluginTestHelper extends PluginSpockTestSupport {

    static def helperProjName = 'JBoss Helper Project'
    static def helperProcedure = 'RunCustomCommand'

    def createDefaultConfiguration(String configName, props = [:]) {
        String pluginName = "EC-JBoss"

        def username = EnvPropertiesHelper.getJbossUsername();
        def password = EnvPropertiesHelper.getJbossPassword();
        def jboss_url = EnvPropertiesHelper.getJbossControllerUrl();
        def jboss_cli_path = EnvPropertiesHelper.getJbossCliPath();
        def jboss_log_level = EnvPropertiesHelper.getJbossLogLevel();

        createPluginConfiguration(
                'EC-JBoss',
                configName,
                [jboss_url         : jboss_url,
                 scriptphysicalpath: jboss_cli_path,
                 java_opts         : '',
                 log_level         : jboss_log_level],
                username,
                password,
                props
        )
    }

    def createJBossResource() {
        def hostname = EnvPropertiesHelper.getResourceHostname()
        def port = EnvPropertiesHelper.getResourcePort()

        def resources = dsl "getResources()"
        logger.debug(objectToJson(resources))

        def resource = resources.resource.find {
            it.hostName == hostname && it.port == port
        }
        if (resource) {
            logger.debug("JBoss resource already exists")
            return resource.resourceName
        }
        logger.debug("Creating new JBoss resource")

        def result = dsl """
            createResource(
                resourceName: '${randomize("JBoss")}',
                hostName: '$hostname',
                port: '$port'
            )
        """

        logger.debug(objectToJson(result))
        def resName = result?.resource?.resourceName
        assert resName
        resName
    }

    def deleteProject(String projectName) {
        dsl """
            deleteProject(projectName: '$projectName')
        """
    }

    def redirectLogs(String parentProperty = '/myJob') {
        def propertyLogName = parentProperty + '/debug_logs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty",
                value: "$propertyLogName"
            )
        """
        return propertyLogName
    }

    def redirectLogsToPipeline() {
        def propertyName = '/myPipelineRuntime/debugLogs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty",
                value: "$propertyName"
            )
        """
        propertyName
    }

    def getJobLogs(def jobId) {
        assert jobId
        def logs
        try {
            logs = getJobProperty("/myJob/debug_logs", jobId)
        } catch (Throwable e) {
            logs = "Possible exception in logs; check job"
        }
        logs
    }

    def getPipelineLogs(flowRuntimeId) {
        assert flowRuntimeId
        getPipelineProperty('/myPipelineRuntime/debugLogs', flowRuntimeId)
    }

    def flexibleIgnore(name = null) {
        def ignoreVar = System.getenv('SPEC_CASE')
        println ignoreVar
        if (!ignoreVar) {
            return false
        }
        if (!name) {
            return true
        }
        return name =~ /$ignoreVar/ ? false : true
    }

    def runProcedureDsl(dslString) {
        redirectLogs()
        assert dslString

        def result = dsl(dslString)
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        def logs = getJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def createHelperProject(resName, configName) {
        dslFile 'dsl/RunProcedure.dsl', [
                projName: helperProjName,
                resName : resName,
                procName: helperProcedure,
                params  : [
                        serverconfig      : configName,
                        scriptphysicalpath: '',
                        customCommand      : '',
                        propertyName      : '',
                        dumpFormat      : '',
                ]
        ]
    }

    def runCliCommand(String command, boolean catchJBossReply = false) {
        String propertyName = catchJBossReply ? "/myJob/RunCustomCommandResult" : ""

        def prcedureDsl = """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedure',
                actualParameter: [
                    customCommand: '''$command''',
                    propertyName: '''$propertyName''',
                    dumpFormat: 'propertySheet',
                ]
            )
        """
        def result = runProcedureDsl prcedureDsl
        assert result.outcome == 'success'

        if (catchJBossReply) {
            def props = getPropertiesRecursive(propertyName, result.jobId, { path, id ->
                def res = dsl """
                getProperties(path: '$path', jobId: '$id')
            """
                res?.propertySheet?.property
            })
            logger.debug("RunCustomCommandResult properties: " + objectToJson(props))
            result.jbossReply = props
            assert result.jbossReply.outcome == "success"
        }

        return result
    }

}
