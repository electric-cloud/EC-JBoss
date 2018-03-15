# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';
use Data::Dumper;
use URI;
use File::Basename;
use JSON;

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
        applicationContentSourcePath
        deploymentName
        runtimeName
        enabledServerGroups
        disabledServerGroups
        additionalOptions
        /);

    my $param_application_content_source_path = $params->{applicationContentSourcePath};
    my $param_deployment_name = $params->{deploymentName};
    my $param_runtime_name = $params->{runtimeName};
    my $param_enabled_server_groups = $params->{enabledServerGroups};
    my $param_disabled_server_groups = $params->{disabledServerGroups};
    my $param_additional_options = $params->{additionalOptions};

    ########
    # check required parameters, check application content source path - is it filepath or URL, check deployment name etc.
    ########
    $jboss->log_info("=======Started: initial analyzing of parametes=======");
    if (!$param_application_content_source_path) {
        $jboss->bail_out("Required parameter 'applicationContentSourcePath' is not provided");
    }

    my $source_is_url = 0;
    # e.g. we can accept the following:
    # '--url=https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war'
    if ($param_application_content_source_path =~ /^--url=/) {
        $jboss->log_info("Source with deployment is URL (such option available for EAP 7 and later versions): '$param_application_content_source_path'");
        $source_is_url = 1;
    }
    else {
        $jboss->log_info("Source with deployment is filepath: '$param_application_content_source_path'");
    }

    # expected source - filepath or url (url without '--url=' anchor)
    my $expected_source_for_summary = $param_application_content_source_path;
    if ($source_is_url) {
        $expected_source_for_summary =~ s/^--url=//;
    }

    # expected name of the deployment
    my $expected_deployment_name;
    if ($param_deployment_name) {
        $expected_deployment_name = $param_deployment_name;
    }
    elsif ($source_is_url) {
        my $application_content_source_url = $param_application_content_source_path;
        $application_content_source_url =~ s/^--url=//;
        # retrieve filename for url
        $expected_deployment_name = (URI->new($application_content_source_url)->path_segments)[- 1];
    }
    else {
        # retrieve filename for filepath
        $expected_deployment_name = fileparse($param_application_content_source_path);
    }
    $jboss->log_info("Expected deployment name is '$expected_deployment_name'");

    # if application content source path is filepath - check if file exists
    if (!$source_is_url && !-e $param_application_content_source_path) {
        $jboss->bail_out("File '$param_application_content_source_path' doesn't exists");
    }
    $jboss->log_info("=======Finished: initial analyzing of parametes=======");

    ########
    # check jboss launch type
    ########
    $jboss->log_info("=======Started: checking JBoss launch type=======");
    my $json_launch_type = run_command_and_get_json_with_exiting_on_error(
        command => ':read-attribute(name=launch-type)',
        jboss   => $jboss
    );
    if ($json_launch_type->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json_launch_type));
    }
    if (!defined $json_launch_type->{result}) {
        $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json_launch_type));
    }

    my $launch_type = lc $json_launch_type->{result};
    if (!$launch_type || ($launch_type ne "standalone" && $launch_type ne "domain")) {
        $jboss->bail_out("Unknown JBoss launch type: '$launch_type'");
    }
    $jboss->log_info("JBoss launch type is $launch_type");
    my $jboss_is_domain = 1 if $launch_type eq "domain";
    $jboss->log_info("=======Finished: checking JBoss launch type=======");

    if ($jboss_is_domain) {
        ########
        # logic for domain jboss
        ########

        # step 1
        # new content (deployment) will be uploaded to content repository
        # for this the --force flag used, what means if the content already exists - it will be replaced (upgraded) by new one
        # the upgrade operation also means that all server groups which already use the content will now use the updated one

        # step 2
        # content will be assigned to specified server groups if it is not assigned yet
        # content will be enabled on the groups specified in 'enabled server groups' parameter
        # content will be disabled on the groups specified in 'disabled server groups' parameter

        # workaround for WildFly issue - https://issues.jboss.org/browse/WFCORE-2939 (e.g. can be reproduce e.g. on Centos 7 with JBoss EAP 7.0)
        # for all server groups which use the deployment but are not specified in 'enabled/disabled server groups' parameters
        # we would like to make sure that state of deployment will be the same as before on these missing server groups
        # due to WFCORE-2939 before step 1 we will store all server groups on which deployment is disabled and which are missing in 'enabled/disabled server groups' parameters
        # and we will disable deployment on this server groups (e.g. if it becomes enabled due to WFCORE-2939)

        my $enabled_server_groups_is_all;
        if ($param_enabled_server_groups eq "--all-server-groups") {
            $enabled_server_groups_is_all = 1;
            if ($param_disabled_server_groups) {
                $jboss->bail_out("'disabledServerGroups' should be empty if 'enabledServerGroups' is '--all-server-groups'");
            }
        }
        my $disabled_server_groups_is_all;
        if ($param_disabled_server_groups eq "--all-server-groups") {
            $disabled_server_groups_is_all = 1;
            if ($param_enabled_server_groups) {
                $jboss->bail_out("'enabledServerGroups' should be empty if 'disabledServerGroups' is '--all-server-groups'");
            }
        }

        $jboss->log_info("=======Started: analyzing of information about the deployment on server groups=======");
        my @all_server_groups = @{ get_all_server_groups(jboss => $jboss) };
        my %all_server_groups_hash = map {$_ => 1} @all_server_groups;

        if (@all_server_groups) {
            $jboss->log_debug("List of all server groups: @all_server_groups");
        }
        else {
            $jboss->log_info("No server groups found");
        }

        my @specified_enabled_server_groups = $enabled_server_groups_is_all
            ? @all_server_groups
            : split /,/, $param_enabled_server_groups;
        $jboss->log_info("Requested to assign (in case of need) and enable deployment '$expected_deployment_name' on the following server group(s): @specified_enabled_server_groups.")
            if @specified_enabled_server_groups;

        my @specified_disabled_server_groups = $disabled_server_groups_is_all
            ? @all_server_groups
            : split /,/, $param_disabled_server_groups;
        $jboss->log_info("Requested to assign (in case of need) and disable deployment '$expected_deployment_name' on the following server group(s): @specified_disabled_server_groups.")
            if @specified_disabled_server_groups;

        my %specified_disabled_server_groups_hash = map {$_ => 1} @specified_disabled_server_groups;
        my @duplicated_server_groups_in_enabled_and_disabled_lists = grep {$specified_disabled_server_groups_hash{$_}} @specified_enabled_server_groups;
        if (@duplicated_server_groups_in_enabled_and_disabled_lists) {
            $jboss->bail_out("Duplicated server group(s) in enabled and disabled lists (please check provided parameters): " . join(
                ",", @duplicated_server_groups_in_enabled_and_disabled_lists));
        }

        my @all_specified_server_groups = (@specified_enabled_server_groups, @specified_disabled_server_groups);
        my %all_specified_server_groups_hash = map {$_ => 1} @all_specified_server_groups;

        my @non_existing_server_groups = grep {
            !$all_server_groups_hash{$_}
        } @all_specified_server_groups;
        $jboss->bail_out("Specified non existing server group(s): @non_existing_server_groups. Please add server groups before deploying to them.")
            if @non_existing_server_groups;

        my @missing_server_groups = grep {!$all_specified_server_groups_hash{$_}} @all_server_groups;

        my $missing_server_groups_sets_based_on_deployment = get_server_groups_sets_based_on_deployment(
            jboss         => $jboss,
            deployment    => $expected_deployment_name,
            server_groups => \@missing_server_groups
        );
        my @missing_enabled_server_groups = @{$missing_server_groups_sets_based_on_deployment->{server_groups_with_deployment_enabled}};
        $jboss->log_info("Deployment '$expected_deployment_name' is assigned and enabled on the server group(s) which are not in enabled/disabled list: @missing_enabled_server_groups. Leave the deployment enabled on this server group(s).")
            if @missing_enabled_server_groups;
        # needed for WFCORE-2939 workaround
        my @missing_disabled_server_groups = @{$missing_server_groups_sets_based_on_deployment->{server_groups_with_deployment_disabled}};
        $jboss->log_info("Deployment '$expected_deployment_name' is assigned and disabled on the server group(s) which are not in enabled/disabled list: @missing_disabled_server_groups. WFCORE-2939 workaround to be applied to make sure that we leave the deployment disabled on this server group(s).")
            if @missing_disabled_server_groups;

        my $specified_server_groups_sets_based_on_deployment = get_server_groups_sets_based_on_deployment(
            jboss         => $jboss,
            deployment    => $expected_deployment_name,
            server_groups => \@all_specified_server_groups
        );
        my @specified_server_groups_where_deployment_was_enabled = @{$specified_server_groups_sets_based_on_deployment->{server_groups_with_deployment_enabled}};
        $jboss->log_info("FYI.. Deployment '$expected_deployment_name' is assigned and enabled on the server group(s) which are in enabled/disabled list: @specified_server_groups_where_deployment_was_enabled")
            if @specified_server_groups_where_deployment_was_enabled;
        my @specified_server_groups_where_deployment_was_disabled = @{$specified_server_groups_sets_based_on_deployment->{server_groups_with_deployment_disabled}};
        $jboss->log_info("FYI.. Deployment '$expected_deployment_name' is assigned and disabled on the server group(s) which are in enabled/disabled list: @specified_server_groups_where_deployment_was_disabled")
            if @specified_server_groups_where_deployment_was_disabled;
        my @specified_server_groups_where_deployment_was_missing = @{$specified_server_groups_sets_based_on_deployment->{server_groups_without_deployment}};
        $jboss->log_info("Deployment '$expected_deployment_name' is not assigned the server group(s) which are in enabled/disabled list: @specified_server_groups_where_deployment_was_missing. Assigning will be performed for this server group(s)")
            if @specified_server_groups_where_deployment_was_missing;

        $jboss->log_info("=======Finished: analyzing of information about the deployment on server groups=======");


        #######
        # upgrade of deployment in content repository (or it is upload the deployment does not exist in content repository)
        #######
        $jboss->log_info("=======Started: upgrading (or uploading) of content in repository=======");
        my $cli_command_upgrade_content = qq/deploy --force --name=$expected_deployment_name /;
        $cli_command_upgrade_content .= qq/ --runtime-name=$param_runtime_name / if $param_runtime_name;
        $cli_command_upgrade_content .= $source_is_url ? qq/ $param_application_content_source_path / : qq/ "$param_application_content_source_path" /;

        run_command_with_exiting_on_error(
            command => $cli_command_upgrade_content,
            jboss   => $jboss
        );
        $jboss->log_info("=======Finished: upgrading (or uploading) of content in repository=======");

        #######
        # assigning of deployment to not yet assigned groups which are in enabled/disabled list
        #######
        if (@specified_server_groups_where_deployment_was_missing) {
            $jboss->log_info("=======Started: assigning deployment to needed server groups=======");
            assign_deployment_to_server_groups_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name,
                server_groups   => \@specified_server_groups_where_deployment_was_missing
            );
            $jboss->log_info("=======Finished: assigning deployment to needed server groups=======");
        }

        #######
        # enabling/disabling deployment on needed server groups
        #######
        if (@specified_enabled_server_groups) {
            $jboss->log_info("=======Started: enabling deployment on server groups which are in enabled list=======");
            enable_deployment_on_server_groups_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name,
                server_groups   => \@specified_enabled_server_groups
            );
            $jboss->log_info("=======Finished: enabling deployment on server groups which are in enabled list=======");
        }

        if (@specified_disabled_server_groups) {
            $jboss->log_info("=======Started: disabling deployment on server groups which are in disabled list=======");
            disable_deployment_on_server_groups_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name,
                server_groups   => \@specified_disabled_server_groups
            );
            $jboss->log_info("=======Finished: disabling deployment on server groups which are in disabled list=======");
        }

        # needed for WFCORE-2939 workaround
        if (@missing_disabled_server_groups) {
            $jboss->log_info("=======Started: WFCORE-2939 workaround - disabling deployment on server groups which are not in enabled/disabled list, but has disabled depoyment before=======");
            disable_deployment_on_server_groups_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name,
                server_groups   => \@missing_disabled_server_groups
            );
            $jboss->log_info("=======Finished: WFCORE-2939 workaround - disabling deployment on server groups which are not in enabled/disabled list, but has disabled depoyment before=======");
        }

        $jboss->log_info("=======Started: composing summary=======");
        my $summary = "Application '$expected_deployment_name' has been successfully deployed from '$expected_source_for_summary'.";
        eval {
            my $new_server_groups_sets_based_on_deployment = get_server_groups_sets_based_on_deployment(
                jboss         => $jboss,
                deployment    => $expected_deployment_name,
                server_groups => \@all_server_groups
            );

            my @new_enabled_server_groups = @{$new_server_groups_sets_based_on_deployment->{server_groups_with_deployment_enabled}};
            if (@new_enabled_server_groups) {
                $jboss->log_info("Summary: deployment '$expected_deployment_name' is assigned and enabled on the server group(s): @new_enabled_server_groups.");
                $summary .= "\nEnabled on: " . join(",", @new_enabled_server_groups) . " server groups.";
            }

            my @new_disabled_server_groups = @{$new_server_groups_sets_based_on_deployment->{server_groups_with_deployment_disabled}};
            if (@new_disabled_server_groups) {
                $jboss->log_info("Summary: deployment '$expected_deployment_name' is assigned and disabled on the server group(s): @new_disabled_server_groups.");
                $summary .= "\nDisabled on: " . join(",", @new_disabled_server_groups) . " server groups.";
            }
        };
        if ($@) {
            $summary .= "\nError occured when composing summary: $@";
            $jboss->log_warning("Error occured when composing summary: $@");
            $jboss->warning();
        }
        $jboss->set_property(summary => $summary);
        $jboss->log_info("=======Finished: composing summary=======");
        return;
    }
    else {
        ########
        # logic for standalone jboss
        ########
        $jboss->log_info("=======Started: deploying to standalone=======");
        my $cli_command_deploy_to_standalone = qq/deploy --force --name=$expected_deployment_name/;
        $cli_command_deploy_to_standalone .= qq/ --runtime-name=$param_runtime_name / if $param_runtime_name;
        $cli_command_deploy_to_standalone .= $source_is_url ? qq/ $param_application_content_source_path / : qq/ "$param_application_content_source_path" /;

        run_command_with_exiting_on_error(
            command => $cli_command_deploy_to_standalone,
            jboss   => $jboss
        );
        $jboss->log_info("=======Finished: deploying to standalone=======");

        my $summary = "Application '$expected_deployment_name' has been successfully deployed from '$expected_source_for_summary'.";

        if ($param_additional_options && $param_additional_options eq "--disabled") {
            $jboss->log_info("=======Started: disabling deployment on standalone server=======");
            disable_deployment_on_standalone_server_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name
            );
            $summary .= "\nDisabled on standalone server.";
            $jboss->log_info("=======Finished: disabling deployment on standalone server=======");
        }
        else {
            $jboss->log_info("=======Started: enabling deployment on standalone server=======");
            enable_deployment_on_standalone_server_or_fail(
                jboss           => $jboss,
                deployment_name => $expected_deployment_name
            );
            $summary .= "\nEnabled on standalone server.";
            $jboss->log_info("=======Finished: enabling deployment on standalone server=======");
        }

        if ($param_additional_options && $param_additional_options ne "--disabled") {
            $jboss->log_warning("Additional options '$param_additional_options' are not supported, currently supported option is '--disabled'");
            $summary .= "\nAdditional options '$param_additional_options' are not supported, currently supported option is '--disabled'";
            $jboss->warning();
        }

        $jboss->set_property(summary => $summary);
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

sub enable_deployment_on_server_groups_or_fail {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment_name = $args{deployment_name} || croak "'deployment_name' is required param";
    my $server_groups = $args{server_groups} || croak "'server_groups' is required param";

    foreach my $server_group (@$server_groups) {
        $jboss->log_info("Enabling deployment '$deployment_name' on server group '$server_group'");
        my $json = run_command_and_get_json_with_exiting_on_error(
            command => "/server-group=$server_group/deployment=$deployment_name:deploy",
            jboss   => $jboss
        );
        if ($json->{outcome} ne "success") {
            $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
        }
        $jboss->log_info("Enabled deployment '$deployment_name' on server group '$server_group'");
    }
}

sub disable_deployment_on_server_groups_or_fail {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment_name = $args{deployment_name} || croak "'deployment_name' is required param";
    my $server_groups = $args{server_groups} || croak "'server_groups' is required param";

    foreach my $server_group (@$server_groups) {
        $jboss->log_info("Disabling deployment '$deployment_name' on server group '$server_group'");
        my $json = run_command_and_get_json_with_exiting_on_error(
            command => "/server-group=$server_group/deployment=$deployment_name:undeploy",
            jboss   => $jboss
        );
        if ($json->{outcome} ne "success") {
            $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
        }
        $jboss->log_info("Disabled deployment '$deployment_name' on server group '$server_group'");
    }
}

sub assign_deployment_to_server_groups_or_fail {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment_name = $args{deployment_name} || croak "'deployment_name' is required param";
    my $server_groups = $args{server_groups} || croak "'server_groups' is required param";

    foreach my $server_group (@$server_groups) {
        $jboss->log_info("Assigning deployment '$deployment_name' to server group '$server_group'");
        my $json = run_command_and_get_json_with_exiting_on_error(
            command => "/server-group=$server_group/deployment=$deployment_name:add",
            jboss   => $jboss
        );
        if ($json->{outcome} ne "success") {
            $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
        }
        $jboss->log_info("Assigned deployment '$deployment_name' to server group '$server_group'");
    }
}

sub get_all_server_groups {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json = run_command_and_get_json_with_exiting_on_error(
        command => ':read-children-names(child-type=server-group)',
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }
    if (!defined $json->{result}) {
        $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
    }

    my $all_server_groups = $json->{result};
    return $all_server_groups;
}

sub get_all_deployments_on_server_group {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $server_group = $args{server_group} || croak "'server_group' is required param";

    my $json = run_command_and_get_json_with_exiting_on_error(
        command => "/server-group=$server_group:read-children-names(child-type=deployment)",
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }
    if (!defined $json->{result}) {
        $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
    }

    my $all_deployments = $json->{result};
    return $all_deployments;
}

sub is_deployment_assigned_to_server_group {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $server_group = $args{server_group} || croak "'server_group' is required param";
    my $deployment = $args{deployment} || croak "'deployment' is required param";

    my $all_deployments = get_all_deployments_on_server_group(
        jboss        => $jboss,
        server_group => $server_group
    );
    my %all_deployments_hash = map {$_ => 1} @$all_deployments;

    if ($all_deployments_hash{$deployment}) {
        return 1;
    }
    else {
        return 0;
    }
}

sub get_server_groups_sets_based_on_deployment {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment = $args{deployment} || croak "'deployment' is required param";
    my $server_groups = $args{server_groups} || croak "'server_groups' is required param";

    my @server_groups_with_deployment_enabled;
    my @server_groups_with_deployment_disabled;
    my @server_groups_without_deployment;
    my %server_groups_sets_based_on_deployment;

    foreach my $server_group (@$server_groups) {
        if (is_deployment_assigned_to_server_group(jboss => $jboss, server_group => $server_group, deployment =>
            $deployment)) {
            my $json = run_command_and_get_json_with_exiting_on_error(
                command => "/server-group=$server_group/deployment=$deployment:read-resource",
                jboss   => $jboss
            );
            if ($json->{outcome} ne "success") {
                $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
            }
            if (!defined $json->{result}) {
                $jboss->bail_out("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
            }

            if ($json->{result}->{enabled}) {
                push @server_groups_with_deployment_enabled, $server_group;
            }
            else {
                push @server_groups_with_deployment_disabled, $server_group;
            }
        }
        else {
            push @server_groups_without_deployment, $server_group;
        }
    }

    $server_groups_sets_based_on_deployment{server_groups_with_deployment_enabled} = \@server_groups_with_deployment_enabled;
    $server_groups_sets_based_on_deployment{server_groups_with_deployment_disabled} = \@server_groups_with_deployment_disabled;
    $server_groups_sets_based_on_deployment{server_groups_without_deployment} = \@server_groups_without_deployment;

    return \%server_groups_sets_based_on_deployment;
}

sub enable_deployment_on_standalone_server_or_fail {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment_name = $args{deployment_name} || croak "'deployment_name' is required param";

    $jboss->log_info("Enabling deployment '$deployment_name' on standalone server");
    my $json = run_command_and_get_json_with_exiting_on_error(
        command => "/deployment=$deployment_name/:deploy",
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }
    $jboss->log_info("Enabled deployment '$deployment_name' on standalone server");
}

sub disable_deployment_on_standalone_server_or_fail {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $deployment_name = $args{deployment_name} || croak "'deployment_name' is required param";

    $jboss->log_info("Disabling deployment '$deployment_name' on standalone server");
    my $json = run_command_and_get_json_with_exiting_on_error(
        command => "/deployment=$deployment_name/:undeploy",
        jboss   => $jboss
    );
    if ($json->{outcome} ne "success") {
        $jboss->bail_out("JBoss replied with outcome other than success: " . (encode_json $json));
    }
    $jboss->log_info("Disabled deployment '$deployment_name' on standalone server");
}