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

    $jboss->{hide_password} = 1;

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
        $env_info = get_env_info_system_dump(
            jboss                 => $jboss,
            additional_parameters => $param_additional_options
        );
    }
    elsif ($param_information_type eq "profiles") {
        $env_info = get_env_info_profiles(
            jboss                 => $jboss,
            additional_parameters => $param_additional_options
        );
    }
    elsif ($param_information_type eq "dataSources" || $param_information_type eq "xaDataSources") {
        my $get_xa = 1 if $param_information_type eq "xaDataSources";

        if ($jboss_is_domain) {
            if ($param_information_type_context) {
                $env_info = get_env_info_data_sources_in_profile(
                    jboss                 => $jboss,
                    get_xa                => $get_xa,
                    profile               => $param_information_type_context,
                    additional_parameters => $param_additional_options
                );
            }
            else {
                $env_info = get_env_info_data_sources_in_all_profiles(
                    jboss                 => $jboss,
                    get_xa                => $get_xa,
                    additional_parameters => $param_additional_options
                );
            }
        }
        else {
            $env_info = get_env_info_data_sources_in_standalone(
                jboss                 => $jboss,
                get_xa                => $get_xa,
                additional_parameters => $param_additional_options
            );
        }
    }

    $env_info = replace_passwords_by_stars_in_cli_response($env_info);

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

sub replace_passwords_by_stars_in_cli_response {
    my $string = shift;
    return $string unless $string;
    $string =~ s/"password" => ".*?"/"password" => "***"/gs;
    return $string;
}

sub is_datasources_subsystem_available_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my @subsystems = @{ get_all_subsystems_in_profile(jboss => $jboss, profile => $profile) };
    my %subsystems_hash = map {$_ => 1} @subsystems;

    if ($subsystems_hash{'datasources'}) {
        return 1;
    }
    else {
        return 0;
    }
}

sub get_all_subsystems_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = qq|/profile=$profile/:read-children-names(child-type=subsystem)|;
    my $subsystems = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $subsystems;
}

sub get_env_info_system_dump {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $additional_parameters = $args{additional_parameters};

    $additional_parameters = $additional_parameters ? $additional_parameters : "";
    my $cli_command = "/:read-resource($additional_parameters)";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $additional_parameters = $args{additional_parameters};

    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/:read-children-resources(child-type=profile$additional_parameters)";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my $xa_prefix = $get_xa ? "xa-" : "";
    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/subsystem=datasources/:read-children-resources(child-type=${xa_prefix}data-source${additional_parameters})";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my $xa_prefix = $get_xa ? "xa-" : "";
    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/profile=${profile}/subsystem=datasources/:read-children-resources(child-type=${xa_prefix}data-source${additional_parameters})";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_all_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my @all_profiles = @{ get_all_profiles(jboss => $jboss) };
    my %profiles_env_info;
    foreach my $profile (@all_profiles) {
        if (is_datasources_subsystem_available_in_profile(jboss => $jboss, profile => $profile)) {
            $jboss->log_info("There is 'datasources' subsystem within '$profile' profile");
            $profiles_env_info{$profile} = get_env_info_data_sources_in_profile(
                jboss                 => $jboss,
                get_xa                => $get_xa,
                profile               => $profile,
                additional_parameters => $additional_parameters
            );
        }
        else {
            $jboss->log_info("There is no 'datasources' subsystem within '$profile' profile");
            $profiles_env_info{$profile} = "No 'datasources' subsystem";
        }
    }
    my $env_info = join("\n", map {"Profile '$_': $profiles_env_info{$_}"} keys %profiles_env_info);

    return $env_info;
}