# -------------------------------------------------------------------------
# File
#    startStandaloneServer.pl
#
# Dependencies
#    None
#
# Template Version
#    1.0
#
# Date
#    08/31/2011
#
# Engineer
#    Alonso Blanco
#
# Copyright (c) 2011 Electric Cloud, Inc.
# All rights reserved
# -------------------------------------------------------------------------

# -------------------------------------------------------------------------
# Includes
# -------------------------------------------------------------------------
use ElectricCommander;
use warnings;
use strict;
use Cwd;
use File::Spec;
use ElectricCommander::PropDB;
$|=1;

# -------------------------------------------------------------------------
# Constants
# -------------------------------------------------------------------------
use constant {
    SUCCESS => 0,
    ERROR   => 1,
    SQUOTE => q{'},
    DQUOTE => q{"},
    BSLASH => q{\\},
    PLUGIN_NAME => 'EC-JBoss',
    WIN_IDENTIFIER => 'MSWin32',
    CREDENTIAL_ID => 'credential',
    MAX_ELAPSED_TEST_TIME => 30,
    SLEEP_INTERVAL_TIME => 3,
    SERVER_RESPONDING => 1,
    SERVER_NOT_RESPONDING => 0,
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
$::gScriptPhysicalLocation = ($::gEC->getProperty("scriptphysicalpath") )->findvalue("//value");
$::gAlternateJBossConfig = ($::gEC->getProperty("alternatejbossconfig") )->findvalue("//value");

$::gServerConfig = ($::gEC->getProperty("serverconfig") )->findvalue("//value");

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
    my $cmdLine = '';
    my %props;
    # start admin server using ecdaemon
    my %config = getConfiguration($::gServerConfig);

    if (!is_dryrun_enabled() && isServerAlreadyAlive(\%config)) {
        print "Server is already alive on url $config{jboss_url}";
        exit 0;
    }
    startServer($::gScriptPhysicalLocation, $::gAlternateJBossConfig);
    verifyServerIsStarted($::gServerConfig);
    setProperties(\%props);
}

sub isServerAlreadyAlive {
    my ($config) = @_;

    my $url = $config->{jboss_url};
    my $agent = LWP::UserAgent->new(env_proxy => 1,keep_alive => 1, timeout => 30);
    my $header = HTTP::Request->new(GET => $url);
    my $request = HTTP::Request->new('GET', $url, $header);

    my $response = $agent->request($request);
    if ($response->is_success()) {
        return 1;
    }
    return 0;
}
########################################################################
# startServer - uses ecdaemon for starting a Server
#
# Arguments:
#   -jboss server script: absolute path to managed server script
#   -server name: name of the instance of the managed server
#   -URL: URL (including protocol and port) of the Admin Server of the domain
#   -user: user of the admin server
#   -password: password of the admin server
#
# Returns:
#   none
#
########################################################################
sub startServer($){
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
        $commandline = SQUOTE . $commandline .  " 1>" . $logfile . " 2>" . $errfile . SQUOTE;
        $commandline = "exec(" . $commandline . ");";
        $commandline = DQUOTE . $commandline . DQUOTE;
        print "Linea de comando: $commandline\n";
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
        @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);
    }
    #print "Command Parameters:\n" . Dumper(@systemcall) . "--------------------\n";
    my %props;
    my $cmdLine = createCommandLine(\@systemcall);
    $props{'startStandaloneServerLine'} = $cmdLine;
    setProperties(\%props);
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
sub getConfiguration($){
    my ($configName) = @_;
    my %configToUse;
    my $proj = "$[/myProject/projectName]";
    my $pluginConfigs = new ElectricCommander::PropDB($::gEC,"/projects/$proj/jboss_cfgs");
    my %configRow = $pluginConfigs->getRow($configName);
    # Check if configuration exists
    unless(keys(%configRow)) {
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
##########################################################################
# verifyServerIsStarted - verifies if the specified managed server is running.
#
# Arguments:
#   -ServerName: name of the server instance
#   -URL: Managed Server URL (including protocol and port)
#   -User: user for logging into the admin server
#   -Password: password for logging into the admin server
#
# Returns:
#   none
#
#########################################################################
sub verifyServerIsStarted {
    my ($configName, $check_once) = @_;
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
        %configuration = getConfiguration($configName);
        $url = $configuration{'jboss_url'};
    }
    print "Checking status of $url\n";
    #create all objects needed for response-request operations
    my $agent = LWP::UserAgent->new(env_proxy => 1,keep_alive => 1, timeout => 30);
    my $header = HTTP::Request->new(GET => $url);
    my $request = HTTP::Request->new('GET', $url, $header);
    # enter BASIC authentication
    #$request->authorization_basic($user, $pass);
    #setting variables for iterating
    my $retries = 0;
    my $attempts = 0;
    my $serverResponding = 0;
    do {
        $attempts++;
        print "----\nAttempt $attempts\n";
        #first attempt will always be done, no need to be forced to sleep
        if($retries > 0) {
            my $testtimestart = time;
            #sleeping process during N seconds
            sleep SLEEP_INTERVAL_TIME;
            my $elapsedtesttime = time - $testtimestart;
            print "Elapsed interval time on attempt $attempts: $elapsedtesttime seconds\n"
        }
        #execute check
        my $response = $agent->request($request);
        # Check the outcome of the response
        if ($response->is_success){
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
    if ($serverResponding == SERVER_RESPONDING){
        #server is running
        print "------------------------------------\n";
        print "Server is up and running\n";
        print "------------------------------------\n";
        $ec->setProperty("/myJobStep/outcome", 'success');
    }
    else {
        if($elapsedTime >= MAX_ELAPSED_TEST_TIME){
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


sub is_dryrun_enabled {
    my $dryrun = 0;
    eval {
        $dryrun = $::gEC->getProperty(
            '/plugins/@PLUGIN_KEY@/project/dryrun'
        )->findvalue('//value')->string_value();
    };
    return $dryrun;
}

main();
1;
