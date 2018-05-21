import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Requires({ env.JBOSS_MODE == 'standalone' })
class CreateOrUpdateDataSourceStandalone extends PluginTestHelper {

    @Shared
    String procName = 'CreateOrUpdateDataSource'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String dataSourceConnectionCredentials = "dataSourceConnectionCredentials"
    @Shared
    def passwords = [
        defaultPassword: 'changeme',
        ]
    @Shared
    def userNames = [
        defaultUserName: 'admin',
        ]

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
			            additionalOptions: '',
			            connectionUrl: '',
			            dataSourceConnectionCredentials: '',
			            dataSourceName: '',
			            enabled: '',
			            jdbcDriverName: '',
			            jndiName: '',
			            profile: '',
			            serverconfig: '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createCredential(projectName, 'dataSourceConnectionCredentials', userNames.defaultUserName, passwords.defaultPassword)
        attachCredential(projectName, 'dataSourceConnectionCredentials', procName)
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        // deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }


    RunProcedureJob runProcedureUnderTest(def parameters, def credential) {
        return runProcedureDsl(projectName, procName, parameters, credential)
    }

    @Unroll
    def "CreateorUpdateXADataSource, MySQL, minimum parameters (C289546)"() {
        def runParams = [
            additionalOptions: additionalOptions,
            connectionUrl: url,
            dataSourceConnectionCredentials: creds,
            dataSourceName: dsName,
            enabled: enabled,
            jdbcDriverName: jdbcDriverName,
            jndiName: jndiName,
            profile: profile,
            serverconfig: configName

        ]
        def credential = [
                credentialName: creds,
                userName: userName,
                password: password
        ]
        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams, credential)
        // if (EnvPropertiesHelper.getVersion() == '6.1') {
                // reloadServer()
            // }

        // we expect "success" or "warning"
        // "success": if the server does not need reloading
        // "warning": if server needs reloading, and this case we throw text "reload-required" or
        // "restart-required" (it depends on jboss version).
        // def jobUpperStepSummary = runProcedureJob.getUpperStepSummary()
        def jobExpectedStatus = "success"
        // if (jobUpperStepSummary.contains("reload-required") || jobUpperStepSummary.contains("restart-required")) {
        //     jobExpectedStatus = "warning"
        // }
        then:
        assert runProcedureJob.getStatus() == jobExpectedStatus
        // assert jobUpperStepSummary =~ "XA data source '$xaDataSourceName' has been added successfully"
        // checkCreateXADataSource(xaDataSourceName, jndiName.mysql, jdbcDriverName, "1",
        //         defaultPassword, defaultUserName)

        // cleanup: 
        // reloadServer()
        // // remove XA datasource
        // removeXADatasource(xaDataSourceName)
        // reloadServer()
        // runCliCommandAnyResult(CliCommandsGeneratorHelper.deleteJDBCDriverInStandalone(jdbcDriverName))


        where: 'The following params will be: '
        testCaseId     | configName         | dsName     | jndiName     | jdbcDriverName | url   | creds          | userName| password | enabled | profile | additionalOptions 
        'C323725'      | defaultConfigName  | 'QA'       | 'java:/QA'   | 'h2'           | 'url' | 'dataSourceConnectionCredentials' | 'qa' 	| 'pass'   | '1'     | 'full'  | ''
    }

}