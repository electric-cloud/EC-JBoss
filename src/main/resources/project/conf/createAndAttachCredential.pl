##########################
# createAndAttachCredential.pl
##########################

use ElectricCommander;

use constant {
	SUCCESS => 0,
	ERROR   => 1,
};

my $ec = new ElectricCommander();
$ec->abortOnError(0);

my $credName = '$[/myJob/config]';
my $xpath = $ec->getFullCredential("credential");
my $userName = $xpath->findvalue("//userName");
my $password = $xpath->findvalue("//password");

# Create credential
my $projName = "@PLUGIN_KEY@-@PLUGIN_VERSION@";

$ec->deleteCredential($projName, $credName);
$xpath = $ec->createCredential($projName, $credName, $userName, $password);
my $errors = $ec->checkAllErrors($xpath);

# Give config the credential's real name
my $configPath = "/projects/$projName/jboss_cfgs/$credName";
$xpath = $ec->setProperty($configPath . "/credential", $credName);
$errors .= $ec->checkAllErrors($xpath);

# Give job launcher full permissions on the credential
my $user = '$[/myJob/launchedByUser]';
$xpath = $ec->createAclEntry("user", $user,
    {projectName => $projName,
     credentialName => $credName,
     readPrivilege => allow,
     modifyPrivilege => allow,
     executePrivilege => allow,
     changePermissionsPrivilege => allow});
$errors .= $ec->checkAllErrors($xpath);

# Attach credential to steps that will need it
$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StartStandaloneServer',
     stepName => 'StartStandaloneServer'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StartDomainServer',
     stepName => 'StartDomainServer'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'ShutdownStandaloneServer',
     stepName => 'ShutdownInstance'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CheckServerStatus',
     stepName => 'CheckServerStatus'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'RunCustomCommand',
        stepName => 'RunCustomCommand'
    }
);
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'StartServers',
        stepName => 'StartServers'
    }
);
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'StopServers',
        stepName => 'StopServers'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'DeployApp',
        stepName => 'DeployApp'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'UndeployApp',
        stepName => 'UndeployApp'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'CreateDatasource',
        stepName => 'CreateDatasource'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'DeleteDatasource',
        stepName => 'DeleteDatasource'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'DisableDeploy',
        stepName => 'DisableDeploy'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'EnableDeploy',
        stepName => 'EnableDeploy'
    }
);
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName, {
        procedureName => 'CheckDeployStatus',
        stepName => 'CheckDeployStatus'
    }
);
$errors .= $ec->checkAllErrors($xpath);


if ($errors ne '') {
    
    # Cleanup the partially created configuration we just created
    $ec->deleteProperty($configPath);
    $ec->deleteCredential($projName, $credName);
    my $errMsg = 'Error creating configuration credential: ' . $errors;
    $ec->setProperty("/myJob/configError", $errMsg);
    print $errMsg;
    exit ERROR;
    
}
