#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

#########################
## createcfg.pl
#########################

use ElectricCommander;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;

#*****************************************************************************
use constant {
    SUCCESS => 0,
    ERROR   => 1,
};

my $opts;

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

#*****************************************************************************
# get an EC object
my $ec = ElectricCommander->new();
$ec->abortOnError(0);

ElectricCommander::PropMod::loadPerlCodeFromProperty($ec, '/myProject/jboss_driver/EC::Logger');

my $logger = EC::Logger->new(log_level_old_api_value => '$[log_level]');

#*****************************************************************************
sub configurationError {
    my ($errmsg) = @_;

    $ec->setProperty('/myJob/configError', $errmsg);
    $ec->setProperty('/myJobStep/summary', $errmsg);
    $ec->setProperty('/myJobStep/outcome', 'error');

    $logger->diagnostic_error("Create Configuration failed.\n\n$errmsg");

    $logger->error("Create configuration failed");

    return;
}

#*****************************************************************************
# load option list from procedure parameters
my $x       = $ec->getJobDetails($ENV{COMMANDER_JOBID});
my $nodeset = $x->find("//actualParameter");
foreach my $node ($nodeset->get_nodelist) {
    my $parm = $node->findvalue("actualParameterName");
    my $val  = $node->findvalue("value");
    $opts->{$parm} = "$val";
}

if (!defined $opts->{config} || "$opts->{config}" eq '') {
    configurationError("Config parameter must exist and be non-blank");
    exit(ERROR);
}

# check to see if a config with this name already exists before we do anything else
my $xpath    = $ec->getProperty("/myProject/jboss_cfgs/$opts->{config}");
my $property = $xpath->findvalue("//response/property/propertyName");

if (defined $property && "$property" ne "") {
    configurationError("A configuration named '$opts->{config}' already exists.");
    exit(ERROR);
}

my $cfg = ElectricCommander::PropDB->new($ec, "/myProject/jboss_cfgs");

# add all the options as properties
foreach my $key (keys %{$opts}) {
    if ($key eq 'config') {
        next;
    }

    $cfg->setCol("$opts->{config}", $key, "$opts->{$key}");
}

$logger->info("Configuration \"$opts->{config}\" created.\n");

exit(SUCCESS);
