# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

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
        datasource_name
        profile
    /);

    my $command = 'data-source remove ';
    $command .= qq| --name=$params->{datasource_name} |;

    if ($params->{profile}) {
        $command .= qq| --profile=$params->{profile} |;
    }

    my %result = $jboss->run_command($command);

    $jboss->process_response(%result);
}
