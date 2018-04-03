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
        timeout
        allControllersShutdown
        /);

    my $param_timeout = $params->{timeout};
    my $param_all_controllers_shutdown = $params->{allControllersShutdown};

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

    if (!$jboss_is_domain) {
        $jboss->bail_out("Wrong usage of the procedure - StopDomain should be used for Managed Domain (not Standalone)");
    }

    ########
    # stop all servers in domain
    ########
    $jboss->log_info("=======Started: stopping all servers within domain=======");
    my $cli_stop_servers = ":stop-servers";
    $cli_stop_servers .= "(timeout=$param_timeout)" if defined $param_timeout;
    run_command_with_exiting_on_error(command => $cli_stop_servers, jboss => $jboss);
    $jboss->log_info("=======Finished: stopping all servers within domain=======");

    if ($param_all_controllers_shutdown) {
        $jboss->log_info("=======Started: shutdown all host controllers within domain=======");

        my @all_slave_hosts = @{ get_all_slave_hosts(jboss => $jboss) };
        foreach my $host (@all_slave_hosts) {
            $jboss->log_info("Starting shudown of slave host '$host'");
            my $cli_shutdown_slave = "shutdown --host=$host";
            $cli_shutdown_slave .= " --timeout=$param_timeout" if defined $param_timeout;
            run_command_with_exiting_on_error(command => $cli_shutdown_slave, jboss => $jboss);
            $jboss->log_info("Done with shudown of slave host '$host'");
        }

        my $master_host = get_master_host_name(jboss => $jboss);
        $jboss->log_info("Starting shudown of master host '$master_host'");
        my $cli_shutdown_master = "shutdown --host=$master_host";
        $cli_shutdown_master .= " --timeout=$param_timeout" if defined $param_timeout;
        run_command_with_exiting_on_error(command => $cli_shutdown_master, jboss => $jboss);
        $jboss->log_info("Done with shudown of master host '$master_host'");

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

sub get_all_slave_hosts {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my @slave_hosts;

    my $cli_command = qq|/host=*:query(select=["name"],where=["master","false"])|;
    my $selected_entries = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    foreach my $entry (@$selected_entries) {
        push @slave_hosts, $entry->{result}->{name};
    }

    return \@slave_hosts;
}

sub get_master_host_name {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $master_host;

    my $cli_command = qq|/host=*:query(select=["name"],where=["master","true"])|;
    my $selected_entries = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    $master_host = $selected_entries->[0]->{result}->{name};

    return $master_host;
}