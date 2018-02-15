# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        plugin_name  => $PLUGIN_NAME,
        plugin_key   => $PLUGIN_KEY,
    );

    run_procedure($jboss);

    $jboss->store_commands_history_in_property();
}

sub run_procedure {
    my $jboss = shift;

    my $params = $jboss->get_params_as_hashref(qw/
        queueName
        jndiNames
        profile
        durable
        messageSelector
        additionalOptions
        /);

    ########
    # check if jms queue with specified name exists and if jndi names match
    ########

    my $response = $jboss->run_command(':read-attribute(name=launch-type)');

    if ($response->{code}) {
        $jboss->error();
        $jboss->set_property(summary => $response->{code});
        return;
    }








    $jboss->bail_out("Could not")


    my $launch_type = $jboss->get_launch_type();
    $jboss->bail_out("Cannot retroUnknown JBoss Launch Type")

    $is_domain = 1 if $launch_type eq 'domain';



}
