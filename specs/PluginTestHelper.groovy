import spock.lang.*
import com.electriccloud.spec.*

class PluginTestHelper extends PluginSpockTestSupport {

    def createConfiguration(configName, props = [:]) {
        def username = System.getenv('JBOSS_USERNAME') ?: 'admin'
        def password = System.getenv('JBOSS_PASSWORD') ?: 'changeme'
        def jboss_url = System.getenv('JBOSS_CONTROLLER_URL') ?: 'jboss:9990'

        createPluginConfiguration(
                'EC-JBoss',
                configName,
                [jboss_url: jboss_url,
                 scriptphysicalpath: '',
                 java_opts: '',
                 log_level: 'INFO'],
                username,
                password,
                props
        )
    }

    def createResourceDefault(String resourceName) {
        def hostName = System.getenv('RESOURCE_HOSTNAME') ?: 'jboss'
        def port = System.getenv('RESOURCE_PORT') ?: 7808;

        createResource(resourceName, hostName, port);
    }

    def createResource(String resourceName, String hostName, Integer port) {
        dsl """
            createResource(
                resourceName: '$resourceName',
                hostName: '$hostName',
                port: $port,
            )
        """
    }

    def deleteResource(String resourceName) {
        dsl """
            deleteResource(resourceName: '$resourceName')
        """
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

}
