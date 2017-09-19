# -------------------------------------------------------------------------
# File
#    shutdownInstance.pl
#
# Dependencies
#    None
#
# Template Version
#    1.0
#
# Date
#    09/01/2011
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
use diagnostics;
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

$::gServerConfig = ($::gEC->getProperty("serverconfig") )->findvalue("//value");
my %tempConfig = &getConfiguration($::gServerConfig);

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

    my $rawUrl = '';
    my $user = '';
    my $pass = '';
    my %configuration;

    my $content;

    #getting all info from the configuration, url, user and pass
    if ($::gServerConfig ne '') {
        %configuration = getConfiguration($::gServerConfig);
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
    setProperties(\%props);
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

    return %configToUse;
}

main();

1;
