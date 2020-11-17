use JSON;
my $stepsWithCredentials = getStepsWithCredentials();

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

my %stopDomain = (
    label       => "JBoss - Stop Domain",
    procedure   => "StopDomain",
    description => "Stop all servers within domain with specified timeout and if 'All Controllers Shutdown' option is chosen then perform shutdown of all controllers one by one with specified timeout (shutdown of a master host controller to be performed on final stage)",
    category    => "Application Server"
);

my %getEnvInfo = (
    label       => "JBoss - Get Environment Information",
    procedure   => "GetEnvInfo",
    description => "Get environment information",
    category    => "Application Server"
);

my %createOrUpdateDataSource = (
    label       => "JBoss - Create or Update Data Source",
    procedure   => "CreateOrUpdateDataSource",
    description => "Create or update data source",
    category    => "Application Server"
);

my %startHostController = (
    label       => "JBoss - Start Host Controller",
    procedure   => "StartHostController",
    description => "Start a master or slave host controller for JBoss Domain",
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
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Stop Domain");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Get Environment Information");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Create or Update Data Source");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/JBoss - Start Host Controller");


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
    \%removeXADataSource,
    \%stopDomain,
    \%getEnvInfo,
    \%createOrUpdateDataSource,
    \%startHostController
);

my @formalOutputParameters = (
    {
        formalOutputParameterName => 'servergroupstatus',
        procedureName             => 'CheckServerGroupStatus'
    }
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
            for my $step (@$stepsWithCredentials) {
                $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                    procedureName => $step->{procedureName},
                    stepName => $step->{stepName}
                });
            }
        }
    }
    reattachExternalCredentials($otherPluginName);
}

if ($promoteAction eq 'promote') {

    ## Check if agent supports formalOutputParameters API,
    if (exists $ElectricCommander::Arguments{getFormalOutputParameters}) {
        my $versions = $commander->getVersions();

        if (my $version = $versions->findvalue('//version')) {
            require ElectricCommander::Util;
            ElectricCommander::Util->import('compareMinor');

            if (compareMinor($version, '8.3') >= 0) {
                checkAndSetOutputParameters(@formalOutputParameters);
            }
        }
    }
}

sub checkAndSetOutputParameters {
    my (@parameters) = @_;

    # Form flatten unique list of procedureNames
    # and get all parameters for defined procedures
    my $query = $commander->newBatch();
    my %subs = ();
    foreach my $param (@parameters) {
        my $proc_name = $param->{procedureName};
        $subs{$proc_name} = 1;
    };

    foreach (keys %subs) {
        $subs{$_} = $query->getFormalOutputParameters($otherPluginName, {
                procedureName => $_
            });
    }
    $query->submit();

    my @params_to_create = ();
    foreach my $proc_name (keys %subs) {
        my $response_for_params = $query->find($proc_name);

        push @params_to_create, checkMissingOutputParameters( \@parameters, $response_for_params );
    }

    createMissingOutputParameters(@params_to_create);
}

sub checkMissingOutputParameters {
    my ($parameters, $response) = @_;
    my @parameters = @{$parameters};

    # This is list of keys to build unique parameter's indices
    my @key_parts = ('formalOutputParameterName', 'procedureName');
    my @params_to_create = ();

    my %parsed_parameters = ();
    if ($response) {
        my @defined_params = ($response->findnodes('formalOutputParameter'));

        if (@defined_params) {
            for my $param (@defined_params) {
                my $key = join('_', map {
                        $param->find($_)->string_value()
                    } @key_parts
                );

                # Setting a flag parameter that parameter is already created
                $parsed_parameters{$key} = 1;
            }
        }
    }

    foreach my $param (@parameters) {
        my $key = join('_', map {$param->{$_} || ''} @key_parts);

        if (!exists $parsed_parameters{$key}) {
            push(@params_to_create, [
                    $pluginName,
                    $param->{formalOutputParameterName},
                    {
                        procedureName => $param->{procedureName}
                    }
                ]);
        }
    }

    return @params_to_create;
}

sub createMissingOutputParameters {
    my (@params_to_create) = @_;

    my @responses = ();
    if (@params_to_create) {
        my $create_batch = $commander->newBatch();
        push @responses, $create_batch->createFormalOutputParameter(@$_) foreach (@params_to_create);
        $create_batch->submit();
    }
    # print Dumper \@responses
    return 1;
}


sub reattachExternalCredentials {
    my ($otherPluginName) = @_;

    my $configName = getConfigLocation($otherPluginName);
    my $configsPath = "/plugins/$otherPluginName/project/$configName";

    my $xp = $commander->getProperty($configsPath);

    my $id = $xp->findvalue('//propertySheetId')->string_value();
    my $props = $commander->getProperties({propertySheetId => $id});
    for my $node ($props->findnodes('//property/propertySheetId')) {
        my $configPropertySheetId = $node->string_value();
        my $config = $commander->getProperties({propertySheetId => $configPropertySheetId});

        # iterate through props to get credentials.
        for my $configRow ($config->findnodes('//property')) {
            my $propName = $configRow->findvalue('propertyName')->string_value();
            my $propValue = $configRow->findvalue('value')->string_value();
            # print "Name $propName, value: $propValue\n";
            if ($propName =~ m/credential$/s && $propValue =~ m|^\/|s) {
                for my $step (@$stepsWithCredentials) {
                    $batch->attachCredential({
                        projectName    => $pluginName,
                        procedureName  => $step->{procedureName},
                        stepName       => $step->{stepName},
                        credentialName => $propValue,
                    });
                    #    debug "Attached credential to $step->{stepName}";
                }
                print "Reattaching $propName with val: $propValue\n";
            }
        }
        # exit 0;
    }
}

sub getConfigLocation {
    my ($otherPluginName) = @_;

    my $configName = 'Jenkins_cfgs';
    return $configName;
}

sub getStepsWithCredentials {
    my $retval = [];
    eval {
        my $pluginName = '@PLUGIN_NAME@';
        my $stepsJson = $commander->getProperty("/projects/$pluginName/procedures/CreateConfiguration/ec_stepsWithAttachedCredentials")->findvalue('//value')->string_value;
        $retval = decode_json($stepsJson);
    };
    return $retval;
}

