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
        jbossTimeout
        allControllersShutdown
        /);

    my $param_timeout = $params->{jbossTimeout};
    $param_timeout = "" if !defined $param_timeout;
    $param_timeout = trim($param_timeout);

    my $param_all_controllers_shutdown = $params->{allControllersShutdown};

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

    if (!$jboss_is_domain) {
        $jboss->bail_out("Wrong usage of the procedure - StopDomain should be used for Managed Domain (not Standalone)");
    }

    ########
    # check jboss version
    ########
    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};
    my $jboss_is_6x = 1 if $product_version =~ m/^6/;

    ########
    # check whether timeout option is supported (supported in EAP 7 and later)
    ########
    if ($jboss_is_6x && $param_timeout ne "") {
        $jboss->log_warning("Timeout for stop-servers and shutdown is not supported in JBoss EAP 6.X - ignoring it");
        $param_timeout = "";
    }

    ########
    # get all host controller names
    ########
    my @all_hosts = @{ get_all_hosts(jboss => $jboss) };

    ########
    # stop all servers in domain
    ########
    $jboss->log_info("=======Started: stopping all servers within domain=======");

    # stop all servers withing domain
    my $cli_stop_servers = ":stop-servers";
    $cli_stop_servers .= "(timeout=$param_timeout)" if $param_timeout ne "";
    run_command_with_exiting_on_error(command => $cli_stop_servers, jboss => $jboss);

    # verification that all servers within domain are STOPPED or DISABLED
    my @servers_with_status_stopped_or_disabled;
    my @servers_with_status_stopping;
    my @servers_with_unexpected_status;
    foreach my $host (@all_hosts) {
        $cli_command = qq|/host=$host/:read-children-resources(child-type=server-config,include-runtime=true)|;
        my $server_config_resources = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );
        foreach my $server_name (keys %$server_config_resources) {
            my $server_status = $server_config_resources->{$server_name}->{status};
            if ($server_status eq "STOPPED" || $server_status eq "DISABLED") {
                $jboss->log_info("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_status_stopped_or_disabled, \%server_info;
            }
            elsif ($server_status eq "STOPPING") {
                $jboss->log_warning("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_status_stopping, \%server_info;
            }
            else {
                $jboss->log_error("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_unexpected_status, \%server_info;
            }
        }
    }

    ########
    # preparing step summary for stop servers part
    ########
    my @summary_messages;
    if (@servers_with_unexpected_status) {
        my $message = "Found " . scalar @servers_with_unexpected_status . " servers with unexpected statuses:";
        foreach my $server_info (@servers_with_unexpected_status) {
            my $server_name = $server_info->{server_name};
            my $server_status = $server_info->{server_status};
            my $host_name = $server_info->{host_name};
            $message .= join("\n", " server '$server_name' on host '$host_name' with '$server_status' status");
        }
        push @summary_messages, $message;
    }
    if (@servers_with_status_stopping) {
        my $message = "Found " . scalar @servers_with_status_stopping . " servers with STOPPING status:";
        foreach my $server_info (@servers_with_status_stopping) {
            my $server_name = $server_info->{server_name};
            my $server_status = $server_info->{server_status};
            my $host_name = $server_info->{host_name};
            $message .= join("\n", " server '$server_name' on host '$host_name' with '$server_status' status");
        }
        push @summary_messages, $message;
    }
    if (@servers_with_status_stopped_or_disabled) {
        my $message = "Found " . scalar @servers_with_status_stopped_or_disabled . " servers with expected statuses (STOPPED or DISABLED)";
        push @summary_messages, $message;
    }

    $summary = "Performed stop-servers operation for domain";
    if (@summary_messages) {
        $summary .= "\n" . join("\n", @summary_messages);
    }
    $jboss->set_property(summary => $summary);

    if ($param_all_controllers_shutdown) {
        if (@servers_with_unexpected_status || @servers_with_status_stopping) {
            $jboss->warning();
        }
    }
    else {
        if ((@servers_with_unexpected_status)) {
            $jboss->error();
            exit 1;
        }
        if (@servers_with_status_stopping) {
            $jboss->warning();
        }
    }

    $jboss->log_info("=======Finished: stopping all servers within domain=======");

    if ($param_all_controllers_shutdown) {
        $jboss->log_info("=======Started: shutdown all host controllers within domain=======");

        # gathering information about host controllers
        my @all_slave_hosts;
        my $master_host;

        foreach my $host (@all_hosts) {
            $jboss->log_info("Checking whether host controller '$host' is master or slave");
            if (is_host_master(jboss => $jboss, host => $host)) {
                $jboss->log_info("Host controller '$host' is master");
                $master_host = $host;
            }
            else {
                $jboss->log_info("Host controller '$host' is slave");
                push @all_slave_hosts, $host;
            }
        }

        # shutdown of slave host controllers
        foreach my $host (@all_slave_hosts) {
            $jboss->log_info("Starting shudown of slave host controller '$host'");
            my $cli_shutdown_slave = $jboss_is_6x
                ? get_cli_host_shutdown_6x(host => $host)
                : get_cli_host_shutdown_7x(host => $host, timeout => $param_timeout);
            run_command_with_exiting_on_error(command => $cli_shutdown_slave, jboss => $jboss);
            $summary .= "\nShutdown was performed for slave host controller '$host'";
            $jboss->log_info("Done with shudown of slave host controller '$host'");
        }

        # verification that shutdown of slave host controllers was successful
        my @all_hosts_after_all_slaves_shutdown = @{ get_all_hosts(jboss => $jboss) };
        if (@all_hosts_after_all_slaves_shutdown == 1
            || $all_hosts_after_all_slaves_shutdown[0] eq $master_host) {
            $jboss->log_info("All slave host controllers expect master '$master_host' are shut down");
        }
        else {
            my $error_summary = "Something wrong after stopping all slave host controllers (before stopping master host controller '$master_host').";
            $error_summary .= "\nExpected is to have only master host controller '$master_host' started at this point, but actual list of started host controllers is: ["
                . join(", ", @all_hosts_after_all_slaves_shutdown) . "]";
            $jboss->log_error($error_summary);

            $summary .= "\n\nError: $error_summary";
            $jboss->set_property(summary => $summary);
            $jboss->error();
            exit 1;
        }

        # shutdown of master host controller
        $jboss->log_info("Starting shudown of master host controller '$master_host'");
        my $cli_shutdown_master = $jboss_is_6x
            ? get_cli_host_shutdown_6x(host => $master_host)
            : get_cli_host_shutdown_7x(host => $master_host, timeout => $param_timeout);
        run_command_with_exiting_on_error(command => $cli_shutdown_master, jboss => $jboss);
        $summary .= "\nShutdown was performed for master host controller '$master_host'";
        $jboss->log_info("Done with shudown of master host controller '$master_host'");

        # verification that shutdown of master host controller was successful
        $cli_command = ':read-attribute(name=launch-type)';
        my %result = $jboss->run_command($cli_command);
        if ($result{code}
            && ($result{stdout} =~ m/The\scontroller\sis\snot\savailable/gs
            || $result{stderr} =~ m/The\scontroller\sis\snot\savailable/gs)) {
            $jboss->log_info("Master host controller '$master_host' is shut down, checked that connenction via CLI failed with 'The controller is not available...' error");
        }
        else {
            my $error_summary = "Check that master host controller '$master_host' is shut down failed";
            $jboss->log_error("Check that master host controller '$master_host' is shut down failed (check that connenction via CLI failed with 'The controller is not available...' error is failed)");

            $summary .= "\n\nError: $error_summary";
            $jboss->set_property(summary => $summary);
            $jboss->error();
            exit 1;
        }

        $jboss->set_property(summary => $summary);

        $jboss->log_info("=======Finished: shutdown all servers within domain=======");
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

sub get_all_hosts {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = qq|/:read-children-names(child-type=host)|;
    my $hosts = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return $hosts;
}

sub is_host_master {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host = $args{host} || croak "'host' is required param";

    my $cli_command = qq|/host=$host/:read-attribute(name=master)|;
    my $is_master = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return 1 if $is_master;
    return 0;
}

sub trim {
    my $s = shift;
    return $s if !$s;
    $s =~ s/^\s+|\s+$//g;
    return $s;
}

sub get_cli_host_shutdown_6x {
    my %args = @_;
    my $host = $args{host} || croak "'host' is required param";

    my $cli_host_shutdown = "/host=$host:shutdown";
    return $cli_host_shutdown;
}

sub get_cli_host_shutdown_7x {
    my %args = @_;
    my $host = $args{host} || croak "'host' is required param";
    my $timeout = $args{timeout};

    my $cli_host_shutdown = "shutdown --host=$host";
    $cli_host_shutdown .= " --timeout=$timeout" if $timeout && $timeout ne "";
    return $cli_host_shutdown;
}