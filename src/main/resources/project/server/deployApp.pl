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
    # e.g. the following format we accept: '--url=https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war'
    if ( $params->{warphysicalpath} =~ /^--url=/ ) {
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
        else {
            $jboss->bail_out("When JBoss mode is domain checkbox 'Apply to all servers' should be checked or 'Server groups to apply' should be provided.");
        }
    }

    if ($params->{additional_options}) {
        $params->{additional_options} = $jboss->escape_string($params->{additional_options});
        $command .= ' ' . $params->{additional_options} . ' ';
    }

    my %result = $jboss->run_command($command);

    # expected source
    my $expected_source = $params->{warphysicalpath};
    if ($source_is_url) {
        $expected_source =~ s/^(--url=)(.*)$/$2/;
    }

    # expected name of the deployment
    my $expected_appname;
    if ($params->{appname}) {
        $expected_appname = $params->{appname};
    }
    elsif ($source_is_url) {
        $expected_appname = (URI->new($expected_source)->path_segments)[-1];
    }
    else {
        $expected_appname = fileparse($expected_source);
    }

    $jboss->{success_message} = "Application '$expected_appname' has been successfully deployed from '$expected_source'";
    if ($result{stdout}) {
        $jboss->out("Command output: $result{stdout}");
    }

    $jboss->process_response(%result);
};
