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
        dataSourceName
        profile
        /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_profile = $params->{profile};

    my $cli_command;
    my $json;

    if (!$param_data_source_name) {
        $jboss->bail_out("Required parameter 'dataSourceName' is not provided");
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
    # check if xa data source with specified name exists
    ########
    my @all_xa_data_sources;
    if ($jboss_is_domain) {
        @all_xa_data_sources = @{ get_all_xa_data_sources_domain(jboss => $jboss, profile => $param_profile) };
    }
    else {
        @all_xa_data_sources = @{ get_all_xa_data_sources_standalone(jboss => $jboss) };
    }
    my %all_xa_data_sources_hash = map {$_ => 1} @all_xa_data_sources;

    my $xa_data_source_exists = 1 if $all_xa_data_sources_hash{$param_data_source_name};

    if ($xa_data_source_exists) {
        $jboss->log_info("XA data source '$param_data_source_name' exists - to be removed");

        $cli_command = qq/xa-data-source remove /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --name=$param_data_source_name /;

        my %result = $jboss->run_commands($cli_command);
        $jboss->process_response(%result);

        my $summary;
        if ($result{code}) {
            # we expect that summary was already set within process_response if code is not 0
            exit 1;
        }
        else {
            $summary = "XA data source '$param_data_source_name' has been removed successfully";
            if ($result{stdout}
                && ($result{stdout} =~ m/process-state:\sreload-required/gs
                || $result{stdout} =~ m/process-state:\srestart-required/gs)) {
                $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                $jboss->warning();
                $summary .= "\nJBoss reply: " . $result{stdout};
            }
            $jboss->set_property(summary => $summary);
        }

        return;
    }
    else {
        $jboss->log_info("XA data source '$param_data_source_name' does not exist");

        $jboss->set_property(summary => "XA data source '$param_data_source_name' not found");
        $jboss->warning();
        return;
    }

}

sub run_command_and_get_json_result_with_exiting_on_non_success {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json = run_command_and_get_json_with_exiting_on_non_success(
        command => $command,
        jboss   => $jboss
    );
    if (!defined $json->{result}) {
        $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
    }

    return $json->{result};
}

sub run_command_and_get_json_with_exiting_on_non_success {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json = run_command_and_get_json_with_exiting_on_error(
        command => $command,
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }

    return $json;
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

sub get_all_xa_data_sources_domain {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = "/profile=$profile/subsystem=datasources/:read-children-names(child-type=xa-data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}

sub get_all_xa_data_sources_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = "/subsystem=datasources/:read-children-names(child-type=xa-data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}