# preamble.pl
$[/myProject/procedure_helpers/preamble]

use Carp;

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

$| = 1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1
    );

    my $params = $jboss->get_params_as_hashref(qw/
        informationType
        informationTypeContext
        additionalOptions
        /);

    my $param_information_type = $params->{informationType};
    my $param_information_type_context = $params->{informationTypeContext};
    my $param_additional_options = $params->{additionalOptions};

    my $property_path = "envInfo";
    my $env_info;

    my $cli_command;
    my $json;
    my $summary;

    ########
    # check jboss launch type
    ########
    $cli_command = ':read-attribute(name=launch-type)';

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $launch_type = lc $json->{result};
    if (!$launch_type || ($launch_type ne "standalone" && $launch_type ne "domain")) {
        $jboss->bail_out("Unknown JBoss launch type: '$launch_type'");
    }
    my $jboss_is_domain = 1 if $launch_type eq "domain";

    $jboss->log_info("=======Started: getting environment information, information type - '$param_information_type'=======");

    if ($param_information_type eq "systemDump") {
        my $additional_parameters = $param_additional_options if $param_additional_options;
        $cli_command = "/:read-resource($additional_parameters)";
        my $result = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );
        $env_info = $result;
    }
    elsif ($param_information_type eq "profiles") {
        my $additional_parameters = ",$param_additional_options" if $param_additional_options;
        $cli_command = "/:read-children-resources(child-type=profile$additional_parameters)";
        my $result = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );
        $env_info = $result;
    }
    elsif ($param_information_type eq "dataSources") {
        my $additional_parameters = ",$param_additional_options" if $param_additional_options;
        if ($jboss_is_domain) {
            if ($param_information_type_context) {
                my $profile = $param_information_type_context;
                $cli_command = "/profile=$profile/subsystem=datasources/:read-children-resources(child-type=data-source$additional_parameters)";
                my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                    command => $cli_command,
                    jboss   => $jboss
                );
                $env_info = $result;
            }
            else {
                my @all_profiles = @{ get_all_profiles(jboss => $jboss) };
                foreach my $profile (@all_profiles) {
                    $cli_command = "/profile=$profile/subsystem=datasources/:read-children-resources(child-type=data-source$additional_parameters)";
                    my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                        command => $cli_command,
                        jboss   => $jboss
                    );
                    $env_info = "Profile '$profile': $result\n";
                }
            }
        }
        else {
            $cli_command = "/subsystem=datasources/:read-children-resources(child-type=data-source$additional_parameters)";
            my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                command => $cli_command,
                jboss   => $jboss
            );
            $env_info = $result;
        }
    }
    elsif ($param_information_type eq "xaDataSources") {
        my $additional_parameters = ",$param_additional_options" if $param_additional_options;
        if ($jboss_is_domain) {
            if ($param_information_type_context) {
                my $profile = $param_information_type_context;
                $cli_command = "/profile=$profile/subsystem=datasources/:read-children-resources(child-type=xa-data-source$additional_parameters)";
                my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                    command => $cli_command,
                    jboss   => $jboss
                );
                $env_info = $result;
            }
            else {
                my @all_profiles = @{ get_all_profiles(jboss => $jboss) };
                foreach my $profile (@all_profiles) {
                    $cli_command = "/profile=$profile/subsystem=datasources/:read-children-resources(child-type=xa-data-source$additional_parameters)";
                    my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                        command => $cli_command,
                        jboss   => $jboss
                    );
                    $env_info = "Profile '$profile': $result\n";
                }
            }
        }
        else {
            $cli_command = "/subsystem=datasources/:read-children-resources(child-type=xa-data-source$additional_parameters)";
            my $result = run_command_and_get_json_result_with_exiting_on_non_success(
                command => $cli_command,
                jboss   => $jboss
            );
            $env_info = $result;
        }
    }

    $jboss->log_info("Requested Environment Information: $env_info");
    $jboss->set_property($property_path, $env_info);

    $jboss->log_info("=======Finished: getting environment information, information type - '$param_information_type'=======");
}

sub run_command_and_get_json_result_with_exiting_on_non_success {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json = run_command_and_get_json_with_exiting_on_non_success(
        command => $command,
        jboss   => $jboss
    );
    if (!defined $json->{result}) {
        $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
    }

    return $json->{result};
}

sub run_command_and_get_json_with_exiting_on_non_success {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json = run_command_and_get_json_with_exiting_on_error(
        command => $command,
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }

    return $json;
}

sub run_command_and_get_json_with_exiting_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = run_command_with_exiting_on_error(command => $command, jboss => $jboss);

    my $json = $jboss->decode_answer($result{stdout});
    $jboss->bail_out("Cannot convert JBoss response into JSON") if !$json;

    return $json;
}

sub run_command_with_exiting_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = $jboss->run_command($command);
    $jboss->process_response(%result);

    if ($result{code}) {
        exit 1;
    }

    return %result;
}

sub get_all_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = qq|/:read-children-names(child-type=profile)|;
    my $profiles = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return $profiles;
}