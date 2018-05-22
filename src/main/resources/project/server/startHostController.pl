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
use File::Basename;

$| = 1;

use constant {
    SUCCESS               => 0,
    ERROR                 => 1,
    SQUOTE                => q{'},
    DQUOTE                => q{"},
    BSLASH                => q{\\},
    WIN_IDENTIFIER        => 'MSWin32',
    MAX_ELAPSED_TEST_TIME => 60,
    SLEEP_INTERVAL_TIME   => 3,
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
        hostName
        additionalOptions
        /);

    my $param_startup_script = $params->{startupScript};
    my $param_domain_config = $params->{domainConfig};
    my $param_host_config = $params->{hostConfig};
    my $param_host_name = $params->{hostName};
    my $param_additional_options = $params->{additionalOptions};

    if (!$param_startup_script) {
        $jboss->bail_out("Required parameter 'startupScript' is not provided");
    }

    if (!$param_host_name) {
        $jboss->log_warning("Verification via master CLI that host contoller is started will not be performed due to 'hostName' parameter is not provided");
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

    verify_host_controller_is_started(
        jboss          => $jboss,
        startup_script => $param_startup_script,
        host_name      => $param_host_name
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
            $jboss->add_summary("JBoss is started, but operating mode is '$launch_type' instead of 'domain'");
            $jboss->add_status_error();
            exit ERROR;
        }
        else {
            my @all_hosts = @{ get_all_hosts(jboss => $jboss) };
            my %all_hosts_hash = map {$_ => 1} @all_hosts;
            if ($all_hosts_hash{$host_name}) {
                $jboss->log_warning("JBoss Host Controller '$host_name' is already started");
                $jboss->add_summary("JBoss Host Controller '$host_name' is already started");
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

sub verify_host_controller_is_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $startup_script = $args{startup_script} || croak "'startup_script' is required param";
    my $host_name = $args{host_name};

    my $master_cli_is_available;
    my $host_controller_is_connected;
    my $host_controller_is_connected_and_running;

    if ($host_name) {
        $jboss->log_info("Checking whether JBoss Host Controller '$host_name' is started via master CLI.");
    }
    else {
        $jboss->log_warning("Verification via master CLI that host contoller is started will not be performed due to 'hostName' parameter is not provided");
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
    my $recent_message;
    while (!$host_controller_is_connected_and_running) {
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
            $recent_message = "Failed to connect to Master CLI for verication of Host Controller '$host_name' state";
            $jboss->log_info($recent_message);
            next;
        }
        else {
            $master_cli_is_available = 1;

            my $json = $jboss->decode_answer($result{stdout});
            if (!$json || !$json->{result}) {
                $recent_message = "Failed to read list of host controllers within Master CLI";
                $jboss->log_info($recent_message);
                next;
            }

            my @all_hosts = @{ $json->{result} };
            my %all_hosts_hash = map {$_ => 1} @all_hosts;
            if ($all_hosts_hash{$host_name}) {
                $host_controller_is_connected = 1;
                my $host_state = run_command_and_get_json_result_with_exiting_on_non_success(
                    command => "/host=$host_name/:read-attribute(name=host-state)",
                    jboss   => $jboss
                );
                if ($host_state eq "running") {
                    $host_controller_is_connected_and_running = 1;
                    $recent_message = "JBoss Host Controller '$host_name' has been launched, host state is '$host_state'";
                    $jboss->log_info($recent_message);
                    last;
                }
                else {
                    $recent_message = "State of JBoss Host Controller '$host_name' is '$host_state' instead of 'running'";
                    $jboss->log_info($recent_message);
                    next;
                }

            }
            else {
                $recent_message = "JBoss Host Controller '$host_name' is not started (or not connected to Master)";
                $jboss->log_info($recent_message);
                next;
            }
        }
    }

    $jboss->log_info("--------$recent_message--------");
    $jboss->add_summary($recent_message);
    $jboss->add_status_error() unless $host_controller_is_connected_and_running;

    eval {
        if ($host_name && $host_controller_is_connected) {
            check_boot_errors_via_cli_for_host_cotroller(jboss => $jboss, host_name => $host_name);
            show_logs_via_cli_for_host_cotroller(jboss => $jboss, host_name => $host_name);
        }
        else {
            show_logs_via_file_for_host_cotroller(jboss => $jboss, startup_script => $startup_script);
        }
    };
    if ($@) {
        $jboss->log_warning("Failed to read information about startup: $@");
        $jboss->add_summary("Failed to read information about startup");
        $jboss->add_status_warning()
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

    my $json = run_command_and_get_json_with_exiting_on_non_success(
        command => $command,
        jboss   => $jboss
    );
    if (!defined $json->{result}) {
        $jboss->add_summary("JBoss replied with undefined result when expectation was to verify the result: " . (encode_json $json));
        $jboss->add_status_error();
        exit ERROR;
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
        my $summary = "JBoss replied with outcome other than success: " . (encode_json $json);
        $jboss->log_warning($summary);
        die "Error when converting JBoss response into JSON";

        $jboss->add_summary();
        $jboss->add_status_error();
        exit ERROR;
    }

    return $json;
}

sub run_command_and_get_json_with_failing_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = run_command_with_exiting_on_error(command => $command, jboss => $jboss);

    my $json = $jboss->decode_answer($result{stdout});
    if (!$json) {
        $jboss->log_warning("Error when converting JBoss response into JSON");
        die "Error when converting JBoss response into JSON";
    }

    return $json;
}

sub run_command_with_failing_on_error {
    my %args = @_;
    my $command = $args{command} || croak "'command' is required param";
    my $jboss = $args{jboss} || croak "'jboss' is required param";

    my %result = $jboss->run_command($command);

    if ($result{code}) {
        my $jboss_response_failure_description = $jboss->get_jboss_response_failure_description(jboss_response =>
            \%result);
        $jboss->log_warning("Error when running CLI command: $jboss_response_failure_description");
        die "Error when running CLI command: $jboss_response_failure_description";
    }

    return %result;
}

sub get_error_summary {

}

sub show_logs_via_cli_for_host_cotroller {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    my $host_state = run_command_and_get_json_result_with_exiting_on_non_success(
        command => "/host=$host_name/:read-children-names(child-type=server)",
        jboss   => $jboss
    );

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

sub check_boot_errors_via_cli_for_host_cotroller {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'host_name' is required param";

    $jboss->log_info("Checking boot errors via CLI");

    my $cli_command = "/host=$host_name/core-service=management/:read-boot-errors";

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

sub show_logs_via_file_for_host_cotroller {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $startup_script = $args{startup_script} || croak "'startup_script' is required param";
    my $jboss_cli_script = $jboss->{script_path};
    my $log_file_path;

    if ($startup_script =~ m/bin/) {
        $log_file_path = File::Spec->catfile(dirname(dirname($startup_script)), 'standalone', 'log',
            EXPECTED_LOG_FILE_NAME);
    }
    elsif ($jboss_cli_script =~ m/bin/) {
        $log_file_path = File::Spec->catfile(dirname(dirname($startup_script)), 'standalone', 'log',
            EXPECTED_LOG_FILE_NAME);
    }
    else {
        $jboss->log_warning("Cannot find JBoss log file");
        return;
    }

    my $assumption_sting = sprintf(
        "assumption is that path log file is %s, tailing %u lines",
        $log_file_path,
        NUMBER_OF_LINES_TO_TAIL_FROM_LOG
    );

    $jboss->log_info("Showing logs from file system ($assumption_sting)");

    if (-f $log_file_path) {
        my $recent_log_lines = get_recent_log_lines(
            jboss        => $jboss,
            file         => $log_file_path,
            num_of_lines => NUMBER_OF_LINES_TO_TAIL_FROM_LOG
        );
        $jboss->log_info("JBoss logs  ($assumption_sting):\n   | " . join('   | ', @$recent_log_lines));
    }
    else {
        $jboss->log_warning("Cannot find JBoss log file '$log_file_path'");
    }
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

sub escape_additional_options_for_windows {
    my $additional_options = shift || croak "required param is not provided (additional_options)";

    $additional_options =~ s|"|\"|gs;

    return $additional_options;
}

1;