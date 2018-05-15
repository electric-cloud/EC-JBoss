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

# -------------------------------------------------------------------------
# Constants
# -------------------------------------------------------------------------
use constant {
    SUCCESS                          => 0,
    ERROR                            => 1,
    SQUOTE                           => q{'},
    DQUOTE                           => q{"},
    BSLASH                           => q{\\},
    PLUGIN_NAME                      => 'EC-JBoss',
    WIN_IDENTIFIER                   => 'MSWin32',
    CREDENTIAL_ID                    => 'credential',
    MAX_ELAPSED_TEST_TIME            => 30,
    SLEEP_INTERVAL_TIME              => 3,
    EXPECTED_LOG_FILE_NAME           => 'server.log',
    NUMBER_OF_LINES_TO_TAIL_FROM_LOG => 100,

};
########################################################################
# trim - deletes blank spaces before and after the entered value in 
# the argument
#
# Arguments:
#   -untrimmedString: string that will be trimmed
#
# Returns:
#   trimmed string
#
########################################################################  
sub trim($) {
    my ($untrimmedString) = @_;
    my $string = $untrimmedString;

    #removes leading spaces
    $string =~ s/^\s+//;

    #removes trailing spaces
    $string =~ s/\s+$//;

    #returns trimmed string
    return $string;
}

# -------------------------------------------------------------------------
# Variables
# -------------------------------------------------------------------------
$::gEC = new ElectricCommander();
$::gEC->abortOnError(0);
$::gScriptPhysicalLocation = ($::gEC->getProperty("scriptphysicalpath"))->findvalue("//value");
$::gAlternateJBossConfig = ($::gEC->getProperty("alternatejbossconfig"))->findvalue("//value");

$::gServerConfig = ($::gEC->getProperty("serverconfig"))->findvalue("//value");

my %tempConfig = &getConfiguration($::gServerConfig);

if ($tempConfig{java_opts}) {
    my $new_java_opts = $tempConfig{java_opts};
    if ($ENV{JAVA_OPTS}) {
        $new_java_opts = $ENV{JAVA_OPTS} . ' ' . $new_java_opts;
    }
    $ENV{JAVA_OPTS} = $new_java_opts;
}

# -------------------------------------------------------------------------
# Main functions
# -------------------------------------------------------------------------
########################################################################
# main - contains the whole process to be done by the plugin, it builds 
#        the command line, sets the properties and the working directory
#
# Arguments:
#   none
#
# Returns:
#   none
#
########################################################################
sub main() {
    my $jboss = EC::JBoss->new(
        project_name                    => $PROJECT_NAME,
        plugin_name                     => $PLUGIN_NAME,
        plugin_key                      => $PLUGIN_KEY,
        no_cli_path_in_procedure_params => 1
    );

    exit_if_jboss_is_already_started(jboss => $jboss);

    startServer($::gScriptPhysicalLocation, $::gAlternateJBossConfig);

    verify_jboss_is_started(jboss => $jboss, startup_script => $::gScriptPhysicalLocation);
}

sub startServer($) {
    my ($scriptPhysicalLocation, $alternateConfig) = @_;
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
            $commandline .= " --server-config=" . BSLASH . BSLASH . BSLASH . DQUOTE . $alternateConfig . BSLASH . BSLASH . BSLASH . DQUOTE;
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
        $commandline = SQUOTE . $commandline . SQUOTE;
        print "Command line: $commandline\n";
        @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);
    }
    #print "Command Parameters:\n" . Dumper(@systemcall) . "--------------------\n";
    my %props;
    my $cmdLine = createCommandLine(\@systemcall);
    $props{'startStandaloneServerLine'} = $cmdLine;
    setProperties(\%props);
    print "Command line for ecdaemon: $cmdLine\n";
    system($cmdLine);
}

########################################################################
# createCommandLine - creates the command line for the invocation
# of the program to be executed.
#
# Arguments:
#   -arr: array containing the command name (must be the first element) 
#         and the arguments entered by the user in the UI
#
# Returns:
#   -the command line to be executed by the plugin
#
########################################################################
sub createCommandLine($) {
    my ($arr) = @_;
    my $commandName = @$arr[0];
    my $command = $commandName;
    shift(@$arr);
    foreach my $elem (@$arr) {
        $command .= " $elem";
    }
    return $command;
}

########################################################################
# setProperties - set a group of properties into the Electric Commander
#
# Arguments:
#   -propHash: hash containing the ID and the value of the properties 
#              to be written into the Electric Commander
#
# Returns:
#   none
#
########################################################################
sub setProperties($) {
    my ($propHash) = @_;
    foreach my $key (keys % $propHash) {
        my $val = $propHash->{$key};
        $::gEC->setProperty("/myCall/$key", $val);
    }
}
##########################################################################
# getConfiguration - get the information of the configuration given
#
# Arguments:
#   -configName: name of the configuration to retrieve
#
# Returns:
#   -configToUse: hash containing the configuration information
#
#########################################################################
sub getConfiguration($) {
    my ($configName) = @_;
    my %configToUse;
    my $proj = "$[/myProject/projectName]";
    my $pluginConfigs = new ElectricCommander::PropDB($::gEC, "/projects/$proj/jboss_cfgs");
    my %configRow = $pluginConfigs->getRow($configName);
    # Check if configuration exists
    unless (keys(%configRow)) {
        print 'Error: Configuration doesn\'t exist';
        exit ERROR;
    }
    # Get user/password out of credential
    my $xpath = $::gEC->getFullCredential($configRow{credential});
    $configToUse{'user'} = $xpath->findvalue("//userName");
    $configToUse{'password'} = $xpath->findvalue("//password");
    foreach my $c (keys %configRow) {
        #getting all values except the credential that was read previously
        if ($c ne CREDENTIAL_ID) {
            $configToUse{$c} = $configRow{$c};
        }
    }

    if ($configToUse{jboss_url} !~ m/^https?/s) {
        $configToUse{jboss_url} = 'http://' . $configToUse{jboss_url};
        print "Provided URL is not absolute. Let's assume that it's http: $configToUse{jboss_url}\n";
    }
    return %configToUse;
}

sub verify_jboss_is_started {
    my %args = @_;
    my $jboss = $args{jboss} || croak "'jboss' is required param";
    my $startup_script = $args{startup_script} || croak "'startup_script' is required param";

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
            $recent_message = "JBoss is not started - failed to connect to CLI";
            $jboss->log_info($recent_message);
            next;
        }
        else {
            $jboss_cli_is_available = 1;
            $jboss->process_response(%result);

            my $json = $jboss->decode_answer($result{stdout});
            if (!$json) {
                $recent_message = "Cannot convert JBoss response into JSON";
                $jboss->log_info($recent_message);
                next;
            }

            my $server_state = lc $json->{result};
            if (!$server_state || $server_state ne "running") {
                $recent_message = "Connected to CLI, but server state is '$server_state' instead of 'running'";
                $jboss->log_info($recent_message);
                next;
            }
            else {
                $jboss_is_started = 1;
                $recent_message = "JBoss Standalone is up and running";
                $jboss->log_info($recent_message);
                last;
            }
        }
    }

    $jboss->log_info("--------$recent_message--------");
    $jboss->set_property(summary => $recent_message);
    $jboss->error() unless $jboss_is_started;

    eval {
        if ($jboss_cli_is_available) {
            show_logs_via_cli(jboss => $jboss);
            check_boot_errors_via_cli(jboss => $jboss);
        }
        else {
            show_logs_via_file(jboss => $jboss, startup_script => $startup_script);
        }
    };
    if ($@) {
        $jboss->log_warning("Failed to read JBoss logs: $@");
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
        $jboss->process_response(%result);

        my $json = $jboss->decode_answer($result{stdout});
        $jboss->bail_out("Cannot convert JBoss response into JSON") if !$json;

        my $launch_type = lc $json->{result};
        if (!$launch_type || $launch_type ne "standalone") {
            $jboss->log_warning("JBoss is started, but operating mode is '$launch_type' instead of 'standalone'");
            $jboss->bail_out("JBoss is started, but operating mode is '$launch_type' instead of 'standalone'");
        }
        else {
            $jboss->log_warning("JBoss is already started in expected operating mode '$launch_type'");
            $jboss->set_property(summary => "JBoss is already started in expected operating mode '$launch_type'");
            $jboss->warning();
            exit SUCCESS;
        }
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
        $jboss->log_info("JBoss logs  ($assumption_sting): " . $result{stdout});
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
    }
    else {
        $jboss->process_response(%result);

        my $json = $jboss->decode_answer($result{stdout});
        $jboss->log_warning("Cannot convert JBoss response into JSON when reading boot errors") unless $json;
        $jboss->log_warning("Unexpected JBoss response when reading boot errors") unless exists $json->{result};

        my $boot_errors = $json->{result};

        if (@$boot_errors) {
            $jboss->log_warning("JBoss boot errors: " . $result{stdout});
        }
        else {
            $jboss->log_info("No JBoss boot errors detected");
        }
    }
}

sub show_logs_via_file {
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

main();
1;
