# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

use EC::JBoss;
use Data::Dumper;

$|=1;

main();


# my $property_path = '/plugins/$pk/project/dryrun';
sub main {
    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        appname
        servers
        serversgroup
        criteria
        wait_time
        hosts
    /);

    my @servers = ();
    my @server_groups = ();
    my @hosts = ();
    if ($params->{servers}) {
        @servers = map {$jboss->trim($_); $_;} split(',', $params->{servers});
    }

    if ($params->{serversgroup}) {
        @server_groups = map {$jboss->trim($_); $_;} split(',', $params->{serversgroup});
    }
    if ($params->{hosts}) {
        @hosts = map {$jboss->trim($_); $_;} split(',', $params->{hosts});
    }

    my $appname = $params->{appname};
    my $launch_type = $jboss->get_launch_type();
    # my $server_groups = [];
    my $servers = {};
    if ($launch_type eq 'domain') {
        $jboss->log_debug("Requesting servers with following parameters:");
        $jboss->log_debug("Hosts: " . join ', ', @hosts);
        $jboss->log_debug("Servers: " . join ', ', @servers);
        $jboss->log_debug("Groups: " . join ', ', @server_groups);
        # $server_groups = $jboss->get_server_groups();
        $servers = $jboss->get_servers(
            hosts => \@hosts,
            servers => \@servers,
            groups => \@server_groups
        );
        $jboss->log_debug("Servers found: " . Dumper $servers);
    }
    # logic for domain check
    if ($launch_type eq 'domain') {
        my @errors = ();
        my $result = $jboss->run_commands_until_done({
            sleep_time => 5,
            time_limit => $params->{wait_time},
        }, sub {
            @errors = ();
            for my $host (keys %$servers) {
                if (!@{$servers->{$host}}) {
                    $jboss->bail_out("No servers were found by your input for host: $host.");
                }
                for my $server (@{$servers->{$host}}) {
                    my $command = sprintf '/host=%s/server=%s/deployment=%s:read-attribute(name=status)', $host, $server, $appname;
                    my %result = $jboss->run_command($command);
                    if ($result{code}) {
                        $jboss->out("Application $appname (server: '$server', host: '$host') is NOT OK.");
                        # IF returned error AND expected ok THEN treat it as error. Otherwise it is an expected behaviour.
                        if ($params->{criteria} eq 'OK') {
                            push @errors, $server;
                        }
                        next;
                    }

                    my $json = $jboss->decode_answer($result{stdout});
                    if (!is_criteria_met_domain($json->{result}, $params->{criteria})) {
                        # if ($json->{result} ne 'OK') {
                        $jboss->out("Application $appname (server: '$server', host: '$host') is NOT OK.");
                        push @errors, $server;
                    }
                    $jboss->out("Application $appname (server: '$server', host: '$host') has status: $json->{result}. Desired: $params->{criteria}");
                }
            }
            if (@errors) {
                my $msg = 'Wrong application status on the following servers: ' . join (', ', @errors);
                $jboss->out($msg);
                return 0;
                # $jboss->bail_out($msg);
            }
            return 1;
        });
        if (!$result) {
            my $msg = 'Wrong application status on the following servers: ' . join (', ', @errors);
            $jboss->bail_out($msg);
        }
        return;
    }
    # logic for standalone mode

    my $command = "/deployment=$params->{appname}:read-attribute(name=status)";

    my $result = $jboss->run_commands_until_done({
        time_limit => $params->{wait_time},
        sleep_time => 5,
    }, sub {
        my %result = $jboss->run_command($command);
        my $json = $jboss->decode_answer($result{stdout});
        if (!is_criteria_met_standalone($json, $params->{criteria})) {
            $jboss->out("Criteria was not met. Application status: $result{stdout}");
            return 0;
        }
        return 1;
    });

    if ($result) {
        $jboss->success();
        return 1;
    }
    $jboss->error();
};

sub is_criteria_met_standalone {
    my ($json, $criteria) = @_;

    if ($criteria eq 'OK') {
        if ($json && $json->{outcome} && $json->{result} eq 'OK') {
            return 1;
        }
        return 0;
    }
    else {
        if ($json && $json->{outcome} && $json->{result} eq 'OK') {
            return 0;
        }
        return 1;
    }

}
sub is_criteria_met_domain {
    my ($got, $expected) = @_;

    if ($expected eq 'OK') {
        return 1 if $got eq $expected;
    }
    else {
        return 1 if $got ne 'OK';
    }
    return 0;
}
