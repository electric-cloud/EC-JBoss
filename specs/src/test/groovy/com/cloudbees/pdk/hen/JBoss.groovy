package com.cloudbees.pdk.hen

import com.cloudbees.pdk.hen.procedures.*
import com.cloudbees.pdk.hen.Plugin

import static com.cloudbees.pdk.hen.Utils.env

class JBoss extends Plugin {

    static JBoss create() {
        JBoss plugin = new JBoss(name: 'EC-JBoss')
        plugin.configure(plugin.config)
        return plugin
    }
    static JBoss createWithoutConfig() {
        JBoss plugin = new JBoss(name: 'EC-JBoss')
        return plugin
    }

    //user-defined after boilerplate was generated, default parameters setup
    JBossConfig config = JBossConfig
        .create(this)
        //.parameter(value) add parameters here


    CheckDeployStatus checkDeployStatus = CheckDeployStatus.create(this)

    CheckHostControllerStatus checkHostControllerStatus = CheckHostControllerStatus.create(this)

    CheckServerGroupStatus checkServerGroupStatus = CheckServerGroupStatus.create(this)

    CheckServerStatus checkServerStatus = CheckServerStatus.create(this)

    CreateDatasource createDatasource = CreateDatasource.create(this)

    CreateOrUpdateDataSource createOrUpdateDataSource = CreateOrUpdateDataSource.create(this)

    CreateOrUpdateJMSQueue createOrUpdateJMSQueue = CreateOrUpdateJMSQueue.create(this)

    CreateOrUpdateJMSTopic createOrUpdateJMSTopic = CreateOrUpdateJMSTopic.create(this)

    CreateOrUpdateXADataSource createOrUpdateXADataSource = CreateOrUpdateXADataSource.create(this)

    DeleteDatasource deleteDatasource = DeleteDatasource.create(this)

    DeployApp deployApp = DeployApp.create(this)

    DeployApplication deployApplication = DeployApplication.create(this)

    DisableDeploy disableDeploy = DisableDeploy.create(this)

    EditConfiguration editConfiguration = EditConfiguration.create(this)

    EnableDeploy enableDeploy = EnableDeploy.create(this)

    GetEnvInfo getEnvInfo = GetEnvInfo.create(this)

    RemoveJMSQueue removeJMSQueue = RemoveJMSQueue.create(this)

    RemoveJMSTopic removeJMSTopic = RemoveJMSTopic.create(this)

    RemoveXADataSource removeXADataSource = RemoveXADataSource.create(this)

    RunCustomCommand runCustomCommand = RunCustomCommand.create(this)

    ShutdownStandaloneServer shutdownStandaloneServer = ShutdownStandaloneServer.create(this)

    StartDomainServer startDomainServer = StartDomainServer.create(this)

    StartHostController startHostController = StartHostController.create(this)

    StartServers startServers = StartServers.create(this)

    StartStandaloneServer startStandaloneServer = StartStandaloneServer.create(this)

    StopDomain stopDomain = StopDomain.create(this)

    StopServers stopServers = StopServers.create(this)

    TestConfiguration testConfiguration = TestConfiguration.create(this)

    UndeployApp undeployApp = UndeployApp.create(this)

}