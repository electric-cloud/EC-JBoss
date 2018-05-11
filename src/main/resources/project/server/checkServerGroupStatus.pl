=head1 NAME

checkServerGroupStatus.pl

=head1 DESCRIPTION

Checks if servers in server group are running/stopped.

=head1 COPYRIGHT

Copyright (c) 2016 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

my $PARAM_CRITERIA_STARTED = 'STARTED';
my $PARAM_CRITERIA_STOPPED_OR_DISABLED = 'STOPPED';

my %PARAM_CRITERIA_LABELS = (
    $PARAM_CRITERIA_STARTED             => "STARTED",
    $PARAM_CRITERIA_STOPPED_OR_DISABLED => "STOPPED",
);

my $STATUS_STOPPED = 'STOPPED'; # stopped status for servers with auto-start true
my $STATUS_DISABLED = 'DISABLED'; # stopped status for servers with auto-start false
my $STATUS_STARTED = 'STARTED'; # started status for servers
my $SLEEP_TIME = 5;

$| = 1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        plugin_name  => $PLUGIN_NAME,
        plugin_key   => $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'criteria',
        'wait_time'
    );

    $jboss->bail_out("Required parameter 'serversgroup' is not provided") unless $params->{serversgroup};
    $jboss->bail_out("Required parameter 'criteria' is not provided") unless $params->{criteria};

    my $param_criteria = $params->{criteria};
    if ($param_criteria ne $PARAM_CRITERIA_STARTED && $param_criteria ne $PARAM_CRITERIA_STOPPED_OR_DISABLED) {
        $jboss->bail_out("Unsupported option '$param_criteria' provided for parameter 'criteria' (supported options are: '$PARAM_CRITERIA_STARTED', '$PARAM_CRITERIA_STOPPED_OR_DISABLED)'");
    }
    my $param_criteria_label = $PARAM_CRITERIA_LABELS{$param_criteria};

    $params->{wait_time} = $jboss->trim($params->{wait_time});
    my $wait_time = undef;
    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time should be a positive integer");
        }
    }
    # Seems like servers in one group could be on different hosts
    # So we have to list all hosts

    my $server_group_name = $params->{serversgroup};
    my $done = 0;
    my $time_start = time();

    my $result = {
        error => 0,
        msg   => ''
    };

    while (!$done) {
        # wait time is not defined, it's empty so, one loop iteration only.
        my $time_diff = time() - $time_start;
        if (!defined $wait_time) {
            $done = 1;
        }
        elsif ($wait_time && $time_diff >= $wait_time) {
            $done = 1;
            last;
        }
        # otherwise we will wait forever.

        my ($servers, $states_ref) = $jboss->get_servergroup_status($server_group_name);
        my @states = @$states_ref;

        if (!@states) {
            $jboss->bail_out("Server group '$server_group_name' doesn't exist or empty");
        }

        my $criteria_is_met;
        if ($param_criteria eq "STARTED") {
            my @started_states = grep {$_ eq $STATUS_STARTED} @states;
            $criteria_is_met = 1 if scalar @started_states == scalar @states;
        }
        else {
            my @stopped_states = grep {$_ eq $STATUS_STOPPED || $_ eq $STATUS_DISABLED} @states;
            $criteria_is_met = 1 if scalar @stopped_states == scalar @states;
        }

        my %unique_states = map {$_ => 1} @states;
        my $unique_states_str = join(', ', keys %unique_states);

        $jboss->set_property('server_group_status', $unique_states_str);

        $jboss->log_info("Summarry log for server within '$server_group_name' server group:");
        for my $host_name (keys %$servers) {
            for my $server_name (keys %{$servers->{$host_name}}) {
                my $server_status = $servers->{$host_name}->{$server_name}->{status};
                $jboss->log_info("Server '$server_name' on host '$host_name' has status '$server_status'");
            }
        }

        if ($criteria_is_met) {
            $jboss->log_info("Criteria '$param_criteria_label' is met on this iteration. Servers in '$server_group_name' server group have statuses $unique_states_str");
            $result->{msg} = "Criteria '$param_criteria_label' is met.\nServers in '$server_group_name' server group have statuses $unique_states_str";
            $result->{error} = 0;
            $done = 1;
            last;
        }
        else {
            $jboss->log_info("Criteria '$param_criteria_label' is not met on this iteration. Servers in '$server_group_name' server group have statuses $unique_states_str");
            $result->{msg} = "Criteria '$param_criteria_label' is not met.\nServers in '$server_group_name' server group have statuses $unique_states_str";
            $result->{error} = 1;
        }

        sleep $SLEEP_TIME;
    }

    if ($result->{error}) {
        $jboss->error($result->{msg});
    }
    else {
        $jboss->success($result->{msg});
    }
    return 1;
}

1;
