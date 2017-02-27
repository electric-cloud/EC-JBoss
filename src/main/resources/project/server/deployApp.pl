# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;

$|=1;

main();


# my $property_path = '/plugins/$pk/project/dryrun';
sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        scriptphysicalpath
        warphysicalpath
        appname
        runtimename
        force
        assignservergroups
        assignallservergroups
        additional_options
    /);

    $jboss->log_debug("Params: " . Dumper $params);
    my $is_domain = 0;
    my $launch_type = $jboss->get_launch_type();
    $is_domain = 1 if $launch_type eq 'domain';
    if (!$jboss->{dryrun} && !-e $params->{warphysicalpath}) {
        croak "File: $params->{warphysicalpath} doesn't exists";
    }

    my $command = qq/deploy $params->{warphysicalpath} /;

    if ($params->{appname}) {
        $command .= qq/ --name=$params->{appname} /;
    }

    if ($params->{runtimename}) {
        $command .= qq/ --runtime-name=$params->{runtimename} /;
    }

    if ($params->{force}) {
        $command .= ' --force ';
    }

    if ($is_domain && !$params->{force}) {
        if ($params->{assignallservergroups}) {
            $command .= ' --all-server-groups ';
        }
        elsif ($params->{assignservergroups}) {
            $command .= qq/ --server-groups=$params->{assignservergroups}/;
        }
        else {
            $jboss->bail_out("When JBoss mode is domain checkbox 'Apply to all servers' should be checked or 'Server groups to apply' should be provided.");
        }
    }

    if ($params->{additional_options}) {
        $params->{additional_options} = $jboss->escape_string($params->{additional_options});
        $command .= ' ' . $params->{additional_options} . ' ';
    }

    my %result = $jboss->run_command($command);

    $jboss->{success_message} = sprintf 'Application %s (%s) has been successfully deployed.', $params->{appname}, $params->{warphysicalpath};
    if ($result{stdout}) {
        $jboss->out("Command output: $result{stdout}");
    }

    $jboss->process_response(%result);
};
