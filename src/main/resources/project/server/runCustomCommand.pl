=head1 NAME

runCustomCommand.pl

=head1 DESCRIPTION

JBoss custom commands runner

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]

use warnings;
use strict;

my $PROJECT_NAME = '$[/myProject/projectName]';
$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
    );

    my $params = $jboss->get_params_as_hashref('customCommand', 'serverconfig', 'scriptphysicalpath');
    $jboss->out("Custom command: $params->{customCommand}");
    my %result = $jboss->run_command($params->{customCommand});

    $jboss->out("Command result:\n", $result{stdout});
    $jboss->process_response(%result);
}

1;
