# preamble.pl
$[/myProject/procedure_helpers/preamble]

use Carp;

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

$| = 1;

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1
    );

    my $params = $jboss->get_params_as_hashref(qw/
        queueName
        jndiNames
        profile
        /);

    my $param_queue_name = $params->{queueName};
    my $param_profile = $params->{profile};

    my $cli_command;
    my $json;

    if (!$param_queue_name) {
        $jboss->bail_out("Required parameter 'queueName' is not provided");
    }

    ########
    # check jboss launch type
    ########
    $cli_command = ':read-attribute(name=launch-type)';

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $launch_type = lc $json->{result};
    if (!$launch_type || ($launch_type ne "standalone" && $launch_type ne "domain")) {
        $jboss->bail_out("Unknown JBoss launch type: '$launch_type'");
    }
    my $jboss_is_domain = 1 if $launch_type eq "domain";

    if ($jboss_is_domain && !$param_profile) {
        $jboss->bail_out("Required parameter 'profile' is not provided (parameter required for JBoss domain)");
    }

    ########
    # check jboss version
    ########
    my $subsystem_part = "subsystem=messaging-activemq";
    my $provider_part = "server=default";

    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};
    if ($product_version =~ /^6/) {
        $subsystem_part = "subsystem=messaging";
        $provider_part = "hornetq-server=default";
    }

    ########
    # check if jms queue with specified name exists
    ########
    if ($jboss_is_domain) {
        $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part:read-children-resources(child-type=jms-queue)";
    }
    else {
        $cli_command = "/$subsystem_part/$provider_part:read-children-resources(child-type=jms-queue)";
    }

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_queue_resources = $json->{result};
    my $jms_queue_exists = 1 if $jms_queue_resources->{$param_queue_name};

    if ($jms_queue_exists) {
        $jboss->log_info("JMS queue '$param_queue_name' exists - to be removed");

        $cli_command = qq/jms-queue remove /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --queue-address=$param_queue_name /;

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS queue '$param_queue_name' has been removed successfully");
        return;
    }
    else {
        $jboss->log_info("JMS queue '$param_queue_name' does not exist");

        $jboss->set_property(summary => "JMS queue '$param_queue_name' not found");
        $jboss->warning();
        return;
    }
}

sub run_command_and_get_json_with_exiting_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = run_command_with_exiting_on_error(command => $command, jboss => $jboss);

    my $json = $jboss->decode_answer($result{stdout});
    $jboss->bail_out("Cannot convert JBoss response into JSON") if !$json;

    return $json;
}

sub run_command_with_exiting_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = $jboss->run_command($command);
    $jboss->process_response(%result);

    if ($result{code}) {
        exit 1;
    }

    return %result;
}