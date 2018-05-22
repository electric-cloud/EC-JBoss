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

$|=1;

use constant {
    SUCCESS => 0,
    ERROR   => 1,
    SQUOTE => q{'},
    DQUOTE => q{"},
    BSLASH => q{\\},
    WIN_IDENTIFIER => 'MSWin32',
    MAX_ELAPSED_TEST_TIME => 60,
    SLEEP_INTERVAL_TIME => 3,
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
        alternatejbossconfig
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

    if ($param_host_name) {
        exit_if_host_controller_is_already_started(
            jboss => $jboss,
            host_name => $param_host_name
        );
    }

    start_host_controller(
        startup_script     => $param_startup_script,
        optional_config_domain    => $param_domain_config,
        optional_config_host    => $param_host_config,
        additional_options => $param_additional_options,
        jboss              => $jboss
    );

    verify_host_controller_is_started(
        jboss => $jboss,
        startup_script => $param_startup_script,
        host_name => $param_host_name
    );
}

sub start_host_controller {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $scriptPhysicalLocation = $args{startup_script} || croak "'startup_script' is required param";
    my $alternateConfigDomain = $args{optional_config_domain};
    my $alternateConfigHost = $args{optional_config_host};
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
        my $commandline = BSLASH . BSLASH . BSLASH . DQUOTE . $scriptPhysicalLocation . BSLASH . BSLASH . BSLASH . DQUOTE;

        if ($alternateConfigDomain && $alternateConfigDomain ne '') {
            $commandline .= " --domain-config=" . BSLASH . DQUOTE . $alternateConfigDomain . BSLASH . DQUOTE;
        }
        if ($alternateConfigHost && $alternateConfigHost ne '') {
            $commandline .= " --host-config=" . BSLASH . DQUOTE . $alternateConfigHost . BSLASH . DQUOTE;
        }
        if ($additional_options) {
            my $escaped_additional_options = escape_additional_options_for_windows($additional_options);
            $commandline .= " $escaped_additional_options";
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

sub verify_host_controller_is_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $startup_script = $args{startup_script} || croak "'startup_script' is required param";
    my $host_name = $args{host_name};

    $jboss->log_info(
        sprintf(
            "Checking whether JBoss Host Controller '%s' is started by connecting to CLI. Max time - %s seconds, sleep between attempts - %s seconds",
            $host_name,
            MAX_ELAPSED_TEST_TIME,
            SLEEP_INTERVAL_TIME
        )
    );

    my $elapsedTime = 0;
    my $startTimeStamp = time;
    my $attempts = 0;
    my $recent_message;
    my $host_controller_is_started;
    my $master_cli_is_available;
    while (!$host_controller_is_started) {
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
                #todo
            }
            else {
                $jboss->log_info("JBoss Host Controller '$host_name' is not started (or not connected to Master) yet");
                next;
            }
        }
    }

    #todo
    $jboss->log_info("--------$recent_message--------");
    $jboss->add_summary($recent_message);
    $jboss->add_status_error() unless $master_cli_is_available && $host_controller_is_started;

    eval {
        if ($master_cli_is_available) {
            check_boot_errors_via_cli(jboss => $jboss);
            show_logs_via_cli(jboss => $jboss);
        }
        else {
            show_logs_via_file(jboss => $jboss, startup_script => $startup_script);
        }
    };
    if ($@) {
        $jboss->log_warning("Failed to read information about startup: $@");
        $jboss->add_summary("Failed to read information about startup");
        $jboss->add_status_warning()
    }
}

sub exit_if_host_controller_is_already_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $host_name = $args{host_name} || croak "'jboss' is required param";

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

1;