# Data that drives the create step picker registration for this plugin.
my %checkServerStatus = (
    label       => "JBoss - Check Server Status",
    procedure   => "CheckServerStatus",
    description => "Check the status of the JBoss server",
    category    => "Application Server"
);
my %deployApp = (
    label       => "JBoss - Deploy App",
    procedure   => "DeployApp",
    description => "Deploy an application into JBoss server",
    category    => "Application Server"
);
my %deployApplication = (
    label       => "JBoss - Deploy Application",
    procedure   => "DeployApplication",
    description => "Deploy an application (mainly WAR or EAR) from the specified source (usually from filepath) to standalone server (for Standalone JBoss) or to content repository and specified server groups (for Domain JBoss)",
    category    => "Application Server"
);
my %shutdownStandaloneServer = (
    label       => "JBoss - Shutdown Standalone Server",
    procedure   => "ShutdownStandaloneServer",
    description => "Shutdown a running standalone JBoss server",
    category    => "Application Server"
);
my %startDomainServer = (
    label       => "JBoss - Start Domain Server",
    procedure   => "StartDomainServer",
    description => "Start a JBoss domain server",
    category    => "Application Server"
);
my %startStandaloneServer = (
    label       => "JBoss - Start Standalone Server",
    procedure   => "StartStandaloneServer",
    description => "Starts a JBoss server in standalone mode",
    category    => "Application Server"
);
my %undeployApp = (
    label       => "JBoss - Undeploy Application",
    procedure   => "UndeployApp",
    description => "Undeploy an application in JBoss server",
    category    => "Application Server"
);
my %checkDeployStatus = (
    label       => "JBoss - Check Deploy Status",
    procedure   => "CheckDeployStatus",
    description => "Checks the status of a given URL",
    category    => "Application Server"
);
my %enableDeploy = (
    label       => "JBoss - Enable Deployment",
    procedure   => "EnableDeploy",
    description => "Enables an already existing deployment. Implemented only for the Standalone Mode",
    category    => "Application Server"
);
my %disableDeploy = (
    label       => "JBoss - Disable Deployment",
    procedure   => "DisableDeploy",
    description => "Disables a deployment. Implemented only for the Standalone Mode",
    category    => "Application Server"
);
my %createDatasource = (
    label       => "JBoss - Create Datasource",
    procedure   => "CreateDatasource",
    description => "Creates a datasource in JBoss",
    category    => "Application Server"
);
my %deleteDatasource = (
    label       => "JBoss - Delete Datasource",
    procedure   => "DeleteDatasource",
    description => "Deletes a datasource in JBoss",
    category    => "Application Server"
);
my %runCustomCommand = (
    label       => "JBoss - Run Custom Command",
    procedure   => "RunCustomCommand",
    description => "Runs custom command",
    category    => "Application Server"
);

my %startServers = (
    label       => "JBoss - Start Servers",
    procedure   => "StartServers",
    description => "Starts group of servers",
    category    => "Application Server"
);

my %stopServers = (
    label       => "JBoss - Stop Servers",
    procedure   => "StopServers",
    description => "Stops group of servers",
    category    => "Application Server"
);

my %checkServerGroupStatus = (
    label       => "JBoss - Check Server Group Status",
    procedure   => "CheckServerGroupStatus",
    description => "Checks server group status",
    category    => "Application Server"
);

my %checkHostControllerStatus = (
    label       => "JBoss - Check HostController Status",
    procedure   => "CheckHostControllerStatus",
    description => "Checks HostController Status",
    category    => "Application Server"
);

my %createOrUpdateJMSQueue = (
    label       => "JBoss - Create or Update JMS Queue",
    procedure   => "CreateOrUpdateJMSQueue",
    description => "Create or update JMS queue",
    category    => "Application Server"
);

my %createOrUpdateJMSTopic = (
    label       => "JBoss - Create or Update JMS Topic",
    procedure   => "CreateOrUpdateJMSTopic",
    description => "Create or update JMS topic",
    category    => "Application Server"
);

my %removeJMSQueue = (
    label       => "JBoss - Remove JMS Queue",
    procedure   => "RemoveJMSQueue",
    description => "Remove JMS queue",
    category    => "Application Server"
);

my %removeJMSTopic = (
    label       => "JBoss - Remove JMS Topic",
    procedure   => "RemoveJMSTopic",
    description => "Remove JMS topic",
    category    => "Application Server"
);

my %createOrUpdateXADataSource = (
    label       => "JBoss - Create or Update XA Data Source",
    procedure   => "CreateOrUpdateXADataSource",
    description => "Create or update XA data source",
    category    => "Application Server"
);

my %removeXADataSource = (
    label       => "JBoss - Remove XA Data Source",
    procedure   => "RemoveXADataSource",
    description => "Remove XA data source",
    category    => "Application Server"
);

$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Check Server Status");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Deploy App");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Shutdown Standalone Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Start Domain Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Start Standalone Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Undeploy App");

$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Deploy Application");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Undeploy Application");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Check Deploy Status");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Enable Deploy");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Enable Deployment");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Disable Deployment");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Create Datasource");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Delete Datasource");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Run Custom Command");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Run Start Servers");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Run Stop Servers");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Check Server Group Status");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - CheckHostControllerStatus");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Create or Update JMS Queue");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Create or Update JMS Topic");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Remove JMS Queue");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Remove JMS Topic");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Create or Update XA Data Source");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Remove XA Data Source");


@::createStepPickerSteps = (
    \%checkServerStatus,
    \%deployApp,
    \%deployApplication,
    \%shutdownStandaloneServer,
    \%startDomainServer,
    \%startStandaloneServer,
    \%undeployApp,
    \%checkDeployStatus,
    \%enableDeploy,
    \%disableDeploy,
    \%createDatasource,
    \%deleteDatasource,
    \%runCustomCommand,
    \%startServers,
    \%stopServers,
    \%checkServerGroupStatus,
    \%checkHostControllerStatus,
    \%createOrUpdateJMSQueue,
    \%createOrUpdateJMSTopic,
    \%removeJMSQueue,
    \%removeJMSTopic,
    \%createOrUpdateXADataSource,
    \%removeXADataSource
);


if ($upgradeAction eq "upgrade") {
    my $query = $commander->newBatch();
    my $newcfg = $query->getProperty(
        "/plugins/$pluginName/project/jboss_cfgs");
    my $oldcfgs = $query->getProperty(
        "/plugins/$otherPluginName/project/jboss_cfgs");
	my $creds = $query->getCredentials(
        "\$[/plugins/$otherPluginName]");

	local $self->{abortOnError} = 0;
    $query->submit();

    # if new plugin does not already have cfgs
    if ($query->findvalue($newcfg,"code") eq "NoSuchProperty") {
        # if old cfg has some cfgs to copy
        if ($query->findvalue($oldcfgs,"code") ne "NoSuchProperty") {
            $batch->clone({
                path => "/plugins/$otherPluginName/project/jboss_cfgs",
                cloneName => "/plugins/$pluginName/project/jboss_cfgs"
            });
        }
    }

    # Copy configuration credentials and attach them to the appropriate steps
    my $nodes = $query->find($creds);
    if ($nodes) {
        my @nodes = $nodes->findnodes('credential/credentialName');
        for (@nodes) {

            my $cred = $_->string_value;

            # Clone the credential
            $batch->clone({
                path => "/plugins/$otherPluginName/project/credentials/$cred",
                cloneName => "/plugins/$pluginName/project/credentials/$cred"
            });

            # Make sure the credential has an ACL entry for the new project principal
            my $xpath = $commander->getAclEntry("user", "project: $pluginName", {
                projectName => $otherPluginName,
                credentialName => $cred
            });
            if ($xpath->findvalue("//code") eq "NoSuchAclEntry") {
                $batch->deleteAclEntry("user", "project: $otherPluginName", {
                    projectName => $pluginName,
                    credentialName => $cred
                });
                $batch->createAclEntry("user", "project: $pluginName", {
                    projectName => $pluginName,
                    credentialName => $cred,
                    readPrivilege => 'allow',
                    modifyPrivilege => 'allow',
                    executePrivilege => 'allow',
                    changePermissionsPrivilege => 'allow'
                });
            }

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'StartStandaloneServer',
                stepName => 'StartStandaloneServer'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'StartDomainServer',
                stepName => 'StartDomainServer'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckServerStatus',
                stepName => 'CheckServerStatus'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'ShutdownStandaloneServer',
                stepName => 'ShutdownInstance'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'RunCustomCommand',
                stepName => 'RunCustomCommand'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'StartServers',
                stepName => 'StartServers'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'StopServers',
                stepName => 'StopServers'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'DeployApp',
                stepName => 'DeployApp'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'DeployApplication',
                    stepName => 'DeployApplication'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'UndeployApp',
                stepName => 'UndeployApp'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CreateDatasource',
                stepName => 'CreateDatasource'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'DeleteDatasource',
                stepName => 'DeleteDatasource'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'DisableDeploy',
                stepName => 'DisableDeploy'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'EnableDeploy',
                stepName => 'EnableDeploy'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckServerStatus',
                stepName => 'CheckServerStatus'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckDeployStatus',
                stepName => 'CheckDeployStatus'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckServerGroupStatus',
                stepName => 'CheckServerGroupStatus'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckHostControllerStatus',
                stepName => 'CheckHostControllerStatus'
            });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'CreateOrUpdateJMSQueue',
                    stepName => 'CreateOrUpdateJMSQueue'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'CreateOrUpdateJMSTopic',
                    stepName => 'CreateOrUpdateJMSTopic'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'RemoveJMSQueue',
                    stepName => 'RemoveJMSQueue'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'RemoveJMSTopic',
                    stepName => 'RemoveJMSTopic'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'CreateOrUpdateXADataSource',
                    stepName => 'CreateOrUpdateXADataSource'
                });

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => 'RemoveXADataSource',
                    stepName => 'RemoveXADataSource'
                });
        }
    }
}

