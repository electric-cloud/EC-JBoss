=head1 NAME

runCustomCommand.pl

=head1 DESCRIPTION

JBoss custom commands runner

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

$|=1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref('customCommand', 'serverconfig', 'scriptphysicalpath', 'propertyName', 'dumpFormat');
    $jboss->out("Custom command: $params->{customCommand}");
    my %result = $jboss->run_command($params->{customCommand});

    if ($params->{propertyName}) {
        my $stdout = $result{stdout};
        my $json = $jboss->decode_answer($stdout);

        $jboss->save_retrieved_data(
            data => $json,
            property => $params->{propertyName},
            format => $params->{dumpFormat},
            raw => $stdout,
        );
    }

    $jboss->out("Command result:\n", $result{stdout});
    $jboss->process_response(%result);
}

1;
