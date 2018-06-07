# preamble.pl
$[/myProject/procedure_helpers/preamble]

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

use ElectricCommander;
use warnings;
use strict;
use Cwd;
use File::Spec;
use ElectricCommander::PropDB;

$| = 1;

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
};

main();

sub main {
    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1
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

sub get_all_hosts {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my $cli_command = qq|/:read-children-names(child-type=host)|;
    my $hosts = run_command_and_get_json_result_with_exiting_on_non_success(
        command => $cli_command,
        jboss   => $jboss
    );

    return $hosts;
}

sub run_command_and_get_json_result_with_exiting_on_non_success {
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

sub check_servers {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    my @all_servers = @{ run_command_and_get_json_result_with_exiting_on_non_success(
        command => "/host=$host_name/:read-children-names(child-type=server-config)",
        jboss   => $jboss
    ) };

    if (!@all_servers) {
        $jboss->log_info("There are no servers on host '$host_name'");
        $jboss->add_summary("There are no servers on host '$host_name'");
    }

    foreach my $server (@all_servers) {
        my $server_status = run_command_and_get_json_result_with_exiting_on_non_success(
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

sub escape_additional_options_for_windows {
    my $additional_options = shift || croak "required param is not provided (additional_options)";

    $additional_options =~ s|"|\"|gs;

    return $additional_options;
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

1;