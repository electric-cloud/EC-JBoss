package FlowPlugin::JBoss;
use strict;
use warnings;
use base qw/FlowPDF/;
use FlowPDF::Log;
use EC::JBoss;      # TODO: remove after pull port to PDK
use Carp;           # TODO: remove after pull port to PDK
use File::Basename;

# For StartHostController procedure
use constant {
    SUCCESS                          => 0,
    ERROR                            => 1,
    SQUOTE                           => q{'},
    DQUOTE                           => q{"},
    BSLASH                           => q{\\},
    WIN_IDENTIFIER                   => 'MSWin32',
    MAX_ELAPSED_TEST_TIME            => 60,
    SLEEP_INTERVAL_TIME              => 3,
    EXPECTED_SERVER_LOG_FILE_NAME    => 'server.log',
    NUMBER_OF_LINES_TO_TAIL_FROM_LOG => 100,
    STATUS_ERROR                     => "error",
    STATUS_WARNING                   => "warning",
    STATUS_SUCCESS                   => "success",
    # For StartStandaloneServer
    PLUGIN_NAME                      => '@PLUGIN_KEY@',
    CREDENTIAL_ID                    => 'credential',
    EXPECTED_LOG_FILE_NAME           => 'server.log',
    # For StartDomainServer
    DOMAIN_MAX_ELAPSED_TEST_TIME     => 30,
    SERVER_RESPONDING                => 1,
    SERVER_NOT_RESPONDING            => 0,
};


# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    return {
        pluginName          => '@PLUGIN_KEY@',
        pluginVersion       => '@PLUGIN_VERSION@',
        configFields        => ['config_name', 'configuration_name', 'serverconfig', 'config'],
        configLocations     => ['jboss_cfgs', 'ec_plugin_cfgs'],
        defaultConfigValues => {}
    };
}

# Auto-generated method for the connection check.
# Add your code into this method and it will be called when configuration is getting created.
# $self - reference to the plugin object
# $p - step parameters
# $sr - StepResult object
# Parameter: config
# Parameter: desc
# Parameter: credential
# Parameter: java_opts
# Parameter: jboss_url
# Parameter: log_level
# Parameter: scriptphysicalpath
# Parameter: test_connection
# Parameter: test_connection_res

sub checkConnection {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    my $configValues = $context->getConfigValues()->asHashref();
    logInfo("Config values are: ", $configValues);

    eval {
        # Use $configValues to check connection, e.g. perform some ping request
        # my $client = Client->new($configValues); $client->ping();
        my $password = $configValues->{password};
        if ($password ne 'secret') {
            # die "Failed to test connection - dummy check connection error\n";
        }
        1;
    } or do {
        my $err = $@;
        # Use this property to surface the connection error details in the CD server UI
        $sr->setOutcomeProperty("/myJob/configError", $err);
        $sr->apply();
        die $err;
    };
}
## === check connection ends ===

# Auto-generated method for the procedure CheckDeployStatus/CheckDeployStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: hosts
# Parameter: serversgroup
# Parameter: servers
# Parameter: criteria
# Parameter: wait_time

# $sr - StepResult object
sub checkDeployStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    => $PROJECT_NAME,
        plugin_name     => $PLUGIN_NAME,
        plugin_key      => $PLUGIN_KEY,
        flowpdf         => $self
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
}
# Auto-generated method for the procedure CheckHostControllerStatus/CheckHostControllerStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: hostcontroller_name
# Parameter: wait_time
# Parameter: criteria

# $sr - StepResult object
sub checkHostControllerStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);


    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         =>  $self
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
# Auto-generated method for the procedure CheckServerGroupStatus/CheckServerGroupStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time
# Parameter: criteria

# $sr - StepResult object
sub checkServerGroupStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $PARAM_CRITERIA_STARTED = 'STARTED';
    my $PARAM_CRITERIA_STOPPED = 'STOPPED';
    my $PARAM_CRITERIA_DISABLED = 'DISABLED';
    my $PARAM_CRITERIA_STOPPED_OR_DISABLED = 'STOPPED_OR_DISABLED';

    my %PARAM_CRITERIA_LABELS = (
        $PARAM_CRITERIA_STARTED             => "STARTED",
        $PARAM_CRITERIA_STOPPED             => "STOPPED",
        $PARAM_CRITERIA_DISABLED            => "DISABLED",
        $PARAM_CRITERIA_STOPPED_OR_DISABLED => "STOPPED or DISABLED",
    );

    my $STATUS_STOPPED = 'STOPPED'; # stopped status for servers with auto-start true
    my $STATUS_DISABLED = 'DISABLED'; # stopped status for servers with auto-start false
    my $STATUS_STARTED = 'STARTED'; # started status for servers
    my $SLEEP_TIME = 5;

    my $OUTPUT_PARAM_CRITERIA_MET = 'servergroupstatus';
    my $CRITERIA_MET_TRUE = 'TRUE';
    my $CRITERIA_MET_FALSE = 'FALSE';

    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        plugin_name  => $PLUGIN_NAME,
        plugin_key   => $PLUGIN_KEY,
        flowpdf      =>  $self
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'criteria',
        'wait_time'
    );

    $jboss->bail_out("Required parameter 'serversgroup' is not provided") unless $params->{serversgroup};
    $jboss->bail_out("Required parameter 'criteria' is not provided") unless $params->{criteria};

    my $param_criteria = $params->{criteria};
    if (!$PARAM_CRITERIA_LABELS{$param_criteria}) {
        $jboss->bail_out(
            sprintf(
                "Unsupported option '%s' provided for parameter 'criteria' (supported options are: %s)",
                $param_criteria,
                join(", ", keys %PARAM_CRITERIA_LABELS)
            )
        );
    }
    my $param_criteria_label = $PARAM_CRITERIA_LABELS{$param_criteria};

    $params->{wait_time} = $jboss->trim($params->{wait_time});
    my $wait_time = undef;
    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time should be a positive integer");
        }
    }
    # Seems like servers in one group could be on different hosts
    # So we have to list all hosts

    my $server_group_name = $params->{serversgroup};
    my $done = 0;
    my $time_start = time();

    my $result = {
        error                      => 0,
        msg                        => '',
        $OUTPUT_PARAM_CRITERIA_MET => ''
    };

    while (!$done) {
        # wait time is not defined, it's empty so, one loop iteration only.
        my $time_diff = time() - $time_start;
        if (!defined $wait_time) {
            $done = 1;
        }
        elsif ($wait_time && $time_diff >= $wait_time) {
            $done = 1;
            last;
        }
        # otherwise we will wait forever.

        my ($servers, $states_ref) = $jboss->get_servergroup_status($server_group_name);
        my @states = @$states_ref;

        if (!@states) {
            $jboss->bail_out("Server group '$server_group_name' doesn't exist or empty");
        }

        my @matching_states;
        if ($param_criteria eq $PARAM_CRITERIA_STARTED) {
            @matching_states = grep {$_ eq $STATUS_STARTED} @states;
        }
        elsif ($param_criteria eq $PARAM_CRITERIA_STOPPED) {
            @matching_states = grep {$_ eq $STATUS_STOPPED} @states;
        }
        elsif ($param_criteria eq $PARAM_CRITERIA_DISABLED) {
            @matching_states = grep {$_ eq $STATUS_DISABLED} @states;
        }
        elsif ($param_criteria eq $PARAM_CRITERIA_STOPPED_OR_DISABLED) {
            @matching_states = grep {$_ eq $STATUS_STOPPED || $_ eq $STATUS_DISABLED} @states;
        }
        else {
            $jboss->bail_out(
                sprintf(
                    "Unsupported option '%s' provided for parameter 'criteria' (supported options are: %s)",
                    $param_criteria,
                    join(", ", keys %PARAM_CRITERIA_LABELS)
                )
            );
        }

        my $criteria_is_met = 1 if scalar @matching_states == scalar @states;

        my %unique_states = map {$_ => 1} @states;
        my $unique_states_str = join(', ', keys %unique_states);

        $jboss->set_property('server_group_status', $unique_states_str);

        $jboss->log_info("Summarry log for servers within '$server_group_name' server group:");
        for my $host_name (keys %$servers) {
            for my $server_name (keys %{$servers->{$host_name}}) {
                my $server_status = $servers->{$host_name}->{$server_name}->{status};
                $jboss->log_info("Server '$server_name' on host '$host_name' has status '$server_status'");
            }
        }

        if ($criteria_is_met) {
            $jboss->log_info("Criteria '$param_criteria_label' is met on this iteration. Servers in '$server_group_name' server group have statuses $unique_states_str");
            $result->{msg} = "Criteria '$param_criteria_label' is met.\nServers in '$server_group_name' server group have statuses $unique_states_str";
            $result->{error} = 0;
            $result->{$OUTPUT_PARAM_CRITERIA_MET} = $CRITERIA_MET_TRUE;
            $done = 1;
            last;
        }
        else {
            $jboss->log_info("Criteria '$param_criteria_label' is not met on this iteration. Servers in '$server_group_name' server group have statuses $unique_states_str");
            $result->{msg} = "Criteria '$param_criteria_label' is not met.\nServers in '$server_group_name' server group have statuses $unique_states_str";
            $result->{error} = 1;
            $result->{$OUTPUT_PARAM_CRITERIA_MET} = $CRITERIA_MET_FALSE;
        }

        sleep $SLEEP_TIME;
    }

    if ($result->{$OUTPUT_PARAM_CRITERIA_MET}) {
        $jboss->set_output_parameter($OUTPUT_PARAM_CRITERIA_MET, $result->{$OUTPUT_PARAM_CRITERIA_MET});
    }

    if ($result->{error}) {
        $jboss->error($result->{msg});
    }
    else {
        $jboss->success($result->{msg});
    }
    return 1;
}
# Auto-generated method for the procedure CheckServerStatus/CheckServerStatus
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: url_check
# Parameter: host
# Parameter: server
# Parameter: criteria
# Parameter: wait_time

# $sr - StepResult object
sub checkServerStatus {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         =>  $self
    );

    my $params = $jboss->get_params_as_hashref(qw/
        host
        server
        criteria
        url_check
        wait_time
    /);

    my $creds = $jboss->get_plugin_configuration();
    if ($params->{url_check}) {
        my $check_result;

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
    $jboss->run_commands_until_done({
        sleep_time => 5,
        time_limit => $params->{wait_time}
    }, sub {
        eval {
            $launch_type = $jboss->get_launch_type();
        };
        $jboss->log_info("launch type is '" . ($launch_type ? $launch_type : "unknown") . "'");
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
            # we assume that server is not running in case we cannot retrieve launch type
            $jboss->success("Server is not running. Criteria met");
            return;
        }
        else {
            $jboss->error("Unknown launch type. Criteria not met.");
            return;
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
    my $state_criteria_met;
    $jboss->run_commands_until_done({
        sleep_time => 5,
        time_limit => $params->{wait_time},
    }, sub {
        %result = $jboss->run_command($command);
        $state_criteria_met = is_criteria_met($jboss, \%result, $launch_type, $params->{criteria});
        return $state_criteria_met;
    });

    if ($state_criteria_met) {
        $jboss->success("Criteria '" .$params->{criteria} . "' met");
        return;
    }
    else {
        $jboss->error("Criteria '" .$params->{criteria} . "' not met.");
        return;
    }
}
# Auto-generated method for the procedure CreateDatasource/CreateDatasource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: application_name
# Parameter: connectionURL
# Parameter: driverClass
# Parameter: jndiName
# Parameter: driverName
# Parameter: profile
# Parameter: ds_credential
# Parameter: enabled

# $sr - StepResult object
sub createDatasource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         =>  $self
    );
    my $params = $jboss->get_params_as_hashref(qw/
        serverconfig
        profile
        connectionURL
        driverClass
        jndiName
        driverName
        ds_credential
        enabled
        application_name
    /);

    # See example with options
    # https://docs.jboss.org/author/display/AS71/CLI+Recipes#CLIRecipes-AddaNewDatasource

    my $command = 'data-source add';
    $command .= qq| --connection-url=$params->{connectionURL} |;
    $command .= qq| --datasource-class=$params->{driverClass} |;
    $command .= qq| --jndi-name=$params->{jndiName} |;
    $command .= qq| --driver-name=$params->{driverName} |;
    $command .= qq| --name=$params->{application_name} |;

    # profile for domain mode
    if ($params->{profile}) {
        $command .= qq| --profile=$params->{profile} |;
    }

    my $is_6_0_0 = $jboss->is_6_0_0();
    $jboss->log_debug("Is JBoss 6.0.0: $is_6_0_0");

    # on JBoss 6.0.0 there is no --enabled option and command fails.
    # this workaround adds --enabled flag only when JBoss version is 6.0.0.

    if (!$is_6_0_0) {
        if ($params->{enabled}) {
            $command .= ' --enabled=true ';
        }
        else {
            $command .= ' --enabled=false ';
        }
    }

    if ($params->{ds_credential}) {
        my $ec = $context->getEc();
        my $xpath = $ec->getFullCredential('ds_credential');
        my $userName = $xpath->findvalue("//userName");
        my $password = $xpath->findvalue("//password");

        if ($userName) {
            $command .= qq| --user-name=$userName |;
        }

        if ($password) {
            $command .= qq| --password=$password |;
        }
    }
    my %result = $jboss->run_command($command);
    if ($is_6_0_0 && $params->{enabled}) {
        my $ds_profile = $params->{profile};
        $ds_profile ||= 'default';
        my $enable_command = sprintf 'data-source enable --name=%s --profile=%s', $params->{application_name}, $ds_profile;
        $jboss->out("Enabling datasource...");
        my %enable_res = $jboss->run_command($enable_command);
        unless ($enable_res{code}) {
            $jboss->out("Enabled.");
        }
        else {
            $jboss->bail_out("Failed to enable datasource.");
        }
    }
    $jboss->process_response(%result);
}
# Auto-generated method for the procedure CreateOrUpdateDataSource/CreateOrUpdateDataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: jndiName
# Parameter: jdbcDriverName
# Parameter: connectionUrl
# Parameter: dataSourceConnection_credential
# Parameter: enabled
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateDataSource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self
    );

    $jboss->{hide_password} = 1;

    my $params = $jboss->get_params_as_hashref(qw/
        dataSourceName
        jndiName
        jdbcDriverName
        connectionUrl
        dataSourceConnection_credential
        enabled
        profile
        additionalOptions
    /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_jndi_name = $params->{jndiName};
    my $param_jdbc_driver_name = $params->{jdbcDriverName};
    my $param_connection_url = $params->{connectionUrl};
    my $param_data_source_connection_credentials = $params->{dataSourceConnection_credential};
    my $param_enabled = $params->{enabled};
    my $param_profile = $params->{profile};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_data_source_name) {
        $jboss->bail_out("Required parameter 'dataSourceName' is not provided");
    }
    if (!$param_jndi_name) {
        $jboss->bail_out("Required parameter 'jndiName' is not provided");
    }
    if (!$param_jdbc_driver_name) {
        $jboss->bail_out("Required parameter 'jdbcDriverName' is not provided");
    }
    if (!defined $param_enabled) {
        $jboss->bail_out("Required parameter 'enabled' is not provided");
    }

    my $param_user_name;
    my $param_password;
    if ($param_data_source_connection_credentials) {
        my $xpath = $jboss->ec()->getFullCredential($param_data_source_connection_credentials);
        $param_user_name = $xpath->findvalue("//userName");
        $param_password = $xpath->findvalue("//password");
    }
    else {
        $jboss->bail_out("Required parameter 'dataSourceConnection_credential' is not provided");
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

    my $profile_prefix = $jboss_is_domain ? "/profile=$param_profile" : "";

    ########
    # check jboss version
    ########
    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};

    if ($product_version =~ m/^6/ || $product_version =~ m/^7\.0/) {
        if (!$param_connection_url) {
            $jboss->bail_out("Required parameter 'connectionUrl' is not provided (parameter required for JBoss EAP 6.X and 7.0)");
        }
    }

    ########
    # check if data source with specified name exists
    ########
    my @all_data_sources;
    if ($jboss_is_domain) {
        @all_data_sources = @{ get_all_data_sources_domain(jboss => $jboss, profile => $param_profile) };
    }
    else {
        @all_data_sources = @{ get_all_data_sources_standalone(jboss => $jboss) };
    }
    my %all_data_sources_hash = map {$_ => 1} @all_data_sources;

    my $data_source_exists = 1 if $all_data_sources_hash{$param_data_source_name};

    if ($data_source_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("Data source '$param_data_source_name' exists");

        $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:read-resource(recursive=false)";
        my $data_source_resource = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );

        my $existing_jndi_name = $data_source_resource->{'jndi-name'};
        my $existing_user_name = $data_source_resource->{'user-name'};
        my $existing_password = $data_source_resource->{'password'};

        my @updated_items;
        my @update_responses;

        if ($existing_jndi_name ne $param_jndi_name) {
            ########
            # jndi name differs
            ########
            $jboss->log_info("JNDI name differs and to be updated: current '$existing_jndi_name' VS specified in parameters '$param_jndi_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=jndi-name,value=$param_jndi_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "jndi name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_user_name ne $param_user_name) {
            ########
            # user name differs
            ########
            $jboss->log_info("User name differs and to be updated: current '$existing_user_name' VS specified in parameters '$param_user_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=user-name,value=$param_user_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "user name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_password ne $param_password) {
            ########
            # password differs
            ########
            $jboss->log_info("Password differs and to be updated");

            $cli_command = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:write-attribute(name=password,value=$param_password)";

            $jboss->{silent} = 1;
            my %result = $jboss->run_command($cli_command);
            $jboss->{silent} = 0;

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "password";
                push @update_responses, $result{stdout};
            }
        }

        if (@updated_items) {
            my $updated_items_str = join(", ", @updated_items);
            my $summary = "Data source '$param_data_source_name' has been updated successfully by new $updated_items_str.";

            my @unique_update_responses = do {
                my %seen;
                grep {$_ && !$seen{$_}++} @update_responses
            };
            my $unique_update_responses_str = join("\n", @unique_update_responses);

            foreach my $response (@unique_update_responses) {
                if ($response && is_reload_or_restart_required($response)) {
                    $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                    $jboss->warning();
                    last;
                }
            }
            $summary .= "\nJBoss replies on update operations (unique set, refer to the logs for more information):\n" . $unique_update_responses_str if $unique_update_responses_str;
            $jboss->set_property(summary => $summary);
            return;
        }
        else {
            ########
            # updatable attributes match
            ########
            $jboss->log_info("Updatable attributes match - no updates will be performed");
            $jboss->set_property(summary => "Data source '$param_data_source_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("Data source '$param_data_source_name' does not exist - to be created");

        my $command_add_data_source = qq/data-source add /;
        if ($jboss_is_domain) {
            $command_add_data_source .= qq/ --profile=$param_profile /;
        }
        $command_add_data_source .= qq/ --name=$param_data_source_name --jndi-name=$param_jndi_name --driver-name=$param_jdbc_driver_name /;
        if ($param_user_name) {
            $command_add_data_source .= qq| --user-name=$param_user_name |;
        }
        if ($param_password) {
            $command_add_data_source .= qq| --password=$param_password |;
        }
        #there is no --enabled parameter for 'data-source add' command in JBoss EAP 6.0, to be enabled after creation if needed
        if ($product_version !~ m/^6\.0/) {
            if ($param_enabled) {
                $command_add_data_source .= qq| --enabled=true |;
            }
            else {
                $command_add_data_source .= qq| --enabled=false |;
            }
        }
        if ($param_connection_url) {
            $command_add_data_source .= qq| --connection-url==$param_connection_url |;
        }
        if ($param_additional_options) {
            my $escaped_additional_options = escape_additional_options($param_additional_options);
            $command_add_data_source .= qq/ $escaped_additional_options /;
        }

        my %result = run_command_with_exiting_on_error(command => $command_add_data_source, jboss => $jboss);

        my $summary = "Data source '$param_data_source_name' has been added successfully";
        if ($result{stdout} && is_reload_or_restart_required($result{stdout})) {
            $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
            $jboss->warning();
            $summary .= "\nJBoss reply: " . $result{stdout};
        }

        ########
        # there is no --enabled parameter for 'data-source add' command in JBoss EAP 6.0, let's enable it separately if needed
        ########
        if ($param_enabled && $product_version =~ m/^6\.0/) {
            $jboss->log_info("Enabling data source for 6.0 due to known issue (enable=true within data-source add command does not take affect, data source created and disabled)");

            my $command_enable_data_source = "$profile_prefix/subsystem=datasources/data-source=$param_data_source_name/:enable";
            run_command_with_exiting_on_error(command => $command_enable_data_source, jboss => $jboss);
        }

        $jboss->set_property(summary => $summary);

        return;
    }
}
# Auto-generated method for the procedure CreateOrUpdateJMSQueue/CreateOrUpdateJMSQueue
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: queueName
# Parameter: jndiNames
# Parameter: profile
# Parameter: durable
# Parameter: messageSelector
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateJMSQueue {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';


    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
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
    my $json;

    if (!$param_queue_name) {
        $jboss->bail_out("Required parameter 'queueName' is not provided");
    }
    if (!$param_jndi_names) {
        $jboss->bail_out("Required parameter 'jndiNames' is not provided");
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
    if ($product_version =~ m/^6/) {
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
        ########
        # update logic
        ########
        $jboss->log_info("JMS queue '$param_queue_name' exists");

        my $existing_jndi_names = $jms_queue_resources->{$param_queue_name}->{entries};
        my @specified_jndi_names = split /,/, $param_jndi_names;

        my @sorted_existing_jndi_names = sort @$existing_jndi_names;
        my @sorted_specified_jndi_names = sort @specified_jndi_names;

        if ("@sorted_existing_jndi_names" ne "@sorted_specified_jndi_names") {
            ########
            # jndi names differ
            ########
            $jboss->log_info("JNDI names differ and to be updated: current [@sorted_existing_jndi_names] (sorted) VS specified in parameters [@sorted_specified_jndi_names] (sorted)");

            my $jndi_names_wrapped = join ',', map {qq/"$_"/} @specified_jndi_names;
            if ($jboss_is_domain) {
                $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }
            else {
                $cli_command = "/$subsystem_part/$provider_part/jms-queue=$param_queue_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                if ($result{stdout} && $result{stdout} =~ m/Attribute entries is not writable/s) {
                    $jboss->log_error("Update of JNDI names for JMS queue cannot be performed for this version of JBoss ($product_version). Attribute entries (jndi names) is not writable");
                    my $summary .= "Update of JNDI names for JMS queue '$param_queue_name' cannot be performed for this version of JBoss ($product_version).";
                    $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
                    $jboss->set_property(summary => $summary);
                    $jboss->error();
                    exit 1;
                }
                else {
                    $jboss->process_response(%result);
                    exit 1;
                }
            }
            else {
                $jboss->process_response(%result);
                my $summary = "JMS queue '$param_queue_name' has been updated successfully by new jndi names.";
                if ($result{stdout}) {
                    my $reload_or_restart_required;
                    if ($result{stdout} =~ m/"process-state"\s=>\s"reload-required"/gs
                        || $result{stdout} =~ m/"process-state"\s=>\s"restart-required"/gs) {
                        $reload_or_restart_required = 1;
                    }
                    if ($reload_or_restart_required) {
                        $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                        $jboss->warning();
                    }
                    $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
                }

                $jboss->set_property(summary => $summary);
            }
            return;
        }
        else {
            ########
            # jndi names match
            ########
            $jboss->log_info("JNDI names match - no updates will be performed");
            $jboss->set_property(summary => "JMS queue '$param_queue_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("JMS queue '$param_queue_name' does not exist - to be created");

        $cli_command = qq/jms-queue add /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
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

        $jboss->set_property(summary => "JMS queue '$param_queue_name' has been added successfully");
        return;
    }
}
# Auto-generated method for the procedure CreateOrUpdateJMSTopic/CreateOrUpdateJMSTopic
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: topicName
# Parameter: jndiNames
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateJMSTopic {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';


    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        topicName
        jndiNames
        profile
        additionalOptions
    /);

    my $param_topic_name = $params->{topicName};
    my $param_jndi_names = $params->{jndiNames};
    my $param_profile = $params->{profile};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_topic_name) {
        $jboss->bail_out("Required parameter 'topicName' is not provided");
    }
    if (!$param_jndi_names) {
        $jboss->bail_out("Required parameter 'jndiNames' is not provided");
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
    if ($product_version =~ m/^6/) {
        $subsystem_part = "subsystem=messaging";
        $provider_part = "hornetq-server=default";
    }

    ########
    # check if jms topic with specified name exists
    ########
    if ($jboss_is_domain) {
        $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }
    else {
        $cli_command = "/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_topic_resources = $json->{result};
    my $jms_topic_exists = 1 if $jms_topic_resources->{$param_topic_name};

    if ($jms_topic_exists) {
        ########
        # update logic
        ########
        $jboss->log_info("JMS topic '$param_topic_name' exists");

        my $existing_jndi_names = $jms_topic_resources->{$param_topic_name}->{entries};
        my @specified_jndi_names = split /,/, $param_jndi_names;

        my @sorted_existing_jndi_names = sort @$existing_jndi_names;
        my @sorted_specified_jndi_names = sort @specified_jndi_names;

        if ("@sorted_existing_jndi_names" ne "@sorted_specified_jndi_names") {
            ########
            # jndi names differ
            ########
            $jboss->log_info("JNDI names differ and to be updated: current [@sorted_existing_jndi_names] (sorted) VS specified in parameters [@sorted_specified_jndi_names] (sorted)");

            my $jndi_names_wrapped = join ',', map {qq/"$_"/} @specified_jndi_names;
            if ($jboss_is_domain) {
                $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part/jms-topic=$param_topic_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }
            else {
                $cli_command = "/$subsystem_part/$provider_part/jms-topic=$param_topic_name/:write-attribute(name=entries,value=[$jndi_names_wrapped])";
            }

            my %result = run_command_with_exiting_on_error(
                command => $cli_command,
                jboss   => $jboss
            );

            my $summary = "JMS topic '$param_topic_name' has been updated successfully by new jndi names";
            if ($result{stdout}) {
                my $reload_or_restart_required;
                if ($result{stdout} =~ m/"process-state"\s=>\s"reload-required"/gs
                    || $result{stdout} =~ m/"process-state"\s=>\s"restart-required"/gs) {
                    $reload_or_restart_required = 1;
                }
                if ($reload_or_restart_required) {
                    $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                    $jboss->warning();
                }
                $summary .= "\nJBoss reply: " . $result{stdout} if $result{stdout};
            }

            $jboss->set_property(summary => $summary);
            return;
        }
        else {
            ########
            # jndi names match
            ########
            $jboss->log_info("JNDI names match - no updates will be performed");
            $jboss->set_property(summary => "JMS topic '$param_topic_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("JMS topic '$param_topic_name' does not exist - to be created");

        $cli_command = qq/jms-topic add /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --topic-address=$param_topic_name --entries=$param_jndi_names /;

        if ($param_additional_options) {
            my $escaped_additional_options = $jboss->escape_string($param_additional_options);
            $cli_command .= qq/ $escaped_additional_options /;
        }

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS topic '$param_topic_name' has been added successfully");
        return;
    }

}
# Auto-generated method for the procedure CreateOrUpdateXADataSource/CreateOrUpdateXADataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: jndiName
# Parameter: jdbcDriverName
# Parameter: xaDataSourceProperties
# Parameter: dataSourceConnection_credential
# Parameter: enabled
# Parameter: profile
# Parameter: additionalOptions

# $sr - StepResult object
sub createOrUpdateXADataSource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self
    );

    $jboss->{hide_password} = 1;

    my $params = $jboss->get_params_as_hashref(qw/
        dataSourceName
        jndiName
        jdbcDriverName
        xaDataSourceProperties
        dataSourceConnection_credential
        enabled
        profile
        additionalOptions
    /);

    my $param_data_source_name = $params->{dataSourceName};
    my $param_jndi_name = $params->{jndiName};
    my $param_jdbc_driver_name = $params->{jdbcDriverName};
    my $param_xa_data_source_properties = $params->{xaDataSourceProperties};
    my $param_data_source_connection_credentials = $params->{dataSourceConnection_credential};
    my $param_enabled = $params->{enabled};
    my $param_profile = $params->{profile};
    my $param_additional_options = $params->{additionalOptions};

    my $cli_command;
    my $json;

    if (!$param_data_source_name) {
        $jboss->bail_out("Required parameter 'dataSourceName' is not provided");
    }
    if (!$param_jndi_name) {
        $jboss->bail_out("Required parameter 'jndiName' is not provided");
    }
    if (!$param_jdbc_driver_name) {
        $jboss->bail_out("Required parameter 'jdbcDriverName' is not provided");
    }
    if (!$param_xa_data_source_properties) {
        $jboss->bail_out("Required parameter 'xaDataSourceProperties' is not provided");
    }
    if (!defined $param_enabled) {
        $jboss->bail_out("Required parameter 'enabled' is not provided");
    }

    my $param_user_name;
    my $param_password;
    if ($param_data_source_connection_credentials) {
        my $xpath = $jboss->ec()->getFullCredential($param_data_source_connection_credentials);
        $param_user_name = $xpath->findvalue("//userName");
        $param_password = $xpath->findvalue("//password");
    }
    else {
        $jboss->bail_out("Required parameter 'dataSourceConnection_credential' is not provided");
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

    my $profile_prefix = $jboss_is_domain ? "/profile=$param_profile" : "";

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
        ########
        # update logic
        ########
        $jboss->log_info("XA data source '$param_data_source_name' exists");

        $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:read-resource(recursive=false)";
        my $xa_data_source_resource = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );

        my $existing_jndi_name = $xa_data_source_resource->{'jndi-name'};
        my $existing_user_name = $xa_data_source_resource->{'user-name'};
        my $existing_password = $xa_data_source_resource->{'password'};

        my @updated_items;
        my @update_responses;

        if ($existing_jndi_name ne $param_jndi_name) {
            ########
            # jndi name differs
            ########
            $jboss->log_info("JNDI name differs and to be updated: current '$existing_jndi_name' VS specified in parameters '$param_jndi_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=jndi-name,value=$param_jndi_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "jndi name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_user_name ne $param_user_name) {
            ########
            # user name differs
            ########
            $jboss->log_info("User name differs and to be updated: current '$existing_user_name' VS specified in parameters '$param_user_name'");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=user-name,value=$param_user_name)";

            my %result = $jboss->run_command($cli_command);

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "user name";
                push @update_responses, $result{stdout};
            }
        }

        if ($existing_password ne $param_password) {
            ########
            # password differs
            ########
            $jboss->log_info("Password differs and to be updated");

            $cli_command = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:write-attribute(name=password,value=$param_password)";

            $jboss->{silent} = 1;
            my %result = $jboss->run_command($cli_command);
            $jboss->{silent} = 0;

            if ($result{code}) {
                $jboss->process_response(%result);
                exit 1;
            }
            else {
                $jboss->process_response(%result);

                push @updated_items, "password";
                push @update_responses, $result{stdout};
            }
        }

        if (@updated_items) {
            my $updated_items_str = join(", ", @updated_items);
            my $summary = "XA data source '$param_data_source_name' has been updated successfully by new $updated_items_str.";

            my @unique_update_responses = do {
                my %seen;
                grep {$_ && !$seen{$_}++} @update_responses
            };
            my $unique_update_responses_str = join("\n", @unique_update_responses);

            foreach my $response (@unique_update_responses) {
                if ($response && is_reload_or_restart_required($response)) {
                    $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
                    $jboss->warning();
                    last;
                }
            }
            $summary .= "\nJBoss replies on update operations (unique set, refer to the logs for more information):\n" . $unique_update_responses_str if $unique_update_responses_str;
            $jboss->set_property(summary => $summary);
            return;
        }
        else {
            ########
            # updatable attributes match
            ########
            $jboss->log_info("Updatable attributes match - no updates will be performed");
            $jboss->set_property(summary => "XA data source '$param_data_source_name' is up-to-date");
            return;
        }
    }
    else {
        ########
        # create logic
        ########
        $jboss->log_info("XA data source '$param_data_source_name' does not exist - to be created");

        my $command_add_xa_data_source = qq/xa-data-source add /;
        if ($jboss_is_domain) {
            $command_add_xa_data_source .= qq/ --profile=$param_profile /;
        }
        $command_add_xa_data_source .= qq/ --name=$param_data_source_name --jndi-name=$param_jndi_name --driver-name=$param_jdbc_driver_name /;
        if ($param_user_name) {
            $command_add_xa_data_source .= qq| --user-name=$param_user_name |;
        }
        if ($param_password) {
            $command_add_xa_data_source .= qq| --password=$param_password |;
        }
        if ($param_enabled) {
            $command_add_xa_data_source .= qq| --enabled=true |;
        }
        else {
            $command_add_xa_data_source .= qq| --enabled=false |;
        }
        $command_add_xa_data_source .= qq| --xa-datasource-properties={$param_xa_data_source_properties} |;

        if ($param_additional_options) {
            my $escaped_additional_options = escape_additional_options($param_additional_options);
            $command_add_xa_data_source .= qq/ $escaped_additional_options /;
        }

        my %result = run_command_with_exiting_on_error(command => $command_add_xa_data_source, jboss => $jboss);

        my $summary = "XA data source '$param_data_source_name' has been added successfully";
        if ($result{stdout} && is_reload_or_restart_required($result{stdout})) {
            $jboss->log_warning("Some servers require reload or restart, please check the JBoss response");
            $jboss->warning();
            $summary .= "\nJBoss reply: " . $result{stdout};
        }

        ########
        # workaround for 6.0 (enable=true within xa-data-source add command does not take affect, data source created and disabled)
        ########
        if ($param_enabled) {
            my $version = $jboss->get_jboss_server_version();
            my $product_version = $version->{product_version};
            if ($product_version =~ m/^6\.0/) {
                $jboss->log_info("Enabling data source within 6.0 separately due to known issue (enable=true within xa-data-source add command does not take affect, data source created and disabled)");

                my $command_enable_data_source = "$profile_prefix/subsystem=datasources/xa-data-source=$param_data_source_name/:enable";
                run_command_with_exiting_on_error(command => $command_enable_data_source, jboss => $jboss);
            }
        }

        $jboss->set_property(summary => $summary);

        return;
    }
}
# Auto-generated method for the procedure DeleteDatasource/DeleteDatasource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: datasource_name
# Parameter: profile

# $sr - StepResult object
sub deleteDatasource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         =>  $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        serverconfig
        datasource_name
        profile
    /);

    my $command = 'data-source remove ';
    $command .= qq| --name=$params->{datasource_name} |;

    if ($params->{profile}) {
        $command .= qq| --profile=$params->{profile} |;
    }

    my %result = $jboss->run_command($command);

    $jboss->process_response(%result);
}
# Auto-generated method for the procedure DeployApp/DeployApp
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: warphysicalpath
# Parameter: appname
# Parameter: runtimename
# Parameter: assignallservergroups
# Parameter: assignservergroups
# Parameter: force
# Parameter: additional_options

# $sr - StepResult object
sub deployApp {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        plugin_name  => $PLUGIN_NAME,
        plugin_key   => $PLUGIN_KEY,
        flowpdf      => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        scriptphysicalpath
        warphysicalpath
        appname
        runtimename
        force
        assignservergroups
        assignallservergroups
        additional_options
    /);

    my $source_is_url = 0;
    # e.g. the following format we accept the following:
    # '--url=https://github.com/electric-cloud/hello-world-war/raw/master/dist/hello-world.war'
    if ($params->{warphysicalpath} =~ /^--url=/) {
        $jboss->log_info("Source with deployment is URL (such option available for EAP 7 and later versions): '$params->{warphysicalpath}'");
        $source_is_url = 1;
    }
    else {
        $jboss->log_info("Source with deployment is filepath: '$params->{warphysicalpath}'");
    }

    my $is_domain = 0;
    my $launch_type = $jboss->get_launch_type();
    $is_domain = 1 if $launch_type eq 'domain';

    if (!$jboss->{dryrun} && !$source_is_url && !-e $params->{warphysicalpath}) {
        $jboss->bail_out("File '$params->{warphysicalpath}' doesn't exists");
    }

    ######################
    # generate jboss cli command
    ######################

    my $command = qq/deploy /;

    if ($source_is_url) {
        $command .= qq/ $params->{warphysicalpath} /;
    }
    else {
        $command .= qq/ "$params->{warphysicalpath}" /;
    }

    if ($params->{appname}) {
        $command .= qq/ --name=$params->{appname} /;
    }

    if ($params->{runtimename}) {
        $command .= qq/ --runtime-name=$params->{runtimename} /;
    }

    if ($params->{force}) {
        $command .= ' --force ';
    }

    if ($is_domain && !$params->{force}) {
        if ($params->{assignallservergroups}) {
            $command .= ' --all-server-groups ';
        }
        elsif ($params->{assignservergroups}) {
            $command .= qq/ --server-groups=$params->{assignservergroups} /;
        }
    }

    if ($params->{additional_options}) {
        $params->{additional_options} = $jboss->escape_string($params->{additional_options});
        $command .= ' ' . $params->{additional_options} . ' ';
    }

    ######################
    # generate possible job step summary in case of successful completion
    ######################

    # expected source - filepath or url (url without '--url=' anchor)
    my $expected_source = $params->{warphysicalpath};
    if ($source_is_url) {
        $expected_source =~ s/^--url=//;
    }
    # expected name of the deployment
    my $expected_appname;
    if ($params->{appname}) {
        $expected_appname = $params->{appname};
    }
    elsif ($source_is_url) {
        # retrieve filename for url
        $expected_appname = (URI->new($expected_source)->path_segments)[-1];
    }
    else {
        # retrieve filename for filepath
        $expected_appname = fileparse($expected_source);
    }
    # job step summary for in case of successful completion
    my $job_step_summary_for_success_completion = "Application '$expected_appname' has been successfully deployed from '$expected_source'";

    ######################
    # run jboss cli command and process response
    ######################
    my %result = $jboss->run_command($command);

    # job step summary to be updated by this message within process_response only in case if this process_response will decide to that response is success..
    $jboss->{success_message} = $job_step_summary_for_success_completion;

    if ($result{stdout}) {
        $jboss->out("Command output: $result{stdout}");
    }

    $jboss->process_response(%result);
}
# Auto-generated method for the procedure DeployApplication/DeployApplication
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: applicationContentSourcePath
# Parameter: deploymentName
# Parameter: runtimeName
# Parameter: enabledServerGroups
# Parameter: disabledServerGroups
# Parameter: additionalOptions

# $sr - StepResult object
sub deployApplication {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
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
# Auto-generated method for the procedure DisableDeploy/DisableDeploy
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: assignservergroups

# $sr - StepResult object
sub disableDeploy {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         => $self,
    );
    my $params = $jboss->get_params_as_hashref(qw/
        appname
        assignservergroups
    /);

    my $command = "undeploy --name=$params->{appname} --keep-content";
    my $launch_type = $jboss->get_launch_type();
    if ($launch_type eq 'domain' && !$params->{assignservergroups}) {
        $jboss->bail_out('When JBoss server is launched as domain, "Server groups" parameter is mandatory');
    }

    if ($launch_type eq 'domain') {
        $command .= " --server-groups=$params->{assignservergroups}";
    }
    $jboss->{success_message} = "Application $params->{appname} has been successfully disabled.";
    $jboss->process_response($jboss->run_command($command));
}
# Auto-generated method for the procedure EnableDeploy/EnableDeploy
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: assignservergroups

# $sr - StepResult object
sub enableDeploy {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         =>  $self,
    );
    my $params = $jboss->get_params_as_hashref(qw/
        appname
        assignservergroups
    /);

    my $command = "deploy --name=$params->{appname}";
    my $launch_type = $jboss->get_launch_type();
    if ($launch_type eq 'domain' && !$params->{assignservergroups}) {
        $jboss->bail_out('When JBoss server is launched as domain, "Server groups" parameter is mandatory');
    }

    if ($launch_type eq 'domain') {
        $command .= " --server-groups=$params->{assignservergroups}";
    }
    $jboss->{success_message} = "Application $params->{appname} has been successfully enabled.";
    $jboss->process_response($jboss->run_command($command));
}
# Auto-generated method for the procedure GetEnvInfo/GetEnvInfo
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: informationType
# Parameter: informationTypeContext
# Parameter: additionalOptions

# $sr - StepResult object
sub getEnvInfo {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    $jboss->{hide_password} = 1;

    my $params = $jboss->get_params_as_hashref(qw/
        informationType
        informationTypeContext
        additionalOptions
    /);

    my $param_information_type = $params->{informationType};
    my $param_information_type_context = $params->{informationTypeContext};
    my $param_additional_options = $params->{additionalOptions};

    my $property_path = "envInfo";
    my $env_info;

    my $cli_command;
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
        $jboss->bail_out("Unknown JBoss launch type: '$launch_type'");
    }
    my $jboss_is_domain = 1 if $launch_type eq "domain";

    $jboss->log_info("=======Started: getting environment information, information type - '$param_information_type'=======");

    if ($param_information_type eq "systemDump") {
        $env_info = get_env_info_system_dump(
            jboss                 => $jboss,
            additional_parameters => $param_additional_options
        );
    }
    elsif ($param_information_type eq "profiles") {
        $env_info = get_env_info_profiles(
            jboss                 => $jboss,
            additional_parameters => $param_additional_options
        );
    }
    elsif ($param_information_type eq "dataSources" || $param_information_type eq "xaDataSources") {
        my $get_xa = 1 if $param_information_type eq "xaDataSources";

        if ($jboss_is_domain) {
            if ($param_information_type_context) {
                $env_info = get_env_info_data_sources_in_profile(
                    jboss                 => $jboss,
                    get_xa                => $get_xa,
                    profile               => $param_information_type_context,
                    additional_parameters => $param_additional_options
                );
            }
            else {
                $env_info = get_env_info_data_sources_in_all_profiles(
                    jboss                 => $jboss,
                    get_xa                => $get_xa,
                    additional_parameters => $param_additional_options
                );
            }
        }
        else {
            $env_info = get_env_info_data_sources_in_standalone(
                jboss                 => $jboss,
                get_xa                => $get_xa,
                additional_parameters => $param_additional_options
            );
        }
    }

    $env_info = replace_passwords_by_stars_in_cli_response($env_info);

    $jboss->log_info("Requested Environment Information: $env_info");
    $jboss->set_property($property_path, $env_info);

    $jboss->log_info("=======Finished: getting environment information, information type - '$param_information_type'=======");
}
# Auto-generated method for the procedure RemoveJMSQueue/RemoveJMSQueue
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: queueName
# Parameter: profile

# $sr - StepResult object

sub get_all_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = qq|/:read-children-names(child-type=profile)|;
    my $profiles = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return $profiles;
}

sub removeJMSQueue {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        queueName
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
    if ($product_version =~ m/^6/) {
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
# Auto-generated method for the procedure RemoveJMSTopic/RemoveJMSTopic
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: topicName
# Parameter: profile

# $sr - StepResult object
sub removeJMSTopic {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        topicName
        profile
    /);

    my $param_topic_name = $params->{topicName};
    my $param_profile = $params->{profile};

    my $cli_command;
    my $json;

    if (!$param_topic_name) {
        $jboss->bail_out("Required parameter 'topicName' is not provided");
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
    if ($product_version =~ m/^6/) {
        $subsystem_part = "subsystem=messaging";
        $provider_part = "hornetq-server=default";
    }

    ########
    # check if jms topic with specified name exists
    ########
    if ($jboss_is_domain) {
        $cli_command = "/profile=$param_profile/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }
    else {
        $cli_command = "/$subsystem_part/$provider_part:read-children-resources(child-type=jms-topic)";
    }

    $json = run_command_and_get_json_with_exiting_on_error(
        command => $cli_command,
        jboss   => $jboss
    );

    my $jms_topic_resources = $json->{result};
    my $jms_topic_exists = 1 if $jms_topic_resources->{$param_topic_name};

    if ($jms_topic_exists) {
        $jboss->log_info("JMS topic '$param_topic_name' exists - to be removed");

        $cli_command = qq/jms-topic remove /;

        if ($jboss_is_domain) {
            $cli_command .= qq/ --profile=$param_profile /;
        }

        $cli_command .= qq/ --topic-address=$param_topic_name /;

        run_command_with_exiting_on_error(
            command => $cli_command,
            jboss   => $jboss
        );

        $jboss->set_property(summary => "JMS topic '$param_topic_name' has been removed successfully");
        return;
    }
    else {
        $jboss->log_info("JMS topic '$param_topic_name' does not exist");

        $jboss->set_property(summary => "JMS topic '$param_topic_name' not found");
        $jboss->warning();
        return;
    }
}
# Auto-generated method for the procedure RemoveXADataSource/RemoveXADataSource
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: dataSourceName
# Parameter: profile

# $sr - StepResult object
sub removeXADataSource {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
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
# Auto-generated method for the procedure RunCustomCommand/RunCustomCommand
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: customCommand

# $sr - StepResult object
sub runCustomCommand {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name    =>  $PROJECT_NAME,
        plugin_name     =>  $PLUGIN_NAME,
        plugin_key      =>  $PLUGIN_KEY,
        flowpdf         => $self
    );

    my $params = $jboss->get_params_as_hashref('customCommand', 'serverconfig', 'scriptphysicalpath');
    $jboss->out("Custom command: $params->{customCommand}");
    my %result = $jboss->run_command($params->{customCommand});

    $jboss->out("Command result:\n", $result{stdout});
    $jboss->process_response(%result);
}
# Auto-generated method for the procedure ShutdownStandaloneServer/ShutdownStandaloneServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath

# $sr - StepResult object
sub shutdownStandaloneServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        flowpdf                         => $self,
    );

    my $cfg = $jboss->get_plugin_configuration();

    $::gEC = new ElectricCommander();
    $::gEC->abortOnError(0);

    $::gServerConfig = ($::gEC->getProperty("serverconfig") )->findvalue("//value");
    my %tempConfig = %$cfg;

    if ($tempConfig{java_opts}) {
        my $new_java_opts = $tempConfig{java_opts};
        if ($ENV{JAVA_OPTS}) {
            $new_java_opts = $ENV{JAVA_OPTS} . ' ' . $new_java_opts;
        }
        $ENV{JAVA_OPTS} = $new_java_opts;
    }

    if ($tempConfig{scriptphysicalpath}) {
        $::gScriptPhysicalLocation = $tempConfig{scriptphysicalpath};
    }
    my $temp = ($::gEC->getProperty("scriptphysicalpath") )->findvalue("//value");
    if ($temp) {
        # $::gScriptPhysicalLocation = ($::gEC->getProperty("scriptphysicalpath") )->findvalue("//value");
        $::gScriptPhysicalLocation = $temp;
    }

    if (!$::gScriptPhysicalLocation) {
        print "No script physical path were found neither in configuration nor in procedure\n";
        exit 1;
    }


    my $cmdLine = '';

    my %props;

    my $rawUrl = '';
    my $user = '';
    my $pass = '';
    my %configuration;

    my $content;

    #getting all info from the configuration, url, user and pass
    if ($::gServerConfig ne '') {
        %configuration = %$cfg;
        if (%configuration) {
            $rawUrl = $configuration{'jboss_url'};
            my $url;
            my $port;
            print "$rawUrl\n";
            #checking if raw url comes in the format http(s)://whatever(:port)/(path)
            if ($rawUrl =~ m/http(\w*):\/\/(\S[^:]*)(:*)(\d*)(\/*)(.*)/) {
                $url = $2;
                $port = $4;
            }
            elsif ($rawUrl =~ m/(\S[^:]*)(:*)(\d*)(\/*)(.*)/) {
                $url = $1;
                $port = $3;
            }
            else {
                print "Error: Not a valid URL.\n";
                exit ERROR;
            }
            print "url: $url port: $port\n";
            $cmdLine = "\"$::gScriptPhysicalLocation\" --connect controller=$url:$port command=:shutdown";
        }
    }
    else {
        $cmdLine = "\"$::gScriptPhysicalLocation\" --connect command=:shutdown";
    }
    $content = `$cmdLine`;
    print $content;

    #evaluates if exit was successful to mark it as a success or fail the step

    if ($? == SUCCESS) {
        if ($content =~ m/\"outcome\" => \"success\"(.+)/) {
            #server was turned off
            $::gEC->setProperty("/myJobStep/outcome", 'success');
        } elsif ($content =~ m/You are disconnected at the moment(.+)/) {
            #if not, an exception was reached
            $::gEC->setProperty("/myJobStep/outcome", 'error');
        }
    }
    else {
        $::gEC->setProperty("/myJobStep/outcome", 'error');
    }
    #add masked command line to properties object
    $props{'cmdLine'} = $cmdLine;

     foreach my $key (keys %props) {
        my $val = $props{$key};
        $::gEC->setProperty("/myCall/$key", $val);
    }
}
# Auto-generated method for the procedure StartDomainServer/StartDomainServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: alternatejbossconfig
# Parameter: alternateJBossConfigHost

# $sr - StepResult object
sub startDomainServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        flowpdf                         => $self,
    );
    my $cfg = $jboss->get_plugin_configuration();

    $::gEC = new ElectricCommander();
    $::gEC->abortOnError(0);

    $::gScriptPhysicalLocation = ($::gEC->getProperty("scriptphysicalpath") )->findvalue("//value");
    $::gAlternateJBossConfigDomain = ($::gEC->getProperty("alternatejbossconfig") )->findvalue("//value");
    $::gAlternateJBossConfigHost = ($::gEC->getProperty("alternateJBossConfigHost") )->findvalue("//value");
    $::gServerConfig = ($::gEC->getProperty("serverconfig") )->findvalue("//value");

    my %tempConfig = %$cfg;

    if ($tempConfig{java_opts}) {
        my $new_java_opts = $tempConfig{java_opts};
        if ($ENV{JAVA_OPTS}) {
            $new_java_opts = $ENV{JAVA_OPTS} . ' ' . $new_java_opts;
        }
        $ENV{JAVA_OPTS} = $new_java_opts;
    }


    my $cmdLine = '';

    my %props;

    #start admin server using ecdaemon
    startServer($::gScriptPhysicalLocation, $::gAlternateJBossConfigDomain, $::gAlternateJBossConfigHost);
    verifyServerIsStarted($::gServerConfig, $cfg);

    foreach my $key (keys %props) {
        my $val = $props{$key};
        $::gEC->setProperty("/myCall/$key", $val);
    }
}
# Auto-generated method for the procedure StartHostController/StartHostController
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: startupScript
# Parameter: domainConfig
# Parameter: hostConfig
# Parameter: jbossHostName
# Parameter: additionalOptions
# Parameter: logFileLocation

# $sr - StepResult object
sub startHostController {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        startupScript
        domainConfig
        hostConfig
        jbossHostName
        additionalOptions
        logFileLocation
    /);

    my $param_startup_script = $params->{startupScript};
    my $param_domain_config = $params->{domainConfig};
    my $param_host_config = $params->{hostConfig};
    my $param_host_name = $params->{jbossHostName};
    my $param_additional_options = $params->{additionalOptions};
    my $log_file_location = $params->{logFileLocation};

    if (!$param_startup_script) {
        $jboss->bail_out("Required parameter 'startupScript' is not provided");
    }

    if (!$param_host_name) {
        $jboss->log_warning("Verification via master CLI that host contoller is started will not be performed due to 'jbossHostName' parameter is not provided");
    }

    if ($param_host_name) {
        exit_if_host_controller_is_already_started(
            jboss     => $jboss,
            host_name => $param_host_name
        );
    }

    start_host_controller(
        startup_script     => $param_startup_script,
        domain_config      => $param_domain_config,
        host_config        => $param_host_config,
        additional_options => $param_additional_options,
        jboss              => $jboss
    );

    verify_host_controller_is_started_and_show_startup_info(
        jboss             => $jboss,
        host_name         => $param_host_name,
        log_file_location => $log_file_location
    );
}
# Auto-generated method for the procedure StartServers/StartServers
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time

# $sr - StepResult object
sub startServers {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $DESIRED_STATUS = 'STARTED';
    my $SLEEP_TIME = 5;

    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        flowpdf      => $self,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'wait_time'
    );

    $jboss->bail_out("Required parameter 'serversgroup' is not provided") unless $params->{serversgroup};

    my $wait_time = undef;
    $params->{wait_time} = $jboss->trim($params->{wait_time});

    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time expected to be positive integer (wait time in seconds), 0 (unlimited) or undefined (one time check).");
        }
    }

    my ($servers, $states) = $jboss->get_servergroup_status($params->{serversgroup});

    if (!@$states) {
        $jboss->bail_out("Server group $params->{serversgroup} does not exist or empty.");
    }

    $jboss->{silent} = 1;
    my $servers_with_terminal_status = $jboss->is_servergroup_has_status(
        $params->{serversgroup},
        [$DESIRED_STATUS]
    );
    $jboss->{silent} = 0;
    if (@$servers_with_terminal_status) {
        for my $server_record (@$servers_with_terminal_status) {
            my $message = sprintf(
                "Server %s on %s is already in %s state",
                $server_record->{server},
                $server_record->{host},
                $server_record->{status}
            );
            $jboss->log_warning($message);
            $jboss->add_warning_summary($message);
            $jboss->add_status_warning();
        }
    }
    my $command = sprintf '/server-group=%s:start-servers', $params->{serversgroup};
    $jboss->out("Starting serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command_with_exiting_on_error(
        command => $command
    );

    my $res = {
        error => 0,
        msg => ''
    };

    my $done = 0;
    my $time_start = time();
    while (!$done) {
        my $time_diff = time() - $time_start;

        if (!$wait_time) {
            # if wait time is undefined or 0 then we perform check only once
            $done = 1;
        }
        elsif ($wait_time && $time_diff >= $wait_time) {
            # if wait time is already passed we do not perform more checks
            $done = 1;
            last;
        }

        my ($servers, $states_ref) = $jboss->get_servergroup_status($params->{serversgroup});
        my %seen = ();
        @$states_ref = grep {!$seen{$_}++} @$states_ref;
        if (scalar @$states_ref == 1 && $states_ref->[0] eq $DESIRED_STATUS) {
            $res->{error} = 0;
            $res->{msg} = '';
            last;
        }
        $res->{error} = 1;
        my $msg = 'Following servers are not started:' . "\n";
        for my $host_name ( keys %$servers ) {
            for my $server_name ( keys %{$servers->{$host_name}}) {
                $jboss->out("$server_name (host: $host_name) is $servers->{$host_name}->{$server_name}->{status}");
                # What should we set in this case?
                $msg .= "\n$server_name (host: $host_name, status: $servers->{$host_name}->{$server_name}->{status})";
            }
        }
        sleep 5;
    }
    if ($res->{error}) {
        $jboss->add_error_summary($res->{msg});
        $jboss->add_status_error();
    }
    return 1;
}
# Auto-generated method for the procedure StartStandaloneServer/StartStandaloneServer
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: alternatejbossconfig
# Parameter: additionalOptions
# Parameter: logFileLocation

# $sr - StepResult object
sub startStandaloneServer {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        scriptphysicalpath
        alternatejbossconfig
        additionalOptions
        logFileLocation
    /);

    my $param_startup_script = $params->{scriptphysicalpath};
    my $param_optional_config = $params->{alternatejbossconfig};
    my $param_additional_options = $params->{additionalOptions};
    my $log_file_location = $params->{logFileLocation};

    if (!$param_startup_script) {
        $jboss->bail_out("Required parameter 'scriptphysicalpath' is not provided");
    }

    exit_if_jboss_is_already_started(jboss => $jboss);

    start_standalone_server(
        startup_script     => $param_startup_script,
        optional_config    => $param_optional_config,
        additional_options => $param_additional_options,
        jboss              => $jboss);

    verify_jboss_is_started_and_show_startup_info(
        jboss             => $jboss,
        log_file_location => $log_file_location
    );
}
# Auto-generated method for the procedure StopDomain/StopDomain
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: jbossTimeout
# Parameter: allControllersShutdown

# $sr - StepResult object
sub stopDomain {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $PLUGIN_NAME = '@PLUGIN_NAME@';
    my $PLUGIN_KEY = '@PLUGIN_KEY@';

    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1,
        flowpdf                         => $self,
    );

    my $params = $jboss->get_params_as_hashref(qw/
        jbossTimeout
        allControllersShutdown
    /);

    my $param_timeout = $params->{jbossTimeout};
    $param_timeout = "" if !defined $param_timeout;
    $param_timeout = trim($param_timeout);

    my $param_all_controllers_shutdown = $params->{allControllersShutdown};

    my $cli_command;
    my $json;
    my $summary;

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

    if (!$jboss_is_domain) {
        $jboss->bail_out("Wrong usage of the procedure - StopDomain should be used for Managed Domain (not Standalone)");
    }

    ########
    # check jboss version
    ########
    my $version = $jboss->get_jboss_server_version();
    my $product_version = $version->{product_version};
    my $jboss_is_6x = 1 if $product_version =~ m/^6/;

    ########
    # check whether timeout option is supported (supported in EAP 7 and later)
    ########
    if ($jboss_is_6x && $param_timeout ne "") {
        $jboss->log_warning("Timeout for stop-servers and shutdown is not supported in JBoss EAP 6.X - ignoring it");
        $param_timeout = "";
    }

    ########
    # get all host controller names
    ########
    my @all_hosts = @{ get_all_hosts(jboss => $jboss) };

    ########
    # stop all servers in domain
    ########
    $jboss->log_info("=======Started: stopping all servers within domain=======");

    # stop all servers withing domain
    my $cli_stop_servers = ":stop-servers";
    $cli_stop_servers .= "(timeout=$param_timeout)" if $param_timeout ne "";
    run_command_with_exiting_on_error(command => $cli_stop_servers, jboss => $jboss);

    # verification that all servers within domain are STOPPED or DISABLED
    my @servers_with_status_stopped_or_disabled;
    my @servers_with_status_stopping;
    my @servers_with_unexpected_status;
    foreach my $host (@all_hosts) {
        $cli_command = qq|/host=$host/:read-children-resources(child-type=server-config,include-runtime=true)|;
        my $server_config_resources = run_command_and_get_json_result_with_exiting_on_non_success(
            command => $cli_command,
            jboss   => $jboss
        );
        foreach my $server_name (keys %$server_config_resources) {
            my $server_status = $server_config_resources->{$server_name}->{status};
            if ($server_status eq "STOPPED" || $server_status eq "DISABLED") {
                $jboss->log_info("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_status_stopped_or_disabled, \%server_info;
            }
            elsif ($server_status eq "STOPPING") {
                $jboss->log_warning("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_status_stopping, \%server_info;
            }
            else {
                $jboss->log_error("Server '$server_name' on host '$host' is '$server_status'");
                my %server_info = (
                    server_name   => $server_name,
                    server_status => $server_status,
                    host_name     => $host,
                );
                push @servers_with_unexpected_status, \%server_info;
            }
        }
    }

    ########
    # preparing step summary for stop servers part
    ########
    my @summary_messages;
    if (@servers_with_unexpected_status) {
        my $message = "Found " . scalar @servers_with_unexpected_status . " servers with unexpected statuses:";
        foreach my $server_info (@servers_with_unexpected_status) {
            my $server_name = $server_info->{server_name};
            my $server_status = $server_info->{server_status};
            my $host_name = $server_info->{host_name};
            $message .= join("\n", " server '$server_name' on host '$host_name' with '$server_status' status");
        }
        push @summary_messages, $message;
    }
    if (@servers_with_status_stopping) {
        my $message = "Found " . scalar @servers_with_status_stopping . " servers with STOPPING status:";
        foreach my $server_info (@servers_with_status_stopping) {
            my $server_name = $server_info->{server_name};
            my $server_status = $server_info->{server_status};
            my $host_name = $server_info->{host_name};
            $message .= join("\n", " server '$server_name' on host '$host_name' with '$server_status' status");
        }
        push @summary_messages, $message;
    }
    if (@servers_with_status_stopped_or_disabled) {
        my $message = "Found " . scalar @servers_with_status_stopped_or_disabled . " servers with expected statuses (STOPPED or DISABLED)";
        push @summary_messages, $message;
    }

    $summary = "Performed stop-servers operation for domain";
    if (@summary_messages) {
        $summary .= "\n" . join("\n", @summary_messages);
    }
    $jboss->set_property(summary => $summary);

    if ($param_all_controllers_shutdown) {
        if (@servers_with_unexpected_status || @servers_with_status_stopping) {
            $jboss->warning();
        }
    }
    else {
        if ((@servers_with_unexpected_status)) {
            $jboss->error();
            exit 1;
        }
        if (@servers_with_status_stopping) {
            $jboss->warning();
        }
    }

    $jboss->log_info("=======Finished: stopping all servers within domain=======");

    if ($param_all_controllers_shutdown) {
        $jboss->log_info("=======Started: shutdown all host controllers within domain=======");

        # gathering information about host controllers
        my @all_slave_hosts;
        my $master_host;

        foreach my $host (@all_hosts) {
            $jboss->log_info("Checking whether host controller '$host' is master or slave");
            if (is_host_master(jboss => $jboss, host => $host)) {
                $jboss->log_info("Host controller '$host' is master");
                $master_host = $host;
            }
            else {
                $jboss->log_info("Host controller '$host' is slave");
                push @all_slave_hosts, $host;
            }
        }

        # shutdown of slave host controllers
        foreach my $host (@all_slave_hosts) {
            $jboss->log_info("Starting shudown of slave host controller '$host'");
            my $cli_shutdown_slave = $jboss_is_6x
                ? get_cli_host_shutdown_6x(host => $host)
                : get_cli_host_shutdown_7x(host => $host, timeout => $param_timeout);
            run_command_with_exiting_on_error(command => $cli_shutdown_slave, jboss => $jboss);
            $summary .= "\nShutdown was performed for slave host controller '$host'";
            $jboss->log_info("Done with shudown of slave host controller '$host'");
        }

        # verification that shutdown of slave host controllers was successful
        my @all_hosts_after_all_slaves_shutdown = @{ get_all_hosts(jboss => $jboss) };
        if (@all_hosts_after_all_slaves_shutdown == 1
            && $all_hosts_after_all_slaves_shutdown[0] eq $master_host) {
            $jboss->log_info("All slave host controllers expect master '$master_host' are shut down");
        }
        else {
            my $error_summary = sprintf(
                "Something wrong after stopping all slave host controllers"
                    . "(before stopping master host controller '%s')."
                    . "\nExpected is to have only master host controller '%s' started at this point,"
                    . "but actual list of started host controllers is: [%s]",
                $master_host,
                $master_host,
                join(", ", @all_hosts_after_all_slaves_shutdown)
            );
            $jboss->log_error($error_summary);

            $summary .= "\n\nError: $error_summary";
            $jboss->set_property(summary => $summary);
            $jboss->error();
            exit 1;
        }

        # shutdown of master host controller
        $jboss->log_info("Starting shudown of master host controller '$master_host'");
        my $cli_shutdown_master = $jboss_is_6x
            ? get_cli_host_shutdown_6x(host => $master_host)
            : get_cli_host_shutdown_7x(host => $master_host, timeout => $param_timeout);
        run_command_with_exiting_on_error(command => $cli_shutdown_master, jboss => $jboss);
        $summary .= "\nShutdown was performed for master host controller '$master_host'";
        $jboss->log_info("Done with shudown of master host controller '$master_host'");

        # verification that shutdown of master host controller was successful
        if (is_host_controller_not_available(jboss => $jboss)) {
            $jboss->log_info("Master host controller '$master_host' is shut down, checked that connenction via CLI failed with 'The controller is not available...' error");
        }
        else {
            my $error_summary = "Check that master host controller '$master_host' is shut down failed";
            $jboss->log_error("Check that master host controller '$master_host' is shut down failed (check that connenction via CLI failed with 'The controller is not available...' error is failed)");

            $summary .= "\n\nError: $error_summary";
            $jboss->set_property(summary => $summary);
            $jboss->error();
            exit 1;
        }

        $jboss->set_property(summary => $summary);

        $jboss->log_info("=======Finished: shutdown all servers within domain=======");
    }
}
# Auto-generated method for the procedure StopServers/StopServers
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: serversgroup
# Parameter: wait_time

# $sr - StepResult object
sub stopServers {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    my $PROJECT_NAME = '$[/myProject/projectName]';
    my $STATUS_STOPPED = 'STOPPED'; # stopped status for servers with auto-start true
    my $STATUS_DISABLED = 'DISABLED'; # stopped status for servers with auto-start false
    my $SLEEP_TIME = 5;

    my $jboss = EC::JBoss->new(
        project_name => $PROJECT_NAME,
        flowpdf      => $self,
    );

    my $params = $jboss->get_params_as_hashref(
        'serversgroup',
        'wait_time'
    );

    $jboss->bail_out("Required parameter 'serversgroup' is not provided") unless $params->{serversgroup};

    my $wait_time = undef;
    $params->{wait_time} = $jboss->trim($params->{wait_time});

    if (defined $params->{wait_time} && $params->{wait_time} ne '') {
        $wait_time = $params->{wait_time};
        if ($wait_time !~ m/^\d+$/s) {
            $jboss->bail_out("Wait time expected to be positive integer (wait time in seconds), 0 (unlimited) or undefined (one time check).");
        }
    }

    my ($servers, $states) = $jboss->get_servergroup_status($params->{serversgroup});

    if (!@$states) {
        $jboss->bail_out("Server group $params->{serversgroup} does not exist or empty.");
    }

    $jboss->{silent} = 1;
    my $servers_with_terminal_status = $jboss->is_servergroup_has_status(
        $params->{serversgroup},
        [$STATUS_STOPPED, $STATUS_DISABLED]
    );
    $jboss->{silent} = 0;
    if (@$servers_with_terminal_status) {
        for my $server_record (@$servers_with_terminal_status) {
            my $message = sprintf(
                "Server %s on %s is already in %s state",
                $server_record->{server},
                $server_record->{host},
                $server_record->{status}
            );
            $jboss->log_warning($message);
            $jboss->add_warning_summary($message);
            $jboss->add_status_warning();
        }
    }
    my $command = sprintf '/server-group=%s:stop-servers', $params->{serversgroup};
    $jboss->out("Stopping serversgroup: $params->{serversgroup}");
    my %result = $jboss->run_command_with_exiting_on_error(
        command => $command
    );

    my $res = {
        error => 0,
        msg => ''
    };

    my $done = 0;
    my $time_start = time();
    while (!$done) {
        my $time_diff = time() - $time_start;

        if (!$wait_time) {
            # if wait time is undefined or 0 then we perform check only once
            $done = 1;
        }
        elsif ($wait_time && $time_diff >= $wait_time) {
            # if wait time is already passed we do not perform more checks
            $done = 1;
            last;
        }

        my ($servers, $all_states_ref) = $jboss->get_servergroup_status($params->{serversgroup});

        my @stopped_states = grep { $_ eq $STATUS_STOPPED || $_ eq $STATUS_DISABLED } @$all_states_ref;
        my $all_servers_are_stopped = ( scalar @stopped_states == scalar @$all_states_ref ) ? 1 : 0;

        if ( $all_servers_are_stopped ) {
            $res->{error} = 0;
            $res->{msg} = '';
            last;
        }
        $res->{error} = 1;
        my $msg = 'Following servers are not stopped:' . "\n";
        for my $host_name ( keys %$servers ) {
            for my $server_name ( keys %{$servers->{$host_name}}) {
                $jboss->out("$server_name (host: $host_name) is $servers->{$host_name}->{$server_name}->{status}");
                # What should we set in this case?
                $msg .= "\n$server_name (host: $host_name, status: $servers->{$host_name}->{$server_name}->{status})";
            }
        }
        sleep 5;
    }
    if ($res->{error}) {
        $jboss->add_error_summary($res->{msg});
        $jboss->add_status_error();
    }
    return 1;
}
# Auto-generated method for the procedure UndeployApp/UndeployApp
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: serverconfig
# Parameter: scriptphysicalpath
# Parameter: appname
# Parameter: allrelevantservergroups
# Parameter: servergroups
# Parameter: keepcontent
# Parameter: additional_options

# $sr - StepResult object
sub undeployApp {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
## === step ends ===
# Please do not remove the marker above, it is used to place new procedures into this file.

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

sub check_boot_errors_via_cli {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    $jboss->log_info("Checking boot errors via CLI");

    my $cli_command = '/core-service=management/:read-boot-errors';

    my %result = $jboss->run_command($cli_command);

    if ($result{code}) {
        $jboss->log_warning("Cannot read boot errors via CLI");
        $jboss->add_summary("Cannot read boot errors via CLI");
        $jboss->add_status_warning();
        return;
    }
    else {
        my $json = $jboss->decode_answer($result{stdout});
        if ($json && exists $json->{result}) {
            my $boot_errors_result = $json->{result};
            if (!@$boot_errors_result) {
                $jboss->log_info("No boot errors detected via CLI");
                $jboss->add_summary("No boot errors detected via CLI");
                return;
            }
        }

        $jboss->log_warning("JBoss boot errors: " . $result{stdout});
        $jboss->add_summary("Detected boot errors via CLI, see log for details");
        $jboss->add_status_warning();
        return;
    }
}

sub check_host_cotroller_boot_errors_via_cli {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    $jboss->log_info("Checking boot errors via CLI");

    my $cli_command = "/host=$host_name/core-service=management/:read-boot-errors";

    my %result = $jboss->run_command($cli_command);

    if ($result{code}) {
        $jboss->log_warning("Cannot read boot errors of host controller '$host_name' via CLI");
        $jboss->add_warning_summary("Cannot read boot errors of host controller '$host_name' via CLI");
        $jboss->add_status_warning();
        return;
    }
    else {
        my $json = $jboss->decode_answer($result{stdout});
        if ($json && exists $json->{result}) {
            my $boot_errors_result = $json->{result};
            if (!@$boot_errors_result) {
                $jboss->log_info("No boot errors of host controller '$host_name'");
                $jboss->add_summary("No boot errors of host controller '$host_name'");
                return;
            }
        }

        $jboss->log_warning("Detected boot errors of host controller '$host_name': " . $result{stdout});
        $jboss->add_warning_summary("Detected boot errors of host controller '$host_name', see log for details");
        $jboss->add_status_warning();
        return;
    }
}

sub check_server_boot_errors_via_cli {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";
    my $server_name = $args{server_name} || croak "'server_name' is required param";

    $jboss->log_info("Checking boot errors of server '$server_name' on host '$host_name' via CLI");

    my $cli_command = "/host=$host_name/server=$server_name/core-service=management/:read-boot-errors";

    my %result = $jboss->run_command($cli_command);

    if ($result{code}) {
        $jboss->log_warning("Cannot read boot errors of server '$server_name' on host '$host_name' via CLI");
        $jboss->add_warning_summary("Cannot read boot errors of server '$server_name' on host '$host_name' via CLI");
        $jboss->add_status_warning();
        return;
    }
    else {
        my $json = $jboss->decode_answer($result{stdout});
        if ($json && exists $json->{result}) {
            my $boot_errors_result = $json->{result};
            if (!@$boot_errors_result) {
                $jboss->log_info("No boot errors of server '$server_name' on host '$host_name'");
                $jboss->add_summary("No boot errors of server '$server_name' on host '$host_name'");
                return;
            }
        }

        $jboss->log_warning("Detected boot errors of server '$server_name' on host '$host_name': " . $result{stdout});
        $jboss->add_warning_summary("Detected boot errors of server '$server_name' on host '$host_name', see log for details");
        $jboss->add_status_warning();
        return;
    }
}

sub check_servers {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    my @all_servers = @{ run_command_and_get_json_result_with_exiting_on_non_success_for_starthostcontoller_procedure(
        command => "/host=$host_name/:read-children-names(child-type=server-config)",
        jboss   => $jboss
    ) };

    if (!@all_servers) {
        $jboss->log_info("There are no servers on host '$host_name'");
        $jboss->add_summary("There are no servers on host '$host_name'");
    }

    foreach my $server (@all_servers) {
        my $server_status = run_command_and_get_json_result_with_exiting_on_non_success_for_starthostcontoller_procedure(
            command => "/host=$host_name/server-config=$server/:read-attribute(name=status)",
            jboss   => $jboss
        );
        if ($server_status eq "DISABLED" || $server_status eq "STOPPED") {
            $jboss->log_info("Server '$server' on host '$host_name' has status '$server_status', reading of logs via CLI will not be performed for this server");
            $jboss->add_summary("Server '$server' on host '$host_name' has status '$server_status'");

        }
        elsif ($server_status eq "STARTED") {
            $jboss->log_info("Server '$server' on host '$host_name' has status '$server_status', reading logs via CLI");
            $jboss->add_summary("Server '$server' on host '$host_name' has status '$server_status'");
            check_server_boot_errors_via_cli(
                jboss       => $jboss,
                host_name   => $host_name,
                server_name => $server
            ) if $jboss->is_cli_command_supported_read_boot_errors();
            show_logs_via_cli_for_server(
                jboss       => $jboss,
                host_name   => $host_name,
                server_name => $server
            ) if $jboss->is_cli_command_supported_read_log_file();
        }
        else {
            $jboss->log_warning("Server '$server' on host '$host_name' has status '$server_status', please refer to the JBoss logs on file system for more information");
            $jboss->add_warning_summary("Server '$server' on host '$host_name' has status '$server_status', please refer to the JBoss logs on file system for more information");
            $jboss->add_status_warning();
        }
    }
}

sub create_command_line {
    my ($arr) = @_;

    my $commandName = @$arr[0];

    my $command = $commandName;

    shift(@$arr);

    foreach my $elem (@$arr) {
        $command .= " $elem";
    }
    return $command;
}

sub escape_additional_options_for_windows {
    my $additional_options = shift || croak "required param is not provided (additional_options)";

    $additional_options =~ s|"|\"|gs;

    return $additional_options;
}

sub exit_if_host_controller_is_already_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    $jboss->log_info("Checking via Master CLI whether JBoss Host Controller '$host_name' is already started");
    my $cli_command = ':read-attribute(name=launch-type)';
    my %result = $jboss->run_command($cli_command);
    if ($result{code}) {
        if (($result{stdout} =~ m/The\scontroller\sis\snot\savailable/s
            || $result{stderr} =~ m/The\scontroller\sis\snot\savailable/s)) {
            $jboss->log_info("JBoss Master is not started yet (cannot connect to Master CLI)");
            return;
        }
        else {
            $jboss->process_response(%result);
            exit ERROR;
        }
    }
    else {
        my $json = $jboss->decode_answer($result{stdout});
        $jboss->bail_out("Cannot convert JBoss response into JSON") if !$json;

        my $launch_type = lc $json->{result};
        if (!$launch_type || $launch_type ne "domain") {
            $jboss->log_warning("JBoss is started, but operating mode is '$launch_type' instead of 'domain'");
            $jboss->add_error_summary("JBoss is started, but operating mode is '$launch_type' instead of 'domain'");
            $jboss->add_status_error();
            exit ERROR;
        }
        else {
            my @all_hosts = @{ get_all_hosts(jboss => $jboss) };
            my %all_hosts_hash = map {$_ => 1} @all_hosts;
            if ($all_hosts_hash{$host_name}) {
                $jboss->log_warning("JBoss Host Controller '$host_name' is already started");
                $jboss->add_warning_summary("JBoss Host Controller '$host_name' is already started");
                $jboss->add_status_warning();
                exit SUCCESS;
            }
            else {
                $jboss->log_info("JBoss Host Controller '$host_name' is not started");
                return;
            }
        }
    }
}

sub exit_if_jboss_is_already_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    $jboss->log_info("Checking whether JBoss Standalone is already started by connecting to CLI");
    my $cli_command = ':read-attribute(name=launch-type)';
    my %result = $jboss->run_command($cli_command);
    if ($result{code}
        && ($result{stdout} =~ m/The\scontroller\sis\snot\savailable/s
        || $result{stderr} =~ m/The\scontroller\sis\snot\savailable/s)) {
        $jboss->log_info("JBoss is not started - checked by attempt to connect to the cli");
        return;
    }
    else {
        my $json = $jboss->decode_answer($result{stdout});
        $jboss->bail_out("Cannot convert JBoss response into JSON") if !$json;

        my $launch_type = lc $json->{result};
        if (!$launch_type || $launch_type ne "standalone") {
            $jboss->log_warning("JBoss is started, but operating mode is '$launch_type' instead of 'standalone'");
            $jboss->add_summary("JBoss is started, but operating mode is '$launch_type' instead of 'standalone'");
            $jboss->add_status_error();
            exit ERROR;
        }
        else {
            $jboss->log_warning("JBoss is already started in expected operating mode '$launch_type'");
            $jboss->add_summary("JBoss is already started in expected operating mode '$launch_type'");
            $jboss->add_status_warning();
            exit SUCCESS;
        }
    }
}

sub get_all_hosts {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = qq|/:read-children-names(child-type=host)|;
    my $hosts = run_command_and_get_json_result_with_exiting_on_non_success_for_starthostcontoller_procedure(
        command => $cli_command,
        jboss   => $jboss
    );

    return $hosts;
}

sub get_cli_host_shutdown_6x {
    my %args = @_;
    my $host = $args{host} || croak "'host' is required param";

    my $cli_host_shutdown = "/host=$host:shutdown";
    return $cli_host_shutdown;
}

sub get_cli_host_shutdown_7x {
    my %args = @_;
    my $host = $args{host} || croak "'host' is required param";
    my $timeout = $args{timeout};

    my $cli_host_shutdown = "shutdown --host=$host";
    $cli_host_shutdown .= " --timeout=$timeout" if $timeout && $timeout ne "";
    return $cli_host_shutdown;
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

sub get_recent_log_lines {
    my %args = @_;
    my $file = $args{file} || croak "'file' is required param";
    my $num_of_lines = $args{num_of_lines} || croak "'num_of_lines' is required param";

    my @lines;

    my $count = 0;
    my $filesize = -s $file; # filesize used to control reaching the start of file while reading it backward
    my $offset = - 2; # skip two last characters: \n and ^Z in the end of file

    open F, $file or die "Can't read $file: $!\n";

    while (abs($offset) < $filesize) {
        my $line = "";
        # we need to check the start of the file for seek in mode "2"
        # as it continues to output data in revers order even when out of file range reached
        while (abs($offset) < $filesize) {
            seek F, $offset, 2;     # because of negative $offset & "2" - it will seek backward
            $offset -= 1;           # move back the counter
            my $char = getc F;
            last if $char eq "\n"; # catch the whole line if reached
            $line = $char . $line; # otherwise we have next character for current line
        }

        # got the next line!
        unshift @lines, "$line\n";

        # exit the loop if we are done
        $count++;
        last if $count > $num_of_lines;
    }

    return \@lines;
}

sub is_criteria_met {
    my ($jboss, $result, $launch_type, $criteria) = @_;

    my $json = $jboss->decode_answer($result->{stdout});
    $json = {} unless $json;

    $jboss->log_info("state is '" . ($json->{result} ? $json->{result} : "unknown") . "'");

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
        # state 'running' meet the RUNNING criteria
        # states such as 'starting', 'reload-required', 'restart-required', 'stopping' or other meet the NOT_RUNNING criteria
        # undefined state meet the NOT_RUNNING criteria
        my $state_is_considered_as_running;
        if ( $json->{outcome} && $json->{outcome} eq 'success' && $json->{result} && $json->{result} eq 'running' ) {
            $state_is_considered_as_running = 1;
        }

        if ( $criteria eq 'RUNNING' && $state_is_considered_as_running ) {
            return 1;
        }
        elsif ( $criteria eq 'NOT_RUNNING' && !$state_is_considered_as_running ) {
            return 1;
        }
        else {
            return 0;
        }
    }
}

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

sub is_criteria_met_url {
    my ($resp, $criteria) = @_;

    if ($criteria eq 'RUNNING') {
        return 1 if $resp->is_success();
    }
    # criterua is not running
    else {
        return 1 unless $resp->is_success();
    }
    return 0;
}

sub is_datasources_subsystem_available_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my @subsystems = @{ get_all_subsystems_in_profile(jboss => $jboss, profile => $profile) };
    my %subsystems_hash = map {$_ => 1} @subsystems;

    if ($subsystems_hash{'datasources'}) {
        return 1;
    }
    else {
        return 0;
    }
}

sub is_host_controller_not_available {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = ':read-attribute(name=launch-type)';
    my %result = $jboss->run_command($cli_command);
    if ($result{code}
        && ($result{stdout} =~ m/The\scontroller\sis\snot\savailable/s
        || $result{stderr} =~ m/The\scontroller\sis\snot\savailable/s)) {
        return 1;
    }
    else {
        return 0;
    }
}

sub is_host_master {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host = $args{host} || croak "'host' is required param";

    my $cli_command = qq|/host=$host/:read-attribute(name=master)|;
    my $is_master = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return 1 if $is_master;
    return 0;
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

sub get_all_subsystems_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = qq|/profile=$profile/:read-children-names(child-type=subsystem)|;
    my $subsystems = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $subsystems;
}

sub get_env_info_system_dump {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $additional_parameters = $args{additional_parameters};

    $additional_parameters = $additional_parameters ? $additional_parameters : "";
    my $cli_command = "/:read-resource($additional_parameters)";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $additional_parameters = $args{additional_parameters};

    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/:read-children-resources(child-type=profile$additional_parameters)";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my $xa_prefix = $get_xa ? "xa-" : "";
    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/subsystem=datasources/:read-children-resources(child-type=${xa_prefix}data-source${additional_parameters})";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_profile {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my $xa_prefix = $get_xa ? "xa-" : "";
    $additional_parameters = $additional_parameters ? ",$additional_parameters" : "";
    my $cli_command = "/profile=${profile}/subsystem=datasources/:read-children-resources(child-type=${xa_prefix}data-source${additional_parameters})";
    my %result = run_command_with_exiting_on_error(command => $cli_command, jboss => $jboss);
    my $env_info = $result{stdout};

    return $env_info;
}

sub get_env_info_data_sources_in_all_profiles {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $get_xa = $args{get_xa};
    my $additional_parameters = $args{additional_parameters};

    my @all_profiles = @{ get_all_profiles(jboss => $jboss) };
    my %profiles_env_info;
    foreach my $profile (@all_profiles) {
        if (is_datasources_subsystem_available_in_profile(jboss => $jboss, profile => $profile)) {
            $jboss->log_info("There is 'datasources' subsystem within '$profile' profile");
            $profiles_env_info{$profile} = get_env_info_data_sources_in_profile(
                jboss                 => $jboss,
                get_xa                => $get_xa,
                profile               => $profile,
                additional_parameters => $additional_parameters
            );
        }
        else {
            $jboss->log_info("There is no 'datasources' subsystem within '$profile' profile");
            $profiles_env_info{$profile} = "No 'datasources' subsystem";
        }
    }
    my $env_info = join("\n", map {"Profile '$_': $profiles_env_info{$_}"} keys %profiles_env_info);

    return $env_info;
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

sub replace_passwords_by_stars_in_cli_response {
    my $string = shift;
    return $string unless $string;
    $string =~ s/"password" => ".*?"/"password" => "***"/gs;
    return $string;
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

sub run_command_and_get_json_result_with_exiting_on_non_success_for_starthostcontoller_procedure {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $json_result;
    eval {
        $json_result = $jboss->run_command_and_get_json_result_with_failing_on_error(command => $command);
    };
    if ($@) {
        my $failure_description = $@;
        $jboss->add_error_summary($failure_description);
        $jboss->add_status_error();
        exit ERROR;
    }
    return $json_result;
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

sub get_all_data_sources_domain {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $profile = $args{profile} || croak "'profile' is required param";

    my $cli_command = "/profile=$profile/subsystem=datasources/:read-children-names(child-type=data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
}

sub get_all_data_sources_standalone {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = "/subsystem=datasources/:read-children-names(child-type=data-source)";
    my $json_result = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );
    return $json_result;
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



sub escape_additional_options {
    my $additional_options = shift || croak "required param is not provided (additional_options)";

    $additional_options =~ s|\\|\\\\|;
    $additional_options =~ s|"|\"|gs;

    return $additional_options;
}

sub is_reload_or_restart_required {
    my $jboss_output = shift;
    croak "required param is not provided (jboss_output)" unless defined $jboss_output;
    if ($jboss_output =~ m/process-state:\s(?:reload|restart)-required/s
        || $jboss_output =~ m/"process-state"\s=>\s"(?:reload|restart)-required"/s) {
        return 1;
    }
    return 0;
}

sub show_jboss_logs_from_requested_file {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $log_file_location = $args{log_file_location} || croak "'log_file_location' is required param";

    if (-f $log_file_location) {
        my $num_of_lines = NUMBER_OF_LINES_TO_TAIL_FROM_LOG;
        my $recent_log_lines = get_recent_log_lines(
            file         => $log_file_location,
            num_of_lines => $num_of_lines
        );
        $jboss->log_info("JBoss logs from file '$log_file_location' (showing recent $num_of_lines lines) :\n   | "
            . join('   | ', @$recent_log_lines));
    }
    else {
        $jboss->log_warning("Cannot find JBoss log file '$log_file_location'");
        $jboss->add_warning_summary("Cannot find JBoss log file '$log_file_location'");
        $jboss->add_status_warning();
    }
}

sub show_logs_via_cli {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $assumption_sting = sprintf(
        "assumption is that log file is %s, tailing %u lines",
        EXPECTED_LOG_FILE_NAME,
        NUMBER_OF_LINES_TO_TAIL_FROM_LOG
    );

    $jboss->log_info("Showing logs via CLI ($assumption_sting)");

    my $cli_command = sprintf(
        "/subsystem=logging/log-file=%s/:read-log-file(lines=%u,skip=0)",
        EXPECTED_LOG_FILE_NAME,
        NUMBER_OF_LINES_TO_TAIL_FROM_LOG
    );

    my %result = $jboss->run_command($cli_command);
    if ($result{code}) {
        $jboss->log_warning("Cannot read logs via CLI");
    }
    else {
        $jboss->log_info("JBoss logs ($assumption_sting): " . $result{stdout});
    }
}

sub show_logs_via_cli_for_server {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";
    my $server_name = $args{server_name} || croak "'server_name' is required param";

    my $assumption_sting = sprintf(
        "assumption is that log file is %s, tailing %u lines",
        EXPECTED_SERVER_LOG_FILE_NAME,
        NUMBER_OF_LINES_TO_TAIL_FROM_LOG
    );

    $jboss->log_info("Showing logs via CLI for server '$server_name' on host '$host_name' ($assumption_sting)");

    my $cli_command = sprintf(
        "/host=%s/server=%s/subsystem=logging/log-file=%s/:read-log-file(lines=%u,skip=0)",
        $host_name,
        $server_name,
        EXPECTED_SERVER_LOG_FILE_NAME,
        NUMBER_OF_LINES_TO_TAIL_FROM_LOG
    );

    my %result = $jboss->run_command($cli_command);
    if ($result{code}) {
        $jboss->log_warning("Cannot read logs via CLI for server '$server_name' on host '$host_name', please refer to JBoss logs on file system for more details");
    }
    else {
        $jboss->log_info("JBoss logs for server '$server_name' on host '$host_name' ($assumption_sting): " . $result{stdout});
    }
}

sub start_host_controller {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $startup_script = $args{startup_script} || croak "'startup_script' is required param";
    my $domain_config = $args{domain_config};
    my $host_config = $args{host_config};
    my $additional_options = $args{additional_options};

    # $The quote and backslash constants are just a convenient way to represtent literal literal characters so it is obvious
    # in the concatentations. NOTE: BSLASH ends up being a single backslash, it needs to be doubled here so it does not
    # escape the right curly brace.

    my $operatingSystem = $^O;
    print qq{OS: $operatingSystem\n};

    # Ideally, the logs should exist in the step's workspace directory, but because the ecdaemon continues after the step is
    # completed the temporary drive mapping to the workspace is gone by the time we want to write to it. Instead, the log
    # and errors get the JOBSTEPID appended and it goes in the Tomcat root directory.
    my $LOGNAMEBASE = "jbossstartdomainserver";

    # If we try quoting in-line to get the final string exactly right, it will be confusing and ugly. Only the last
    # parameter to our outer exec() needs _literal_ single and double quotes inside the string itself, so we build that
    # parameter before the call rather than inside it. Using concatenation here both substitutes the variable values and
    # puts literal quote from the constants in the final value, but keeps any other shell metacharacters from causing
    # trouble.

    my @systemcall;

    if ($operatingSystem eq WIN_IDENTIFIER) {
        # Windows has a much more complex execution and quoting problem. First, we cannot just execute under "cmd.exe"
        # because ecdaemon automatically puts quote marks around every parameter passed to it -- but the "/K" and "/C"
        # option to cmd.exe can't have quotes (it sees the option as a parameter not an option to itself). To avoid this, we
        # use "ec-perl -e xxx" to execute a one-line script that we create on the fly. The one-line script is an "exec()"
        # call to our shell script. Unfortunately, each of these wrappers strips away or interprets certain metacharacters
        # -- quotes, embedded spaces, and backslashes in particular. We end up escaping these metacharacters repeatedly so
        # that when it gets to the last level it's a nice simple script call. Most of this was determined by trial and error
        # using the sysinternals procmon tool.
        my $commandline = BSLASH . BSLASH . BSLASH . DQUOTE . $startup_script . BSLASH . BSLASH . BSLASH . DQUOTE;

        if ($domain_config && $domain_config ne '') {
            $commandline .= " --domain-config=" . BSLASH . DQUOTE . $domain_config . BSLASH . DQUOTE;
        }
        if ($host_config && $host_config ne '') {
            $commandline .= " --host-config=" . BSLASH . DQUOTE . $host_config . BSLASH . DQUOTE;
        }
        if ($additional_options) {
            my $escaped_additional_options = escape_additional_options_for_windows($additional_options);
            $commandline .= " $escaped_additional_options";
        }

        my $logfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".log";
        my $errfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".err";
        $commandline = SQUOTE . $commandline . " 1>" . $logfile . " 2>" . $errfile . SQUOTE;
        $commandline = "exec(" . $commandline . ");";
        $commandline = DQUOTE . $commandline . DQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "ec-perl", "-e", $commandline);

    }
    else {
        # Linux is comparatively simple, just some quotes around the script name in case of embedded spaces.
        # IMPORTANT NOTE: At this time the direct output of the script is lost in Linux, as I have not figured out how to
        # safely redirect it. Nothing shows up in the log file even when I appear to get the redirection correct; I believe
        # the script might be putting the output to /dev/tty directly (or something equally odd). Most of the time, it's not
        # really important since the vital information goes directly to $CATALINA_HOME/logs/catalina.out anyway. It can lose
        # important error messages if the paths are bad, etc. so this will be a JIRA.
        my $commandline = DQUOTE . $startup_script . DQUOTE;
        if ($domain_config && $domain_config ne '') {
            $commandline .= " --domain-config=" . DQUOTE . $domain_config . DQUOTE . " ";
        }
        if ($host_config && $host_config ne '') {
            $commandline .= " --host-config=" . DQUOTE . $host_config . DQUOTE . " ";
        }
        if ($additional_options) {
            $commandline .= " $additional_options";
        }
        $commandline = SQUOTE . $commandline . SQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);
    }
    my $cmdLine = create_command_line(\@systemcall);
    $jboss->set_property(startDomainServerLine => $cmdLine);
    $jboss->log_info("Command line for ecdaemon: $cmdLine");
    system($cmdLine);

}

sub startServer($){
    my ($scriptPhysicalLocation, $alternateConfigDomain, $alternateConfigHost) = @_;

    # $The quote and backslash constants are just a convenient way to represtent literal literal characters so it is obvious
    # in the concatentations. NOTE: BSLASH ends up being a single backslash, it needs to be doubled here so it does not
    # escape the right curly brace.

    my $operatingSystem = $^O;
    print qq{OS: $operatingSystem\n};

    # Ideally, the logs should exist in the step's workspace directory, but because the ecdaemon continues after the step is
    # completed the temporary drive mapping to the workspace is gone by the time we want to write to it. Instead, the log
    # and errors get the JOBSTEPID appended and it goes in the Tomcat root directory.
    my $LOGNAMEBASE = "jbossstartdomainserver";

    # If we try quoting in-line to get the final string exactly right, it will be confusing and ugly. Only the last
    # parameter to our outer exec() needs _literal_ single and double quotes inside the string itself, so we build that
    # parameter before the call rather than inside it. Using concatenation here both substitutes the variable values and
    # puts literal quote from the constants in the final value, but keeps any other shell metacharacters from causing
    # trouble.

    my @systemcall;

    if ($operatingSystem eq WIN_IDENTIFIER) {
        # Windows has a much more complex execution and quoting problem. First, we cannot just execute under "cmd.exe"
        # because ecdaemon automatically puts quote marks around every parameter passed to it -- but the "/K" and "/C"
        # option to cmd.exe can't have quotes (it sees the option as a parameter not an option to itself). To avoid this, we
        # use "ec-perl -e xxx" to execute a one-line script that we create on the fly. The one-line script is an "exec()"
        # call to our shell script. Unfortunately, each of these wrappers strips away or interprets certain metacharacters
        # -- quotes, embedded spaces, and backslashes in particular. We end up escaping these metacharacters repeatedly so
        # that when it gets to the last level it's a nice simple script call. Most of this was determined by trial and error
        # using the sysinternals procmon tool.
        my $commandline = BSLASH . BSLASH . BSLASH . DQUOTE . $scriptPhysicalLocation . BSLASH . BSLASH . BSLASH . DQUOTE;

        if ($alternateConfigDomain && $alternateConfigDomain ne '') {
            $commandline .= " --domain-config=" . BSLASH . BSLASH . BSLASH . DQUOTE . $alternateConfigDomain . BSLASH . BSLASH . BSLASH . DQUOTE;
        }
        if ($alternateConfigHost && $alternateConfigHost ne '') {
            $commandline .= " --host-config=" . BSLASH . BSLASH . BSLASH . DQUOTE . $alternateConfigHost . BSLASH . BSLASH . BSLASH . DQUOTE;
        }

        my $logfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".log";
        my $errfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".err";
        $commandline = SQUOTE . $commandline .  " 1>" . $logfile . " 2>" . $errfile . SQUOTE;
        $commandline = "exec(" . $commandline . ");";
        $commandline = DQUOTE . $commandline . DQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "ec-perl", "-e", $commandline);

    }
    else {
        # Linux is comparatively simple, just some quotes around the script name in case of embedded spaces.
        # IMPORTANT NOTE: At this time the direct output of the script is lost in Linux, as I have not figured out how to
        # safely redirect it. Nothing shows up in the log file even when I appear to get the redirection correct; I believe
        # the script might be putting the output to /dev/tty directly (or something equally odd). Most of the time, it's not
        # really important since the vital information goes directly to $CATALINA_HOME/logs/catalina.out anyway. It can lose
        # important error messages if the paths are bad, etc. so this will be a JIRA.
        my $commandline = DQUOTE . $scriptPhysicalLocation . DQUOTE;
        if ($alternateConfigDomain && $alternateConfigDomain ne '') {
            $commandline .= " --domain-config=" . DQUOTE . $alternateConfigDomain . DQUOTE . " ";
        }
        if ($alternateConfigHost && $alternateConfigHost ne '') {
            $commandline .= " --host-config=" . DQUOTE . $alternateConfigHost . DQUOTE . " ";
        }
        $commandline = SQUOTE . $commandline . SQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);
    }
    #print "Command Parameters:\n" . Dumper(@systemcall) . "--------------------\n";
    my %props;
    my $cmdLine = create_command_line(\@systemcall);
    $props{'startDomainServerLine'} = $cmdLine;
    setProperties(\%props);
    print "Command line for ecdaemon: $cmdLine\n";
    system($cmdLine);

}

sub start_standalone_server {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $scriptPhysicalLocation = $args{startup_script} || croak "'startup_script' is required param";
    my $alternateConfig = $args{optional_config};
    my $additional_options = $args{additional_options};

    # $The quote and backslash constants are just a convenient way to represtent literal literal characters so it is obvious
    # in the concatentations. NOTE: BSLASH ends up being a single backslash, it needs to be doubled here so it does not
    # escape the right curly brace.
    my $operatingSystem = $^O;
    print qq{OS: $operatingSystem\n};
    # Ideally, the logs should exist in the step's workspace directory, but because the ecdaemon continues after the step is
    # completed the temporary drive mapping to the workspace is gone by the time we want to write to it. Instead, the log
    # and errors get the JOBSTEPID appended and it goes in the Tomcat root directory.
    my $LOGNAMEBASE = "jbossstartstandaloneserver";
    # If we try quoting in-line to get the final string exactly right, it will be confusing and ugly. Only the last
    # parameter to our outer exec() needs _literal_ single and double quotes inside the string itself, so we build that
    # parameter before the call rather than inside it. Using concatenation here both substitutes the variable values and
    # puts literal quote from the constants in the final value, but keeps any other shell metacharacters from causing
    # trouble.
    my @systemcall;
    if ($operatingSystem eq WIN_IDENTIFIER) {
        # Windows has a much more complex execution and quoting problem. First, we cannot just execute under "cmd.exe"
        # because ecdaemon automatically puts quote marks around every parameter passed to it -- but the "/K" and "/C"
        # option to cmd.exe can't have quotes (it sees the option as a parameter not an option to itself). To avoid this, we
        # use "ec-perl -e xxx" to execute a one-line script that we create on the fly. The one-line script is an "exec()"
        # call to our shell script. Unfortunately, each of these wrappers strips away or interprets certain metacharacters
        # -- quotes, embedded spaces, and backslashes in particular. We end up escaping these metacharacters repeatedly so
        # that when it gets to the last level it's a nice simple script call. Most of this was determined by trial and error
        # using the sysinternals procmon tool.
        my $commandline = BSLASH . BSLASH . BSLASH . DQUOTE . $scriptPhysicalLocation . BSLASH . BSLASH . BSLASH . DQUOTE;
        if ($alternateConfig && $alternateConfig ne '') {
            $commandline .= " --server-config=" . BSLASH . DQUOTE . $alternateConfig . BSLASH . DQUOTE;
        }
        if ($additional_options) {
            my $escaped_additional_options = escape_additional_options_for_windows($additional_options);
            $commandline .= " $escaped_additional_options";
        }
        my $logfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".log";
        my $errfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".err";
        $commandline = SQUOTE . $commandline . " 1>" . $logfile . " 2>" . $errfile . SQUOTE;
        $commandline = "exec(" . $commandline . ");";
        $commandline = DQUOTE . $commandline . DQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "ec-perl", "-e", $commandline);
    }
    else {
        # Linux is comparatively simple, just some quotes around the script name in case of embedded spaces.
        # IMPORTANT NOTE: At this time the direct output of the script is lost in Linux, as I have not figured out how to
        # safely redirect it. Nothing shows up in the log file even when I appear to get the redirection correct; I believe
        # the script might be putting the output to /dev/tty directly (or something equally odd). Most of the time, it's not
        # really important since the vital information goes directly to $CATALINA_HOME/logs/catalina.out anyway. It can lose
        # important error messages if the paths are bad, etc. so this will be a JIRA.
        my $commandline = DQUOTE . $scriptPhysicalLocation . DQUOTE;
        if ($alternateConfig && $alternateConfig ne '') {
            $commandline .= " --server-config=" . DQUOTE . $alternateConfig . DQUOTE . " ";
        }
        if ($additional_options) {
            $commandline .= " $additional_options";
        }
        $commandline = SQUOTE . $commandline . SQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);
    }

    my $cmdLine = create_command_line(\@systemcall);
    $jboss->set_property(startStandaloneServerLine => $cmdLine);
    $jboss->log_info("Command line for ecdaemon: $cmdLine");
    system($cmdLine);
}

sub trim {
    my $s = shift;
    return $s if !$s;
    $s =~ s/^\s+|\s+$//g;
    return $s;
}

sub verify_host_controller_is_started_and_show_startup_info {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name};
    my $log_file_location = $args{log_file_location};

    if ($host_name) {
        $jboss->log_info("Checking whether JBoss Host Controller '$host_name' is started via master CLI.");
    }
    else {
        $jboss->log_warning("Verification via master CLI that host contoller is started will not be performed due to 'jbossHostName' parameter is not provided");
        $jboss->log_info("Retrieving list of host controllers via master CLI");
    }

    $jboss->log_info(
        sprintf(
            "Max time for performing checks - %s seconds, sleep between attempts - %s seconds",
            MAX_ELAPSED_TEST_TIME,
            SLEEP_INTERVAL_TIME
        )
    );

    my $elapsedTime = 0;
    my $startTimeStamp = time;
    my $attempts = 0;
    my %check_result = (
        summary            => "Verification via CLI that host controller started was not performed",
        status             => STATUS_ERROR,
        check_logs_via_cli => 0,
    );
    while (1) {
        $elapsedTime = time - $startTimeStamp;
        $jboss->log_info("Elapsed time so far: $elapsedTime seconds\n") if $attempts > 0;
        last unless $elapsedTime < MAX_ELAPSED_TEST_TIME;
        #sleep between attempts
        sleep SLEEP_INTERVAL_TIME if $attempts > 0;

        $attempts++;
        $jboss->log_info("----Attempt $attempts----");

        #execute check
        my $cli_command = qq|/:read-children-names(child-type=host)|;
        my %result = $jboss->run_command($cli_command);
        if ($result{code}) {
            my $summary = "Failed to connect to Master CLI"
                . ($host_name ? " for verication of Host Controller '$host_name' state"
                : " for retrieving list of available Host Controllers");
            %check_result = (
                summary            => $summary,
                status             => STATUS_ERROR,
                check_logs_via_cli => 0,
            );
            $jboss->log_info($summary);
            next;
        }
        else {
            my $json = $jboss->decode_answer($result{stdout});
            if (!$json || !$json->{result}) {
                my $summary = "Failed to read list of host controllers via Master CLI";
                %check_result = (
                    summary            => $summary,
                    status             => STATUS_ERROR,
                    check_logs_via_cli => 0,
                );
                $jboss->log_info($summary);
                next;
            }

            my @all_hosts = @{ $json->{result} };
            my %all_hosts_hash = map {$_ => 1} @all_hosts;

            if (!$host_name) {
                my $summary = "JBoss Host Controller has been launched, but verification that it is started is not performed (due to 'jbossHostName' parameter is not provided).";
                $summary .= "\nList of host controllers within Domain: " . join(", ", @all_hosts);
                %check_result = (
                    summary            => $summary,
                    status             => STATUS_WARNING,
                    check_logs_via_cli => 1,
                );
                $jboss->log_info($summary);
                last;
            }

            if ($all_hosts_hash{$host_name}) {
                my $host_state;
                eval {
                    $host_state = $jboss->run_command_and_get_json_result_with_failing_on_error(
                        command => "/host=$host_name/:read-attribute(name=host-state)"
                    );
                };
                if ($@) {
                    # e.g. usual error when host controller is connecting to the master (when it is already in the list of hosts within domain):
                    # 'Failed to get the list of the operation properties: "WFLYCTL0379: System boot is in process; execution of remote management operations is not currently available'
                    my $failure_description = $@;
                    my $summary = "Failed to check state of JBoss Host Controller '$host_name': $failure_description";
                    # most likely it is not possible to check logs or boot errors via CLI if we cannot check host state
                    %check_result = (
                        summary            => $summary,
                        status             => STATUS_ERROR,
                        check_logs_via_cli => 0,
                    );
                    $jboss->log_info($summary);
                    next
                }
                if ($host_state eq "running") {
                    my $summary = "JBoss Host Controller '$host_name' has been launched, host state is '$host_state'";
                    %check_result = (
                        summary            => $summary,
                        status             => STATUS_SUCCESS,
                        check_logs_via_cli => 1,
                    );
                    $jboss->log_info($summary);
                    last;
                }
                else {
                    my $summary = "State of JBoss Host Controller '$host_name' is '$host_state' instead of 'running'";
                    %check_result = (
                        summary            => $summary,
                        status             => STATUS_ERROR,
                        check_logs_via_cli => 1,
                    );
                    $jboss->log_info($summary);
                    next;
                }

            }
            else {
                my $summary = "JBoss Host Controller '$host_name' is not started (or not connected to Master)";
                $summary .= "\nList of host controllers within Domain: " . join(", ", @all_hosts);
                %check_result = (
                    summary            => $summary,
                    status             => STATUS_ERROR,
                    check_logs_via_cli => 0,
                );
                $jboss->log_info($summary);
                next;
            }
        }
    }

    if ($check_result{status} eq STATUS_ERROR) {
        $jboss->log_warning("--------$check_result{summary}--------");
        $jboss->add_error_summary($check_result{summary});
        $jboss->add_status_error();
    }
    elsif ($check_result{status} eq STATUS_WARNING) {
        $jboss->log_warning("--------$check_result{summary}--------");
        $jboss->add_warning_summary($check_result{summary});
        $jboss->add_status_warning();
    }
    else {
        $jboss->log_info("--------$check_result{summary}--------");
        $jboss->add_summary($check_result{summary});
    }

    eval {
        if ($log_file_location) {
            show_jboss_logs_from_requested_file(
                jboss             => $jboss,
                log_file_location => $log_file_location
            );
        }

        if ($host_name && $check_result{check_logs_via_cli}) {
            check_host_cotroller_boot_errors_via_cli(
                jboss     => $jboss,
                host_name => $host_name
            ) if $jboss->is_cli_command_supported_read_boot_errors();
            check_servers(jboss => $jboss, host_name => $host_name);
        }
        elsif (!$log_file_location) {
            $jboss->log_info("Please refer to JBoss logs on file system for more information");
            $jboss->add_summary("Please refer to JBoss logs on file system for more information");
        }
    };
    if ($@) {
        $jboss->log_warning("Failed to read information about startup: $@");
        $jboss->add_warning_summary("Failed to read information about startup");
        $jboss->add_status_warning();
        $jboss->log_info("Please refer to JBoss logs on file system for more information");
    }
}

sub verify_jboss_is_started_and_show_startup_info {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $log_file_location = $args{log_file_location};

    $jboss->log_info(
        sprintf(
            "Checking whether JBoss is started by connecting to CLI. Max time - %s seconds, sleep between attempts - %s seconds",
            MAX_ELAPSED_TEST_TIME,
            SLEEP_INTERVAL_TIME
        )
    );

    my $elapsedTime = 0;
    my $startTimeStamp = time;
    my $attempts = 0;
    my $recent_message;
    my $jboss_is_started;
    my $jboss_cli_is_available;
    while (!$jboss_is_started) {
        $elapsedTime = time - $startTimeStamp;
        $jboss->log_info("Elapsed time so far: $elapsedTime seconds\n") if $attempts > 0;
        last unless $elapsedTime < MAX_ELAPSED_TEST_TIME;
        #sleep between attempts
        sleep SLEEP_INTERVAL_TIME if $attempts > 0;

        $attempts++;
        $jboss->log_info("----Attempt $attempts----");

        #execute check
        my $cli_command = '/:read-attribute(name=server-state)';
        my %result = $jboss->run_command($cli_command);
        if ($result{code}) {
            $recent_message = "Failed to connect to CLI for verication of server state";
            $jboss->log_info($recent_message);
            next;
        }
        else {
            $jboss_cli_is_available = 1;

            my $json = $jboss->decode_answer($result{stdout});
            if (!$json) {
                $recent_message = "Failed to read server state via CLI";
                $jboss->log_info($recent_message);
                next;
            }

            my $server_state = lc $json->{result};
            if (!$server_state || $server_state ne "running") {
                $recent_message = "Server state is '$server_state' instead of 'running'";
                $jboss->log_info($recent_message);
                next;
            }
            else {
                $jboss_is_started = 1;
                $recent_message = "JBoss Standalone has been launched, server state is '$server_state'";
                $jboss->log_info($recent_message);
                last;
            }
        }
    }

    $jboss->log_info("--------$recent_message--------");
    $jboss->add_summary($recent_message);
    $jboss->add_status_error() unless $jboss_cli_is_available && $jboss_is_started;

    eval {
        if ($log_file_location) {
            show_jboss_logs_from_requested_file(
                jboss             => $jboss,
                log_file_location => $log_file_location
            );
        }

        if ($jboss_cli_is_available) {
            check_boot_errors_via_cli(jboss => $jboss) if $jboss->is_cli_command_supported_read_boot_errors();
            show_logs_via_cli(jboss => $jboss) if $jboss->is_cli_command_supported_read_log_file();
        }
        elsif (!$log_file_location) {
            # too many options of how log file location can be overriden, so let's do not guess where the logs are (at least for now)
            # also, we are not going to read logs by redirection of console output to files (like it is done for startup in case of Windows)
            # due to JBoss has good handling of logs by itself and it is not good idea to keep extra redirection of logs to EF workout etc.

            $jboss->log_info("Please refer to JBoss logs on file system for more information");
            $jboss->add_summary("Please refer to JBoss logs on file system for more information");
        }
    };
    if ($@) {
        $jboss->log_warning("Failed to read information about startup: $@");
        $jboss->add_summary("Failed to read information about startup");
        $jboss->add_status_warning();
        $jboss->log_info("Please refer to JBoss logs on file system for more information");
    }
}

sub verifyServerIsStarted($$){
    my ($configName, $cfg) = @_;

    # create args array
    my @args = ();
    my %props;

    my $ec = new ElectricCommander();
    $ec->abortOnError(0);

    my $url = '';
    my $user = '';
    my $pass = '';
    my %configuration;

    my $elapsedTime = 0;
    my $startTimeStamp = time;

    #getting all info from the configuration, url, user and pass
    if ($configName ne '') {
        %configuration = %$cfg;
        $url = $configuration{'jboss_url'};
    }

    print "Checking status of $url\n";

    #create all objects needed for response-request operations
    my $agent = LWP::UserAgent->new(env_proxy => 1,keep_alive => 1, timeout => 30);
    my $header = HTTP::Request->new(GET => $url);
    my $request = HTTP::Request->new('GET', $url, $header);
    #enter BASIC authentication
    #$request->authorization_basic($user, $pass);

    #setting variables for iterating
    my $retries = 0;
    my $attempts = 0;
    my $serverResponding = 0;
    do {
        $attempts++;
        print "----\nAttempt $attempts\n";

        #first attempt will always be done, no need to be forced to sleep
        if ($retries > 0) {
            my $testtimestart = time;
            #sleeping process during N seconds
            sleep SLEEP_INTERVAL_TIME;
            my $elapsedtesttime = time - $testtimestart;
            print "Elapsed interval time on attempt $attempts: $elapsedtesttime seconds\n"
        }
        #execute check
        my $response = $agent->request($request);
        # Check the outcome of the response
        if ($response->is_success) {
            #response was successful, server is responding and is available
            #a HTTP 200 could be returned in the most common scenario
            $serverResponding = SERVER_RESPONDING;
        }
        elsif ($response->is_error) {
            #response was erroneus, either server doesn't exist, port is unavailable
            #or server is overloaded. A HTTP 5XX response code can be expected
            $serverResponding = SERVER_NOT_RESPONDING;
        }

        print "Status returned: Attempt $attempts -> ", $response->status_line(), "\n";
        #get response code obtained
        my $httpCode = $response->code();
        print "HTTP code in attempt $attempts: $httpCode\n";

        $elapsedTime = time - $startTimeStamp;
        print "Elapsed time so far: $elapsedTime seconds\n";
        $retries++;

        print "\n";
    } while ($serverResponding == SERVER_NOT_RESPONDING && $elapsedTime < MAX_ELAPSED_TEST_TIME);

    #set any additional error or warning conditions here
    #there may be cases in which an error occurs and the exit code is 0.
    #we want to set to correct outcome for the running step

    #verifying server actual state
    if ($serverResponding == SERVER_RESPONDING) {
        #server is running
        print "------------------------------------\n";
        print "Server is up and running\n";
        print "------------------------------------\n";
        $ec->setProperty("/myJobStep/outcome", 'success');
    }
    else {
        if ($elapsedTime >= MAX_ELAPSED_TEST_TIME) {
            #server is not running
            print "----------------------------------------\n";
            print "Could not check if server was started, process timeout\n";
            print "----------------------------------------\n";
            $ec->setProperty("/myJobStep/outcome", 'error');
        }
        else {
            #server is not running
            print "----------------------------------------\n";
            print "Server is not responding\n";
            print "----------------------------------------\n";
            $ec->setProperty("/myJobStep/outcome", 'error');
        }
    }
}

1;