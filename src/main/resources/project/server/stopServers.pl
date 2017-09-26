=head1 NAME

startServers.pl

=head1 DESCRIPTION

Starts jboss servers group(domain mode).

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

=cut
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $STATUS_STOPPED = 'STOPPED'; # stopped status for servers with auto-start true
my $STATUS_DISABLED = 'DISABLED'; # stopped status for servers with auto-start false
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

    my $wait_time = undef;
    $params->{wait_time} = $jboss->trim($params->{wait_time});

    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time should be a positive integer.");
        }
    }

    my ($servers, $states) = $jboss->get_servergroup_status($params->{serversgroup});

    if (!@$states) {
        $jboss->bail_out("Server group $params->{serversgroup} does not exist or empty.");
    }

    $jboss->{silent} = 1;
    my $servers_with_terminal_status = $jboss->is_servergroup_has_status(
        $params->{serversgroup},
        ['STOPPED']
    );
    $jboss->{silent} = 0;
    if (@$servers_with_terminal_status) {
        for my $server_record (@$servers_with_terminal_status) {
            $jboss->out_warning(
                sprintf(
                    "Server %s on %s is already in %s state",
                    $server_record->{server},
                    $server_record->{host},
                    $server_record->{status}
                )
            );
        }
    }
    my $command = sprintf '/server-group=%s:stop-servers', $params->{serversgroup};
    $jboss->out("Stopping serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command($command);
    my $done = 0;
    my $time_start = time();

    my $res = {
        error => 0,
        msg => ''
    };

    while (defined $wait_time && !$done) {
        my $time_diff = time() - $time_start;
        if ($wait_time && $time_diff >= $wait_time) {
            $done = 1;
            last;
        }
        my ($servers, $states_ref) = $jboss->get_servergroup_status($params->{serversgroup});
        my %seen = ();
        @$states_ref = grep {!$seen{$_}++} @$states_ref;
        if ( scalar @$states_ref == 1 && ( $states_ref->[0] eq $STATUS_STOPPED || $states_ref->[0] eq $STATUS_DISABLED ) ) {
            $res->{error} = 0;
            $res->{msg} = '';
            last;
        }
        $res->{error} = 1;
        my $msg = 'Following servers are not stopped:' . "\n";
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
        $jboss->bail_out($res->{msg});
    }
    $jboss->process_response(%result);
    return 1;
}

1;
