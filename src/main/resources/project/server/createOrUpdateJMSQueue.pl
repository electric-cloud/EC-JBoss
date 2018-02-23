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

            if ($jboss_is_domain) {
                $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$param_jndi_names])";
            }
            else {
                $cli_command = "/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$param_jndi_names])";
            }

            my %result = run_command_with_exiting_on_error(
                command => $cli_command,
                jboss   => $jboss
            );

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