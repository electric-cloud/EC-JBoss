# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
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

    my $check_result = undef;

    my $params = $jboss->get_params_as_hashref(qw/
        host
        server
        criteria
        url_check
        wait_time
    /);

    my $creds = $jboss->get_plugin_configuration();
    if ($params->{url_check}) {
        # do check via LWP.
        my $url = $jboss->fix_url($creds->{jboss_url});
        my $ua = LWP::UserAgent->new();

        $jboss->run_commands_until_done({
            sleep_time => 5,
            time_limit => $params->{wait_time}
        }, sub {
            my $resp = $ua->get($url);
            $check_result = is_criteria_met_url($resp, $params->{criteria});
            return $check_result;
        });
        # here condition
        if ($check_result) {
            $jboss->out("Criteria is met.");
            $jboss->success("Server at $url is $params->{criteria}");
            return;
        }
        $jboss->out("Criteria was not met.");
        $jboss->error("Criteria is not met. Server at $url should be in $params->{criteria} status.");
        return 1;
    }
    my $launch_type;
    $check_result = undef;

    $check_result = $jboss->run_commands_until_done({
        sleep_time => 5,
        time_limit => $params->{wait_time}
    }, sub {
        eval {
            $launch_type = $jboss->get_launch_type();
        };
        if (!$launch_type && $params->{criteria} eq 'NOT_RUNNING') {
            return 1;
        }
        if ($launch_type) {
            return 1;
        }
        return 0;
    });

    if (!$launch_type) {
        if ($params->{criteria} eq 'NOT_RUNNING') {
            $jboss->success("Server is not running. Criteria met");
        }
        else {
            $jboss->bail_out("Unknown launch type. Criteria not met.");
        }
    }
    my $command = '';
    if ($launch_type eq 'domain') {
        # domain detected
        if (!$params->{host} || !$params->{server}) {
            $jboss->bail_out('"Server" and "Host" parameters are mandatory when server is running in domain mode.');
        }
        $command = sprintf ('/host=%s/server-config=%s:read-attribute(name=status)', $params->{host}, $params->{server});
    }
    else {
        # standalone
        $command = ':read-attribute(name=server-state)';
    }

    my %result = ();
    $check_result = undef;
    $jboss->run_commands_until_done({
        sleep_time => 5,
        time_limit => $params->{wait_time},
        }, sub {
            %result = $jboss->run_command($command);
            $check_result = is_criteria_met($jboss, \%result, $launch_type, $params->{criteria});
            return $check_result;
        });
    # $jboss->process_response(%result);
};

sub is_criteria_met_url {
    my ($resp, $criteria) = @_;

    if ($criteria eq 'RUNNING') {
        return 1 if $resp->is_success();
    }
    # criterua is not running
    else {
        return 1 unless $resp->is_success();
    }
    return 0
}
sub is_criteria_met {
    my ($jboss, $result, $launch_type, $criteria) = @_;

    my $json = $jboss->decode_answer($result->{stdout});
    $json = {} unless $json;
    if ($launch_type eq 'domain') {
        # server is running
        if ($json->{result} && $json->{result} eq 'STARTED') {
            # criteria is RUNNING, so criteria met.
            if ($criteria eq 'RUNNING') {
                return 1;
            }
            # criteria is NOT_RUNNING, but server is in RUNNING state, so, criteria not met.
            return 0;
        }
        # criteria is RUNNING, but server is not. Not met.
        if ($criteria eq 'RUNNING') {
            return 0;
        }
        # criteria is NOT_RUNNING, server is not running. Criteria met.
        return 1;

    }
    else {
        if ($json->{outcome} && $json->{outcome} eq 'success' && $json->{result} && $json->{result} eq 'running') {
            if ($criteria eq 'RUNNING') {
                return 1;
            }
            return 0;
        }
        if ($criteria eq 'RUNNING') {
            return 0;
        }
        return 1;
    }
}
