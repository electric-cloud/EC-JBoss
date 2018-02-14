=head1 NAME

checkHostControllerStatus.pl

=head1 DESCRIPTION

Checks status of hostcontroller.

=head1 COPYRIGHT

Copyright (c) 2014 Electric Cloud, Inc.

=cut

$[/myProject/procedure_helpers/preamble]

use warnings;
use strict;

use EC::JBoss;
use Data::Dumper;

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
    my $params = $jboss->get_params_as_hashref(
        'hostcontroller_name',
        'criteria',
        'wait_time'
    );
    my $status = '';
    $params->{criteria} = lc $params->{criteria};
    my $result = $jboss->run_commands_until_done({
        sleep_time => 5,
        time_limit => $params->{wait_time}
    }, sub {
        $status = '';
        $status = get_hostcontroller_status($jboss, $params->{hostcontroller_name});
        $jboss->out("Current HostController status: '$status'. Desired status: '$params->{criteria}'");
        return 1 if $status eq $params->{criteria};
        return 0;
    });
    unless ($result) {
        $jboss->bail_out("Criteria was not met. HostController is in '$status' status\n");
    }
    $jboss->success("Criteria was met. HostController is in '$status' status\n");
    return 1;
}

sub get_hostcontroller_status {
    my ($jboss, $name) = @_;

    my $command = ':read-children-resources(child-type=host,include-runtime=true)';
    my %result = $jboss->run_command($command);
    # if ($result{code} == 1 && $result{stdout} =~ m/Connection\srefused/s) {
    if ($result{code} == 1) {
        return 'not_running';
    }
    my $json = $jboss->decode_answer($result{stdout});

    if ($json->{outcome} ne 'success') {
        $jboss->bail_out();
    }
    if (!$json->{result}->{$name}) {
        # $jboss->bail_out("HostController with name '$name' doesn't exist");
        return 'not_running';
    }
    return $json->{result}->{$name}->{'host-state'};

}
