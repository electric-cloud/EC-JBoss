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

my $SLEEP_TIME = 5;

$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'criteria',
        'wait_time'
    );

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
        msg => ''
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
        my %uniq = map { $_ => 1 } @states;
        # If there is only one unique status, then we can assume that all it is "server group status"
        # There are two terminal statuses: STARTED AND STOPPED (and there also are STARTING and I guess STOPPING)
        if (scalar keys %uniq == 1) {
            my $status = $states[0];
            $jboss->out("Server group $server_group_name is $status");
            $jboss->set_property('server_group_status', $status);
            if ($status eq $params->{criteria}) {
                # we done, criteria was met.
                $result->{msg} = "All servers in '$server_group_name' are $params->{criteria}.";
                $result->{error} = 0;
                $done = 1;
                last;
            }
            # sleep there for next loop iteration
            $result->{msg} = "Criteria $params->{criteria} is not met, group status is $status";
            $result->{error} = 1;
            sleep $SLEEP_TIME;
        }
        else {
            # statuses > 1, so, criteria wasn't met for sure.
            for my $host_name ( keys %$servers ) {
                for my $server_name ( keys %{$servers->{$host_name}}) {
                    $jboss->out("$server_name (host: $host_name) is $servers->{$host_name}->{$server_name}->{status}");
                    # What should we set in this case?
                    $jboss->set_property('server_group_status', 'PARTIAL');
                }
            }
            $result->{msg} = "Criteria $params->{criteria} is not met, servers in group are in different states";
            $result->{error} = 1;
            sleep $SLEEP_TIME;
        }
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
