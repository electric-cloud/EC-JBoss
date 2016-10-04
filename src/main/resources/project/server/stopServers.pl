=head1 NAME

stopServers.pl

=head1 DESCRIPTION

Stops jboss servers group(domain mode).

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
    my $command = sprintf '/server-group=%s:stop-servers', $params->{serversgroup};
    $jboss->out("Stopping serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command($command);
    $jboss->process_response(%result);
}

1;
