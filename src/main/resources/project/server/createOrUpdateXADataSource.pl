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
        dataSourceName
        jndiName
        jdbcDriverName
        xaDataSourceProperties
        dataSourceConnectionCredentials
        enabled
        profile
        additionalOptions
        /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_jndi_name = $params->{jndiName};
    my $param_jdbc_driver_name = $params->{jdbcDriverName};
    my $param_xa_data_source_properties = $params->{xaDataSourceProperties};
    my $param_data_source_connection_credentials = $params->{dataSourceConnectionCredentials};
    my $param_enabled = $params->{enabled};
    my $param_profile = $params->{profile};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_data_source_name) {
        $jboss->bail_out("Required parameter 'dataSourceName' is not provided");
    }
    if (!$param_jndi_name) {
        $jboss->bail_out("Required parameter 'jndiName' is not provided");
    }
    if (!$param_jdbc_driver_name) {
        $jboss->bail_out("Required parameter 'jdbcDriverName' is not provided");
    }
    if (!$param_xa_data_source_properties) {
        $jboss->bail_out("Required parameter 'xaDataSourceProperties' is not provided");
    }
    if (!defined $param_enabled) {
        $jboss->bail_out("Required parameter 'enabled' is not provided");
    }

    my $param_user_name;
    my $param_password;
    if ($param_data_source_connection_credentials) {
        my $xpath = $jboss->ec()->getFullCredential($param_data_source_connection_credentials);
        $param_user_name = $xpath->findvalue("//userName");
        $param_password = $xpath->findvalue("//password");
    }
    else {
        $jboss->bail_out("Required parameter 'dataSourceConnectionCredentials' is not provided");
    }

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

    if ($jboss_is_domain && !$param_profile) {
        $jboss->bail_out("Required parameter 'profile' is not provided (parameter required for JBoss domain)");
    }

    my $profile_prefix = $jboss_is_domain ? "/profile=$param_profile" : "";

    ########
    # check if xa data source with specified name exists
    ########
    my @all_xa_data_sources;
    if ($jboss_is_domain) {
        @all_xa_data_sources = @{ get_all_xa_data_sources_domain(jboss => $jboss, profile => $param_profile) };
    }
    else {
        @all_xa_data_sources = @{ get_all_xa_data_sources_standalone(jboss => $jboss) };
    }
    my %all_xa_data_sources_hash = map {$_ => 1} @all_xa_data_sources;

    my $xa_data_source_exists = 1 if $all_xa_data_sources_hash{$param_data_source_name};

    if ($xa_data_source_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("XA data source '$param_data_source_name' exists");

        $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:read-resource(recursive=false)";
        my $xa_data_source_resource = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );

        my $existing_jndi_name = $xa_data_source_resource->{'jndi-name'};
        my $existing_user_name = $xa_data_source_resource->{'user-name'};
        my $existing_password = $xa_data_source_resource->{'password'};

        my @updated_items;
        my @update_responses;

        if ($existing_jndi_name ne $param_jndi_name) {
            ########
            # jndi name differs
            ########
            $jboss->log_info("JNDI name differs and to be updated: current '$existing_jndi_name' VS specified in parameters '$param_jndi_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=jndi-name,value=$param_jndi_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "jndi name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_user_name ne $param_user_name) {
            ########
            # user name differs
            ########
            $jboss->log_info("User name differs and to be updated: current '$existing_user_name' VS specified in parameters '$param_user_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=user-name,value=$param_user_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "user name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_password ne $param_password) {
            ########
            # password differs
            ########
            $jboss->log_info("Password differs and to be updated");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=password,value=$param_password)";

            $jboss->{silent} = 1;
            my %result = $jboss->run_command($cli_command);
            $jboss->{silent} = 0;

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "password";
                push @update_responses, $result{stdout};
            }
        }

        if (@updated_items) {
            my $updated_items_str = join(", ", @updated_items);
            my $summary = "XA data source '$param_data_source_name' has been updated successfully by new $updated_items_str.";

            my @unique_update_responses = do {
                my %seen;
                grep {$_ && !$seen{$_}++} @update_responses
            };
            my $unique_update_responses_str = join("\n", @unique_update_responses);

            foreach my $response (@unique_update_responses) {
                if ($response && is_reload_or_restart_required($response)) {
                    $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                    $jboss->warning();
                    last;
                }
            }
            $summary .= "\nJBoss replies on update operations (unique set, refer to the logs for more information):\n" . $unique_update_responses_str if $unique_update_responses_str;
            $jboss->set_property(summary => $summary);
            return;
        }
        else {
            ########
            # updatable attributes match
            ########
            $jboss->log_info("Updatable attributes match - no updates will be performed");
            $jboss->set_property(summary => "XA data source '$param_data_source_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("XA data source '$param_data_source_name' does not exist - to be created");

        my $command_add_xa_data_source = qq/xa-data-source add /;
        if ($jboss_is_domain) {
            $command_add_xa_data_source .= qq/ --profile=$param_profile /;
        }
        $command_add_xa_data_source .= qq/ --name=$param_data_source_name --jndi-name=$param_jndi_name --driver-name=$param_jdbc_driver_name /;
        if ($param_user_name) {
            $command_add_xa_data_source .= qq| --user-name=$param_user_name |;
        }
        if ($param_password) {
            $command_add_xa_data_source .= qq| --password=$param_password |;
        }
        if ($param_enabled) {
            $command_add_xa_data_source .= qq| --enabled=true |;
        }
        else {
            $command_add_xa_data_source .= qq| --enabled=false |;
        }
        $command_add_xa_data_source .= qq| --xa-datasource-properties={$param_xa_data_source_properties} |;

        if ($param_additional_options) {
            my $escaped_additional_options = escape_additional_options($param_additional_options);
            $command_add_xa_data_source .= qq/ $escaped_additional_options /;
        }

        my %result = $jboss->run_command($command_add_xa_data_source);
        $jboss->process_response(%result);

        my $summary;
        if ($result{code}) {
            # we expect that summary was already set within process_response if code is not 0
            exit 1;
        }
        else {
            $summary = "XA data source '$param_data_source_name' has been added successfully";
            if ($result{stdout} && is_reload_or_restart_required($result{stdout})) {
                $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                $jboss->warning();
                $summary .= "\nJBoss reply: " . $result{stdout};
            }
            $jboss->set_property(summary => $summary);
        }

        return;
    }
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

sub get_all_xa_data_sources_domain {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = "/profile=$profile/subsystem=datasources/:read-children-names(child-type=xa-data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}

sub get_all_xa_data_sources_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = "/subsystem=datasources/:read-children-names(child-type=xa-data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}

sub escape_additional_options {
    my $additional_options = shift || croak "required param is not provided (additional_options)";

    $additional_options =~ s|\\|\\\\|;
    $additional_options =~ s|"|\"|gs;

    return $additional_options;
}

sub is_reload_or_restart_required {
    my $jboss_output = shift;
    croak "required param is not provided (jboss_output)" unless defined $jboss_output;
    if ($jboss_output =~ m/process-state:\s(reload|restart)-required/gs
        || $jboss_output =~ m/"process-state"\s=>\s"(reload|restart)-required"/gs) {
        return 1;
    }
    return 0;
}