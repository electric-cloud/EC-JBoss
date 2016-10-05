=head1 NAME

checkServerGroupStatus.pl

=head1 DESCRIPTION

Checks if servers in server group are running/stopped.

=head1 COPYRIGHT

Copyright (c) 2016 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';

$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
    );

    # Seems like servers in one group could be on different hosts
    # So we have to list all hosts

    my $server_group_name = $params->{serversgroup};
    my $servers = {};
    my @states = ();
    my $hosts_json = run_jboss_command($jboss, ':read-children-names(child-type=host)');
    for my $host_name ( @{$hosts_json->{result}}) {
        my $command = sprintf '/host=%s:read-children-resources(child-type=server-config,include-runtime=true)', $host_name;
        my $children_json = run_jboss_command($jboss, $command);

        for my $server_name ( keys %{$children_json->{result} } ) {
            my $group = $children_json->{result}->{$server_name}->{group};
            next unless $group eq $server_group_name;

            my $status = $children_json->{result}->{$server_name}->{status};
            $servers->{$host_name}->{$server_name} = {status => $status};
            push @states, $status;
            $jboss->out("Found server $server_name in state $status");
        }
    }

    unless( keys %$servers ) {
        $jboss->bail_out("No servers found in server-group $server_group_name");
    }

    my %uniq = map { $_ => 1 } @states;
    # If there is only one unique status, then we can assume that all it is "server group status"
    # There are two terminal statuses: STARTED AND STOPPED (and there also are STARTING and I guess STOPPING)
    if ( scalar keys %uniq == 1 ) {
        my ($status) = keys %uniq;
        $jboss->out("Server group $server_group_name is $status");
        $jboss->set_property('server_group_status', $status);
    }
    else {
        for my $host_name ( keys %$servers ) {
            for my $server_name ( keys %{$servers->{$host_name}}) {
                $jboss->out("$server_name (host: $host_name) is $servers->{$host_name}->{$server_name}->{status}");
                # What should we set in this case?
                $jboss->set_property('server_group_status', 'PARTIAL');
            }
        }
    }
    $jboss->success;
    return 1;
}


# Bailing out if the command failed
sub run_jboss_command {
    my ($jboss, $command) = @_;

    my %response = $jboss->run_command($command);
    if ( $response{code} ) {
        $jboss->process_response(%response);
        $jboss->bail_out('An error occured while running jboss command');
    }
    my $json = $jboss->decode_answer($response{stdout});
    return $json;
}


1;
