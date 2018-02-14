$[/myProject/procedure_helpers/preamble]

use EC::JBoss;

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
        scriptphysicalpath
        appname
        keepcontent
        servergroups
        allrelevantservergroups
        additional_options
    /);

    my $command = qq/undeploy $params->{appname} /;

    if ($params->{keepcontent}) {
        $command .= ' --keep-content ';
    }
    if ($params->{allrelevantservergroups}) {
        $command .= ' --all-relevant-server-groups ';
    }
    elsif ($params->{servergroups}) {
        $command .= qq/ --server-groups=$params->{servergroups} /;
    }

    if ($params->{additional_options}) {
        $params->{additional_options} = $jboss->escape_string($params->{additional_options});
        $command .= ' ' . $params->{additional_options} . ' ';
    }
    my %result = $jboss->run_command($command);

    $jboss->{success_message} = sprintf 'Application %s has been successfully undeployed.', $params->{appname};
    if ($result{stdout}) {
        $jboss->out("Command output: $result{stdout}");
    }

    $jboss->process_response(%result);
}
