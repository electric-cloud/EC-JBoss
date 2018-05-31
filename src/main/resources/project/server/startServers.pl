=head1 NAME

startServers.pl

=head1 DESCRIPTION

Starts jboss servers group(domain mode).

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]
use Data::Dumper;
my $PROJECT_NAME = '$[/myProject/projectName]';
my $DESIRED_STATUS = 'STARTED';
my $SLEEP_TIME = 5;

$|=1;


main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'wait_time'
    );

    $jboss->bail_out("Required parameter 'serversgroup' is not provided") unless $params->{serversgroup};

    my $wait_time = undef;
    $params->{wait_time} = $jboss->trim($params->{wait_time});

    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time expected to be positive integer (wait time in seconds), 0 (unlimited) or undefined (one time check).");
        }
    }

    my ($servers, $states) = $jboss->get_servergroup_status($params->{serversgroup});

    if (!@$states) {
        $jboss->bail_out("Server group $params->{serversgroup} does not exist or empty.");
    }

    $jboss->{silent} = 1;
    my $servers_with_terminal_status = $jboss->is_servergroup_has_status(
        $params->{serversgroup},
        [$DESIRED_STATUS]
    );
    $jboss->{silent} = 0;
    if (@$servers_with_terminal_status) {
        for my $server_record (@$servers_with_terminal_status) {
            my $message = sprintf(
                "Server %s on %s is already in %s state",
                $server_record->{server},
                $server_record->{host},
                $server_record->{status}
            );
            $jboss->log_warning($message);
            $jboss->add_warning_summary($message);
        }
    }
    my $command = sprintf '/server-group=%s:start-servers', $params->{serversgroup};
    $jboss->out("Starting serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command_with_exiting_on_error(
        command => $command
    );

    my $res = {
        error => 0,
        msg => ''
    };

    my $done = 0;
    my $time_start = time();
    while (!$done) {
        my $time_diff = time() - $time_start;

        if (!$wait_time) {
            # if wait time is undefined or 0 then we perform check only once
            $done = 1;
        }
        elsif ($wait_time && $time_diff >= $wait_time) {
            # if wait time is already passed we do not perform more checks
            $done = 1;
            last;
        }

        my ($servers, $states_ref) = $jboss->get_servergroup_status($params->{serversgroup});
        my %seen = ();
        @$states_ref = grep {!$seen{$_}++} @$states_ref;
        if (scalar @$states_ref == 1 && $states_ref->[0] eq $DESIRED_STATUS) {
            $res->{error} = 0;
            $res->{msg} = '';
            last;
        }
        $res->{error} = 1;
        my $msg = 'Following servers are not started:' . "\n";
        for my $host_name ( keys %$servers ) {
            for my $server_name ( keys %{$servers->{$host_name}}) {
                $jboss->out("$server_name (host: $host_name) is $servers->{$host_name}->{$server_name}->{status}");
                # What should we set in this case?
                $msg .= "\n$server_name (host: $host_name, status: $servers->{$host_name}->{$server_name}->{status})";
            }
        }
        sleep 5;
    }
    if ($res->{error}) {
        $jboss->add_error_summary($res->{msg});
        $jboss->add_status_error();
    }
    return 1;
}

1;
