$[/myProject/procedure_helpers/preamble]
use Data::Dumper;
no warnings qw/redefine/;
my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

my $jboss = EC::JBoss->new(
    project_name                    => $PROJECT_NAME,
    plugin_name                     => $PLUGIN_NAME,
    plugin_key                      => $PLUGIN_KEY,
);
my $cfg = $jboss->get_plugin_configuration();
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
    PLUGIN_NAME => '@PLUGIN_KEY@',
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

main();

1;
