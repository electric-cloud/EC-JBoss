# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;
use URI;
use File::Basename;
use EC::JBoss;
use EC::Procedures::Models::DeployAppProcedure;
use EC::Procedures::Factories::DeployAppFactory qw(get_current_deploy_app_model);
use EC::Procedures::Factories::JBossConfigurationFactory qw(get_jboss_configuration_model);
use EC::Utils::CommanderUtils qw(get_param_value);

$|=1;

main();


# my $property_path = '/plugins/$pk/project/dryrun';
sub main {

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );

    my $deploy_app_model = get_current_deploy_app_model();
    my $jboss_configuration_model = get_jboss_configuration_model($deploy_app_model->get_configuration_name());

#    run_procedure_deploy_app(deploy_app_procedure => $deploy_app_procedure,
#                             jboss_configuration => $jboss_configuration
#                            );




    my $application_content_source_path = $deploy_app_procedure->get_application_content_source_path();
    my $deployment_name = $deploy_app_procedure->get_deployment_name();
    my $runtime_name = $deploy_app_procedure->get_runtime_name();
    my $enabled_server_groups = $deploy_app_procedure->get_enabled_server_groups();
#    my $disabled_server_groups = $deploy_app_procedure->get_disabled_server_groups();
    my $additional_options = $deploy_app_procedure->get_additional_options();

    my $force = get_param_value('force');
    my $all_servers = get_param_value('assignallservergroups');

    my $source_is_url = 0;
    # e.g. the following format we accept the following:
    # '--url=https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war'
    if ($application_content_source_path =~ /^--url=/) {
        $jboss->log_info("Source with deployment is URL (such option available for EAP 7 and later versions): '$application_content_source_path'");
        $source_is_url = 1;
    }
    else {
        $jboss->log_info("Source with deployment is filepath: '$application_content_source_path'");
    }

    my $is_domain = 0;
    my $launch_type = $jboss->get_launch_type();
    $is_domain = 1 if $launch_type eq 'domain';

    if (!$jboss->{dryrun} && !$source_is_url && !-e $application_content_source_path) {
        $jboss->bail_out("File '$application_content_source_path' doesn't exists");
    }

    ######################
    # generate jboss cli command
    ######################

    my $command = qq/deploy /;

    if ($source_is_url) {
        $command .= qq/ $application_content_source_path /;
    }
    else {
        $command .= qq/ "$application_content_source_path" /;
    }

    if ($deployment_name) {
        $command .= qq/ --name=$deployment_name /;
    }

    if ($runtime_name) {
        $command .= qq/ --runtime-name=$runtime_name /;
    }

    if ($force) {
        $command .= ' --force ';
    }

    if ($is_domain && !$force) {
        if ($all_servers) {
            $command .= ' --all-server-groups ';
        }
        elsif ($enabled_server_groups) {
            $command .= qq/ --server-groups=$enabled_server_groups /;
        }
    }

    if ($additional_options) {
        $additional_options = $jboss->escape_string($additional_options);
        $command .= ' ' . $additional_options . ' ';
    }

    ######################
    # generate possible job step summary in case of successful completion
    ######################

    # expected source - filepath or url (url without '--url=' anchor)
    my $expected_source = $application_content_source_path;
    if ($source_is_url) {
        $expected_source =~ s/^--url=//;
    }
    # expected name of the deployment
    my $expected_appname;
    if ($deployment_name) {
        $expected_appname = $deployment_name;
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
