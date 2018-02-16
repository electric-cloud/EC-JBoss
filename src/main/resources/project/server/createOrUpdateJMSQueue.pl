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
        project_name => $PROJECT_NAME,
        plugin_name  => $PLUGIN_NAME,
        plugin_key   => $PLUGIN_KEY,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        queueName
        jndiNames
        profile
        durable
        messageSelector
        additionalOptions
        /);

    my $param_queue_name = $params->{queueName};
    my $param_jndi_names = $params->{jndiNames};
    my $param_profile = $params->{profile};
    my $param_durable = $params->{durable};
    my $param_message_selector = $params->{messageSelector};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $summary_on_error;
    my %result;
    my $json;

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
        $jboss->error("Unknown JBoss launch type: '$launch_type'");
        return;
    }
    my $jboss_is_domain = 1 if $launch_type eq "domain";

    ########
    # check jboss version
    ########
    #todo

    ########
    # check if jms queue with specified name exists
    ########
    $cli_command = '/subsystem=messaging-activemq/server=default:read-children-resources(child-type=jms-queue)';

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_queue_resources = $json->{result};
    my $jms_queue_exists = 1 if $jms_queue_resources->{$param_queue_name};

    if ($jms_queue_exists) {
        ########
        # update logic
        ########

    }
    else {
        ########
        # create logic
        ########
        $cli_command = qq/jms-queue add /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile="$param_profile" /;
        }

        $cli_command .= qq/ --queue-address=$param_queue_name --entries=$param_jndi_names /;

        if ($param_durable) {
            $cli_command .= qq/ --durable=true /;
        }
        else {
            $cli_command .= qq/ --durable=false /;
        }

        if ($param_message_selector) {
            $cli_command .= qq/ --selector="$param_message_selector" /;
        }

        if ($param_additional_options) {
            my $escaped_additional_options = $jboss->escape_string($param_additional_options);
            $cli_command .= qq/ $escaped_additional_options /;
        }

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS queue $param_queue_name has been added successfully");
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