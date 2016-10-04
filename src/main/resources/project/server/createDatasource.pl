# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;

$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );
    my $params = $jboss->get_params_as_hashref(qw/
        serverconfig
        profile
        connectionURL
        driverClass
        jndiName
        driverName
        dsCredential
        enabled
        application_name
    /);

    # See example with options
    # https://docs.jboss.org/author/display/AS71/CLI+Recipes#CLIRecipes-AddaNewDatasource

    my $command = 'data-source add';
    $command .= qq| --connection-url=$params->{connectionURL} |;
    $command .= qq| --datasource-class=$params->{driverClass} |;
    $command .= qq| --jndi-name=$params->{jndiName} |;
    $command .= qq| --driver-name=$params->{driverName} |;
    $command .= qq| --name=$params->{application_name} |;

    # profile for domain mode
    if ($params->{profile}) {
    	$command .= qq| --profile=$params->{profile} |;
	}

    if ($params->{enabled}) {
        $command .= ' --enabled=true ';
    }
    else {
        $command .= ' --enabled=false ';
    }

    if ($params->{dsCredential}) {
        my $xpath = $ec->getFullCredential('dsCredential');
        my $userName = $xpath->findvalue("//userName");
        my $password = $xpath->findvalue("//password");
        
        if ($userName) {
            $command .= qq| --user-name=$userName |;
        }

        if ($password) {
            $command .= qq| --password=$password |;
        }
    }
    
    my %result = $jboss->run_command($command);

    $jboss->process_response(%result);
}
