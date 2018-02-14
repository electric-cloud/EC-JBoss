# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

use EC::JBoss;
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

    my $is_6_0_0 = $jboss->is_6_0_0();
    $jboss->log_debug("Is JBoss 6.0.0: $is_6_0_0");

    # on JBoss 6.0.0 there is no --enabled option and command fails.
    # this workaround adds --enabled flag only when JBoss version is 6.0.0.

    if (!$is_6_0_0) {
        if ($params->{enabled}) {
            $command .= ' --enabled=true ';
        }
        else {
            $command .= ' --enabled=false ';
        }
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
    if ($is_6_0_0 && $params->{enabled}) {
        my $ds_profile = $params->{profile};
        $ds_profile ||= 'default';
        my $enable_command = sprintf 'data-source enable --name=%s --profile=%s', $params->{application_name}, $ds_profile;
        $jboss->out("Enabling datasource...");
        my %enable_res = $jboss->run_command($enable_command);
        unless ($enable_res{code}) {
            $jboss->out("Enabled.");
        }
        else {
            $jboss->bail_out("Failed to enable datasource.");
        }
    }
    $jboss->process_response(%result);
}
