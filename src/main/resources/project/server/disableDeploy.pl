$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

main ();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );
    my $params = $jboss->get_params_as_hashref(qw/
        appname
        assignservergroups
    /);

    my $command = "undeploy --name=$params->{appname} --keep-content";
    my $launch_type = $jboss->get_launch_type();
    if ($launch_type eq 'domain' && !$params->{assignservergroups}) {
        $jboss->bail_out('When JBoss server is launched as domain, "Server groups" parameter is mandatory');
    }

    if ($launch_type eq 'domain') {
        $command .= " --server-groups=$params->{assignservergroups}";
    }
    $jboss->{success_message} = "Application $params->{appname} has been successfully disabled.";
    $jboss->process_response($jboss->run_command($command));
}
