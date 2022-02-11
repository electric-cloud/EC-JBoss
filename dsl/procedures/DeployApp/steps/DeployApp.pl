# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;
use URI;
use File::Basename;

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

    my $source_is_url = 0;
    # e.g. the following format we accept the following:
    # '--url=https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war'
    if ($params->{warphysicalpath} =~ /^--url=/) {
        $jboss->log_info("Source with deployment is URL (such option available for EAP 7 and later versions): '$params->{warphysicalpath}'");
        $source_is_url = 1;
    }
    else {
        $jboss->log_info("Source with deployment is filepath: '$params->{warphysicalpath}'");
    }

    my $is_domain = 0;
    my $launch_type = $jboss->get_launch_type();
    $is_domain = 1 if $launch_type eq 'domain';

    if (!$jboss->{dryrun} && !$source_is_url && !-e $params->{warphysicalpath}) {
        $jboss->bail_out("File '$params->{warphysicalpath}' doesn't exists");
    }

    ######################
    # generate jboss cli command
    ######################

    my $command = qq/deploy /;

    if ($source_is_url) {
        $command .= qq/ $params->{warphysicalpath} /;
    }
    else {
        $command .= qq/ "$params->{warphysicalpath}" /;
    }

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
            $command .= qq/ --server-groups=$params->{assignservergroups} /;
        }
    }

    if ($params->{additional_options}) {
        $params->{additional_options} = $jboss->escape_string($params->{additional_options});
        $command .= ' ' . $params->{additional_options} . ' ';
    }

    ######################
    # generate possible job step summary in case of successful completion
    ######################

    # expected source - filepath or url (url without '--url=' anchor)
    my $expected_source = $params->{warphysicalpath};
    if ($source_is_url) {
        $expected_source =~ s/^--url=//;
    }
    # expected name of the deployment
    my $expected_appname;
    if ($params->{appname}) {
        $expected_appname = $params->{appname};
    }
    elsif ($source_is_url) {
        # retrieve filename for url
        $expected_appname = (URI->new($expected_source)->path_segments)[-1];
    }
    else {
        # retrieve filename for filepath
        $expected_appname = fileparse($expected_source);
    }
    # job step summary for in case of successful completion
    my $job_step_summary_for_success_completion = "Application '$expected_appname' has been successfully deployed from '$expected_source'";

    ######################
    # run jboss cli command and process response
    ######################
    my %result = $jboss->run_command($command);

    # job step summary to be updated by this message within process_response only in case if this process_response will decide to that response is success..
    $jboss->{success_message} = $job_step_summary_for_success_completion;

    if ($result{stdout}) {
        $jboss->out("Command output: $result{stdout}");
    }

    $jboss->process_response(%result);
};
