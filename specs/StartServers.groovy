import com.electriccloud.spec.SpockTestSupport
import spock.lang.*

@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class StartServers extends PluginTestHelper {

    @Shared
    def projectName = 'EC-JBoss Specs StartServers Project'
    @Shared
    def resourceName = 'EC-JBoss Resource'
    @Shared
    def testProcedureName = 'Start Servers Procedure'
    @Shared
    def defaultConfigName = 'specConfig'
    @Shared
    def defaultCliPath = ''
    @Shared
    def defaultWaitTime = '300'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-JBoss/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        createConfiguration(defaultConfigName)
        createResourceDefault(resourceName)
        logger.info("Hello World! doSetupSpec")
        dslFile 'dsl/StartServers/StartServers.dsl', [projName: projectName, resName: resourceName]
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
//        deleteProject(projectName)
//        deleteResource(resourceName);
    }

    def runProcedure(def parameters) {
        def prcedureDsl = """
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
        def result = runProcedureDsl prcedureDsl
        return result
    }

    @Unroll
    def "start server group which is stopped"() {
        given:
        def testServerGroup = 'other-server-group'

        when:
        def runParams = [
                configName : defaultConfigName,
                cliPath    : defaultCliPath,
                serverGroup: testServerGroup,
                waitTime   : defaultWaitTime
        ]
        def result = runProcedure(runParams)

        then:
        assert result.outcome == 'success'
    }

    @Unroll
    def "start server group which is already started"() {
        given:
        def testServerGroup = 'main-server-group'

        when:
        def runParams = [
                configName : defaultConfigName,
                cliPath    : defaultCliPath,
                serverGroup: testServerGroup,
                waitTime   : defaultWaitTime
        ]
        def result = runProcedure(runParams)

        then:
        assert result.outcome == 'warning'
    }

    @Unroll
    def "negative: start not existing server group"() {
        given:
        def testServerGroup = 'not-existing-server-group'

        when:
        def runParams = [
                configName : defaultConfigName,
                cliPath    : defaultCliPath,
                serverGroup: testServerGroup,
                waitTime   : defaultWaitTime
        ]
        def result = runProcedure(runParams)

        then:
        assert result.outcome == 'error'
    }

    @Unroll
    def "negative: start empty server group"() {
        given:
        //todo:
        def testServerGroup = 'empty-server-group'

        when:
        def runParams = [
                configName : defaultConfigName,
                cliPath    : defaultCliPath,
                serverGroup: testServerGroup,
                waitTime   : defaultWaitTime
        ]
        def result = runProcedure(runParams)

        then:
        assert result.outcome == 'error'
    }

//    @Unroll
//    def "negative: start server group with low wait time (timeout)"() {
//        given:
//        def testServerGroup = 'other-server-group'
//        def testWaitTime = '1';
//
//        when:
//        def runParams = [
//                configName : defaultConfigName,
//                cliPath    : defaultCliPath,
//                serverGroup: testServerGroup,
//                waitTime   : testWaitTime
//        ]
//        def result = runProcedure(runParams)
//
//        then:
//        assert result.outcome == 'error'
//    }

}