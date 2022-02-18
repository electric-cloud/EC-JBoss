package FlowPlugin::JBoss;
use strict;
use warnings;
use base qw/FlowPDF/;
use FlowPDF::Log;
use EC::JBoss;      # TODO: remove after pull port to PDK
use Carp;           # TODO: remove after pull port to PDK

# Feel free to use new libraries here, e.g. use File::Temp;

# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    return {
        pluginName          => '@PLUGIN_KEY@',
        pluginVersion       => '@PLUGIN_VERSION@',
        configFields        => ['config_name', 'configuration_name', 'serverconfig', 'config'],
        configLocations     => ['jboss_cfgs', 'ec_plugin_cfgs'],
        defaultConfigValues => {}
    };
}

# Auto-generated method for the connection check.
# Add your code into this method and it will be called when configuration is getting created.
# $self - reference to the plugin object
# $p - step parameters
# $sr - StepResult object
# Parameter: config
# Parameter: desc
# Parameter: credential
# Parameter: java_opts
# Parameter: jboss_url
# Parameter: log_level
# Parameter: scriptphysicalpath
# Parameter: test_connection
# Parameter: test_connection_res

sub checkConnection {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    my $configValues = $context->getConfigValues()->asHashref();
    logInfo("Config values are: ", $configValues);

    eval {
        # Use $configValues to check connection, e.g. perform some ping request
        # my $client = Client->new($configValues); $client->ping();
        my $password = $configValues->{password};
        if ($password ne 'secret') {
            # die "Failed to test connection - dummy check connection error\n";
        }
        1;
    } or do {
        my $err = $@;
        # Use this property to surface the connection error details in the CD server UI
        $sr->setOutcomeProperty("/myJob/configError", $err);
        $sr->apply();
        die $err;
    };
}
## === check connection ends ===

# Auto-generated method for the procedure CheckDeployStatus/CheckDeployStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: hosts
# Parameter: serversgroup
# Parameter: servers
# Parameter: criteria
# Parameter: wait_time

# $sr - StepResult object
sub checkDeployStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure CheckHostControllerStatus/CheckHostControllerStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: hostcontroller_name
# Parameter: wait_time
# Parameter: criteria

# $sr - StepResult object
sub checkHostControllerStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure CheckServerGroupStatus/CheckServerGroupStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time
# Parameter: criteria

# $sr - StepResult object
sub checkServerGroupStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure CheckServerStatus/CheckServerStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: url_check
# Parameter: host
# Parameter: server
# Parameter: criteria
# Parameter: wait_time

# $sr - StepResult object
sub checkServerStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure CreateDatasource/CreateDatasource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: application_name
# Parameter: connectionURL
# Parameter: driverClass
# Parameter: jndiName
# Parameter: driverName
# Parameter: profile
# Parameter: ds_credential
# Parameter: enabled

# $sr - StepResult object
sub createDatasource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure CreateOrUpdateDataSource/CreateOrUpdateDataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: jndiName
# Parameter: jdbcDriverName
# Parameter: connectionUrl
# Parameter: dataSourceConnection_credential
# Parameter: enabled
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateDataSource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self
    );

    $jboss->{hide_password} = 1;

    my $params = $jboss->get_params_as_hashref(qw/
        dataSourceName
        jndiName
        jdbcDriverName
        connectionUrl
        dataSourceConnection_credential
        enabled
        profile
        additionalOptions
    /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_jndi_name = $params->{jndiName};
    my $param_jdbc_driver_name = $params->{jdbcDriverName};
    my $param_connection_url = $params->{connectionUrl};
    my $param_data_source_connection_credentials = $params->{dataSourceConnection_credential};
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
        $jboss->bail_out("Required parameter 'dataSourceConnection_credential' is not provided");
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
    # check jboss version
    ########
    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};

    if ($product_version =~ m/^6/ || $product_version =~ m/^7\.0/) {
        if (!$param_connection_url) {
            $jboss->bail_out("Required parameter 'connectionUrl' is not provided (parameter required for JBoss EAP 6.X and 7.0)");
        }
    }

    ########
    # check if data source with specified name exists
    ########
    my @all_data_sources;
    if ($jboss_is_domain) {
        @all_data_sources = @{ get_all_data_sources_domain(jboss => $jboss, profile => $param_profile) };
    }
    else {
        @all_data_sources = @{ get_all_data_sources_standalone(jboss => $jboss) };
    }
    my %all_data_sources_hash = map {$_ => 1} @all_data_sources;

    my $data_source_exists = 1 if $all_data_sources_hash{$param_data_source_name};

    if ($data_source_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("Data source '$param_data_source_name' exists");

        $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:read-resource(recursive=false)";
        my $data_source_resource = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );

        my $existing_jndi_name = $data_source_resource->{'jndi-name'};
        my $existing_user_name = $data_source_resource->{'user-name'};
        my $existing_password = $data_source_resource->{'password'};

        my @updated_items;
        my @update_responses;

        if ($existing_jndi_name ne $param_jndi_name) {
            ########
            # jndi name differs
            ########
            $jboss->log_info("JNDI name differs and to be updated: current '$existing_jndi_name' VS specified in parameters '$param_jndi_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=jndi-name,value=$param_jndi_name)";

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

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=user-name,value=$param_user_name)";

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

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=password,value=$param_password)";

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
            my $summary = "Data source '$param_data_source_name' has been updated successfully by new $updated_items_str.";

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
            $jboss->set_property(summary => "Data source '$param_data_source_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("Data source '$param_data_source_name' does not exist - to be created");

        my $command_add_data_source = qq/data-source add /;
        if ($jboss_is_domain) {
            $command_add_data_source .= qq/ --profile=$param_profile /;
        }
        $command_add_data_source .= qq/ --name=$param_data_source_name --jndi-name=$param_jndi_name --driver-name=$param_jdbc_driver_name /;
        if ($param_user_name) {
            $command_add_data_source .= qq| --user-name=$param_user_name |;
        }
        if ($param_password) {
            $command_add_data_source .= qq| --password=$param_password |;
        }
        #there is no --enabled parameter for 'data-source add' command in JBoss EAP 6.0, to be enabled after creation if needed
        if ($product_version !~ m/^6\.0/) {
            if ($param_enabled) {
                $command_add_data_source .= qq| --enabled=true |;
            }
            else {
                $command_add_data_source .= qq| --enabled=false |;
            }
        }
        if ($param_connection_url) {
            $command_add_data_source .= qq| --connection-url==$param_connection_url |;
        }
        if ($param_additional_options) {
            my $escaped_additional_options = escape_additional_options($param_additional_options);
            $command_add_data_source .= qq/ $escaped_additional_options /;
        }

        my %result = run_command_with_exiting_on_error(command => $command_add_data_source, jboss => $jboss);

        my $summary = "Data source '$param_data_source_name' has been added successfully";
        if ($result{stdout} && is_reload_or_restart_required($result{stdout})) {
            $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
            $jboss->warning();
            $summary .= "\nJBoss reply: " . $result{stdout};
        }

        ########
        # there is no --enabled parameter for 'data-source add' command in JBoss EAP 6.0, let's enable it separately if needed
        ########
        if ($param_enabled && $product_version =~ m/^6\.0/) {
            $jboss->log_info("Enabling data source for 6.0 due to known issue (enable=true within data-source add command does not take affect, data source created and disabled)");

            my $command_enable_data_source = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:enable";
            run_command_with_exiting_on_error(command => $command_enable_data_source, jboss => $jboss);
        }

        $jboss->set_property(summary => $summary);

        return;
    }
}
# Auto-generated method for the procedure CreateOrUpdateJMSQueue/CreateOrUpdateJMSQueue
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: queueName
# Parameter: jndiNames
# Parameter: profile
# Parameter: durable
# Parameter: messageSelector
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateJMSQueue {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';


    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        queueName
        jndiNames
        profile
        durable
        messageSelector
        additionalOptions
    /);

    my $param_queue_name = $params->{queueName};
    my $param_jndi_names = $params->{jndiNames};
    my $param_profile = $params->{profile};
    my $param_durable = $params->{durable};
    my $param_message_selector = $params->{messageSelector};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_queue_name) {
        $jboss->bail_out("Required parameter 'queueName' is not provided");
    }
    if (!$param_jndi_names) {
        $jboss->bail_out("Required parameter 'jndiNames' is not provided");
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

    ########
    # check jboss version
    ########
    my $subsystem_part = "subsystem=messaging-activemq";
    my $provider_part = "server=default";

    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};
    if ($product_version =~ m/^6/) {
        $subsystem_part = "subsystem=messaging";
        $provider_part = "hornetq-server=default";
    }

    ########
    # check if jms queue with specified name exists
    ########
    if ($jboss_is_domain) {
        $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part:read-children-resources(child-type=jms-queue)";
    }
    else {
        $cli_command = "/$subsystem_part/$provider_part:read-children-resources(child-type=jms-queue)";
    }

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_queue_resources = $json->{result};
    my $jms_queue_exists = 1 if $jms_queue_resources->{$param_queue_name};

    if ($jms_queue_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("JMS queue '$param_queue_name' exists");

        my $existing_jndi_names = $jms_queue_resources->{$param_queue_name}->{entries};
        my @specified_jndi_names = split /,/, $param_jndi_names;

        my @sorted_existing_jndi_names = sort @$existing_jndi_names;
        my @sorted_specified_jndi_names = sort @specified_jndi_names;

        if ("@sorted_existing_jndi_names" ne "@sorted_specified_jndi_names") {
            ########
            # jndi names differ
            ########
            $jboss->log_info("JNDI names differ and to be updated: current [@sorted_existing_jndi_names] (sorted) VS specified in parameters [@sorted_specified_jndi_names] (sorted)");

            my $jndi_names_wrapped = join ',', map {qq/"$_"/} @specified_jndi_names;
            if ($jboss_is_domain) {
                $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }
            else {
                $cli_command = "/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                if ($result{stdout} && $result{stdout} =~ m/Attribute entries is not writable/s) {
                    $jboss->log_error("Update of JNDI names for JMS queue cannot be performed for this version of JBoss ($product_version). Attribute entries (jndi names) is not writable");
                    my $summary .= "Update of JNDI names for JMS queue '$param_queue_name' cannot be performed for this version of JBoss ($product_version).";
                    $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
                    $jboss->set_property(summary => $summary);
                    $jboss->error();
                    exit 1;
                }
                else {
                    $jboss->process_response(%result);
                    exit 1;
                }
            }
            else {
                $jboss->process_response(%result);
                my $summary = "JMS queue '$param_queue_name' has been updated successfully by new jndi names.";
                if ($result{stdout}) {
                    my $reload_or_restart_required;
                    if ($result{stdout} =~ m/"process-state"\s=>\s"reload-required"/gs
                        || $result{stdout} =~ m/"process-state"\s=>\s"restart-required"/gs) {
                        $reload_or_restart_required = 1;
                    }
                    if ($reload_or_restart_required) {
                        $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                        $jboss->warning();
                    }
                    $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
                }

                $jboss->set_property(summary => $summary);
            }
            return;
        }
        else {
            ########
            # jndi names match
            ########
            $jboss->log_info("JNDI names match - no updates will be performed");
            $jboss->set_property(summary => "JMS queue '$param_queue_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("JMS queue '$param_queue_name' does not exist - to be created");

        $cli_command = qq/jms-queue add /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --queue-address=$param_queue_name --entries=$param_jndi_names /;

        if ($param_durable) {
            $cli_command .= qq/ --durable=true /;
        }
        else {
            $cli_command .= qq/ --durable=false /;
        }

        if ($param_message_selector) {
            $cli_command .= qq/ --selector="$param_message_selector" /;
        }

        if ($param_additional_options) {
            my $escaped_additional_options = $jboss->escape_string($param_additional_options);
            $cli_command .= qq/ $escaped_additional_options /;
        }

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS queue '$param_queue_name' has been added successfully");
        return;
    }
}
# Auto-generated method for the procedure CreateOrUpdateJMSTopic/CreateOrUpdateJMSTopic
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: topicName
# Parameter: jndiNames
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateJMSTopic {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';


    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        topicName
        jndiNames
        profile
        additionalOptions
    /);

    my $param_topic_name = $params->{topicName};
    my $param_jndi_names = $params->{jndiNames};
    my $param_profile = $params->{profile};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_topic_name) {
        $jboss->bail_out("Required parameter 'topicName' is not provided");
    }
    if (!$param_jndi_names) {
        $jboss->bail_out("Required parameter 'jndiNames' is not provided");
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

    ########
    # check jboss version
    ########
    my $subsystem_part = "subsystem=messaging-activemq";
    my $provider_part = "server=default";

    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};
    if ($product_version =~ m/^6/) {
        $subsystem_part = "subsystem=messaging";
        $provider_part = "hornetq-server=default";
    }

    ########
    # check if jms topic with specified name exists
    ########
    if ($jboss_is_domain) {
        $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }
    else {
        $cli_command = "/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_topic_resources = $json->{result};
    my $jms_topic_exists = 1 if $jms_topic_resources->{$param_topic_name};

    if ($jms_topic_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("JMS topic '$param_topic_name' exists");

        my $existing_jndi_names = $jms_topic_resources->{$param_topic_name}->{entries};
        my @specified_jndi_names = split /,/, $param_jndi_names;

        my @sorted_existing_jndi_names = sort @$existing_jndi_names;
        my @sorted_specified_jndi_names = sort @specified_jndi_names;

        if ("@sorted_existing_jndi_names" ne "@sorted_specified_jndi_names") {
            ########
            # jndi names differ
            ########
            $jboss->log_info("JNDI names differ and to be updated: current [@sorted_existing_jndi_names] (sorted) VS specified in parameters [@sorted_specified_jndi_names] (sorted)");

            my $jndi_names_wrapped = join ',', map {qq/"$_"/} @specified_jndi_names;
            if ($jboss_is_domain) {
                $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part/jms-topic=$param_topic_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }
            else {
                $cli_command = "/$subsystem_part/$provider_part/jms-topic=$param_topic_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }

            my %result = run_command_with_exiting_on_error(
                command => $cli_command,
                jboss   => $jboss
            );

            my $summary = "JMS topic '$param_topic_name' has been updated successfully by new jndi names";
            if ($result{stdout}) {
                my $reload_or_restart_required;
                if ($result{stdout} =~ m/"process-state"\s=>\s"reload-required"/gs
                    || $result{stdout} =~ m/"process-state"\s=>\s"restart-required"/gs) {
                    $reload_or_restart_required = 1;
                }
                if ($reload_or_restart_required) {
                    $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                    $jboss->warning();
                }
                $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
            }

            $jboss->set_property(summary => $summary);
            return;
        }
        else {
            ########
            # jndi names match
            ########
            $jboss->log_info("JNDI names match - no updates will be performed");
            $jboss->set_property(summary => "JMS topic '$param_topic_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("JMS topic '$param_topic_name' does not exist - to be created");

        $cli_command = qq/jms-topic add /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --topic-address=$param_topic_name --entries=$param_jndi_names /;

        if ($param_additional_options) {
            my $escaped_additional_options = $jboss->escape_string($param_additional_options);
            $cli_command .= qq/ $escaped_additional_options /;
        }

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS topic '$param_topic_name' has been added successfully");
        return;
    }

}
# Auto-generated method for the procedure CreateOrUpdateXADataSource/CreateOrUpdateXADataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: jndiName
# Parameter: jdbcDriverName
# Parameter: xaDataSourceProperties
# Parameter: dataSourceConnection_credential
# Parameter: enabled
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateXADataSource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self
    );

    $jboss->{hide_password} = 1;

    my $params = $jboss->get_params_as_hashref(qw/
        dataSourceName
        jndiName
        jdbcDriverName
        xaDataSourceProperties
        dataSourceConnection_credential
        enabled
        profile
        additionalOptions
    /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_jndi_name = $params->{jndiName};
    my $param_jdbc_driver_name = $params->{jdbcDriverName};
    my $param_xa_data_source_properties = $params->{xaDataSourceProperties};
    my $param_data_source_connection_credentials = $params->{dataSourceConnection_credential};
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
        $jboss->bail_out("Required parameter 'dataSourceConnection_credential' is not provided");
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

        my %result = run_command_with_exiting_on_error(command => $command_add_xa_data_source, jboss => $jboss);

        my $summary = "XA data source '$param_data_source_name' has been added successfully";
        if ($result{stdout} && is_reload_or_restart_required($result{stdout})) {
            $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
            $jboss->warning();
            $summary .= "\nJBoss reply: " . $result{stdout};
        }

        ########
        # workaround for 6.0 (enable=true within xa-data-source add command does not take affect, data source created and disabled)
        ########
        if ($param_enabled) {
            my $version = $jboss->get_jboss_server_version();
            my $product_version = $version->{product_version};
            if ($product_version =~ m/^6\.0/) {
                $jboss->log_info("Enabling data source within 6.0 separately due to known issue (enable=true within xa-data-source add command does not take affect, data source created and disabled)");

                my $command_enable_data_source = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:enable";
                run_command_with_exiting_on_error(command => $command_enable_data_source, jboss => $jboss);
            }
        }

        $jboss->set_property(summary => $summary);

        return;
    }
}
# Auto-generated method for the procedure DeleteDatasource/DeleteDatasource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: datasource_name
# Parameter: profile

# $sr - StepResult object
sub deleteDatasource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure DeployApp/DeployApp
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: warphysicalpath
# Parameter: appname
# Parameter: runtimename
# Parameter: assignallservergroups
# Parameter: assignservergroups
# Parameter: force
# Parameter: additional_options

# $sr - StepResult object
sub deployApp {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure DeployApplication/DeployApplication
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: applicationContentSourcePath
# Parameter: deploymentName
# Parameter: runtimeName
# Parameter: enabledServerGroups
# Parameter: disabledServerGroups
# Parameter: additionalOptions

# $sr - StepResult object
sub deployApplication {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure DisableDeploy/DisableDeploy
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: assignservergroups

# $sr - StepResult object
sub disableDeploy {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure EnableDeploy/EnableDeploy
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: assignservergroups

# $sr - StepResult object
sub enableDeploy {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure GetEnvInfo/GetEnvInfo
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: informationType
# Parameter: informationTypeContext
# Parameter: additionalOptions

# $sr - StepResult object
sub getEnvInfo {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure RemoveJMSQueue/RemoveJMSQueue
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: queueName
# Parameter: profile

# $sr - StepResult object
sub removeJMSQueue {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure RemoveJMSTopic/RemoveJMSTopic
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: topicName
# Parameter: profile

# $sr - StepResult object
sub removeJMSTopic {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure RemoveXADataSource/RemoveXADataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: profile

# $sr - StepResult object
sub removeXADataSource {
    my ($self, $p, $sr) = @_;


    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure RunCustomCommand/RunCustomCommand
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: customCommand

# $sr - StepResult object
sub runCustomCommand {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         => $self
    );

    my $params = $jboss->get_params_as_hashref('customCommand', 'serverconfig', 'scriptphysicalpath');
    $jboss->out("Custom command: $params->{customCommand}");
    my %result = $jboss->run_command($params->{customCommand});

    $jboss->out("Command result:\n", $result{stdout});
    $jboss->process_response(%result);
}
# Auto-generated method for the procedure ShutdownStandaloneServer/ShutdownStandaloneServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath

# $sr - StepResult object
sub shutdownStandaloneServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StartDomainServer/StartDomainServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: alternatejbossconfig
# Parameter: alternateJBossConfigHost

# $sr - StepResult object
sub startDomainServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StartHostController/StartHostController
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: startupScript
# Parameter: domainConfig
# Parameter: hostConfig
# Parameter: jbossHostName
# Parameter: additionalOptions
# Parameter: logFileLocation

# $sr - StepResult object
sub startHostController {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StartServers/StartServers
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time

# $sr - StepResult object
sub startServers {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StartStandaloneServer/StartStandaloneServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: alternatejbossconfig
# Parameter: additionalOptions
# Parameter: logFileLocation

# $sr - StepResult object
sub startStandaloneServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StopDomain/StopDomain
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: jbossTimeout
# Parameter: allControllersShutdown

# $sr - StepResult object
sub stopDomain {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure StopServers/StopServers
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time

# $sr - StepResult object
sub stopServers {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure UndeployApp/UndeployApp
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: allrelevantservergroups
# Parameter: servergroups
# Parameter: keepcontent
# Parameter: additional_options

# $sr - StepResult object
sub undeployApp {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
## === step ends ===
# Please do not remove the marker above, it is used to place new procedures into this file.

sub is_criteria_met_standalone {
    my ($json, $criteria) = @_;

    if ($criteria eq 'OK') {
        if ($json && $json->{outcome} && $json->{result} eq 'OK') {
            return 1;
        }
        return 0;
    }
    else {
        if ($json && $json->{outcome} && $json->{result} eq 'OK') {
            return 0;
        }
        return 1;
    }

}
sub is_criteria_met_domain {
    my ($got, $expected) = @_;

    if ($expected eq 'OK') {
        return 1 if $got eq $expected;
    }
    else {
        return 1 if $got ne 'OK';
    }
    return 0;
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

sub get_all_data_sources_domain {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = "/profile=$profile/subsystem=datasources/:read-children-names(child-type=data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}

sub get_all_data_sources_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = "/subsystem=datasources/:read-children-names(child-type=data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
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
    if ($jboss_output =~ m/process-state:\s(?:reload|restart)-required/s
        || $jboss_output =~ m/"process-state"\s=>\s"(?:reload|restart)-required"/s) {
        return 1;
    }
    return 0;
}

1;