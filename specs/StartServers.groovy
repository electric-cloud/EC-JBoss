import com.electriccloud.spec.SpockTestSupport
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class StartServers extends PluginTestHelper {

    @Shared
    def projectName = 'EC-JBoss Specs StartServers Project'
    @Shared
    def testProcedureName = 'Start Servers Procedure'
    @Shared
    def defaultConfigName = 'specConfig'
    @Shared
    def defaultCliPath = '/opt/jboss/bin/jboss-cli.sh'
    @Shared
    def defaultWaitTime = '300'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        createConfiguration('specConfig')
        logger.info("Hello World!")
        logger.info("doSetupSpec")
        dslFile 'dsl/StartServers/StartServers.dsl', [projName: projectName]
    }

    def doCleanupSpec() {
        dsl "deleteProject(projectName: '$projectName')"
    }

    def runProcedure(def parameters) {
        def code = """
                runProcedure(
                    projectName: '$projectName',
                    procedureName: '$testProcedureName',
                    actualParameter: [
                        configName:      '$parameters.configName',
                        cliPath:         '$parameters.cliPath',
                        serverGroup:     '$parameters.serverGroup',
                        waitTime:        '$parameters.waitTime'
                    ]
                )
        """
        return dsl(code)

    }

    @Unroll
    def "start server group which is already started"() {
        when: 'run StartServers procedure for the server group which is already started'
        def runParams = [
                configName : testConfigName,
                cliPath    : testCliPath,
                serverGroup: testServerGroup,
                waitTime   : 300
        ]
        def result = runProcedure(runParams)
        then: 'wait until procedure finishes'
        assert result?.jobId
        waitUntil {
            jobCompleted result.jobId
        }
//        def logs = getJobProperty("/myJob/debug_logs", result.jobId)
//        SpockTestSupport.logger.debug(logs)
        assert jobStatus(result.jobId).outcome == expectedOutcome
        def properties = getJobProperties(result.jobId)
        where:
        testNumber | testConfigName    | testCliPath    | testServerGroup      | testWaitTime    | expectedOutcome
        '1'        | defaultConfigName | defaultCliPath | 'main-server-group'  | defaultWaitTime | 'success'
        '2'        | defaultConfigName | defaultCliPath | 'other-server-group' | defaultWaitTime | 'success'
    }

}