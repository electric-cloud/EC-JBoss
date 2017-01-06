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

    my $params = $jboss->get_params_as_hashref(qw/
        appname
    /);

    my $appname = $params->{appname};
    my $launch_type = $jboss->get_launch_type();
    # my $server_groups = [];
    my $servers = {};
    if ($launch_type eq 'domain') {
        # $server_groups = $jboss->get_server_groups();
        $servers = $jboss->get_servers();
    }
    # logic for domain check
    my @errors = ();
    if ($launch_type eq 'domain') {
        for my $host (keys %$servers) {
            for my $server (@{$servers->{$host}}) {
                my $command = sprintf '/host=%s/server=%s/deployment=%s:read-attribute(name=status)', $host, $server, $appname;
                my %result = $jboss->run_command($command);
                if ($result{code}) {
                    $jboss->out("Application $appname (server: $server, host: $host is NOT OK");
                    push @errors, $server;
                    next;
                }

                my $json = $jboss->decode_answer($result{stdout});
                if ($json->{result} ne 'OK') {
                    $jboss->out("Application $appname (server: '$server', host: '$host') is NOT OK");
                    push @errors, $server;
                }
                $jboss->out("Application $appname (server: $server, host: $host is $json->{result}");
            }
        }

        if (@errors) {
            my $msg = 'Wrong application status on the following servers: ' . join (', ', @errors);
            $jboss->bail_out($msg);
        }
        return;
    }
    # logic for standalone mode

    my $command = "/deployment=$params->{appname}:read-attribute(name=status)";
    my %result = $jboss->run_command($command);
    $jboss->process_response(%result);
};
