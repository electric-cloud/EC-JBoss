=head1 NAME

startServers.pl

=head1 DESCRIPTION

Starts jboss servers group(domain mode).

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

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
    my $command = sprintf '/server-group=%s:start-servers', $params->{serversgroup};
    $jboss->out("Starting serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command($command);
    $jboss->process_response(%result);
    return 1;
}

1;
