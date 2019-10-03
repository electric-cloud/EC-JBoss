use strict;
use warnings;

use ElectricCommander;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;

use Data::Dumper;

#*****************************************************************************
use constant {
    SUCCESS => 0,
    ERROR   => 1,
};

#*****************************************************************************
my $ec = ElectricCommander->new();

my $PROJECT_NAME = '$[/myProject/projectName]';
my $PLUGIN_NAME = '@PLUGIN_NAME@';
my $PLUGIN_KEY = '@PLUGIN_KEY@';

ElectricCommander::PropMod::loadPerlCodeFromProperty($ec, '/myProject/jboss_driver/EC::JBoss');

my $jboss = EC::JBoss->new(
    project_name                    => $PROJECT_NAME,
    plugin_name                     => $PLUGIN_NAME,
    plugin_key                      => $PLUGIN_KEY,
    no_cli_path_in_procedure_params => 1,
    _credentials                    => {
        config              => '$[config]',
        jboss_url           => '$[jboss_url]',
        scriptphysicalpath  => '$[scriptphysicalpath]',
        credential          => '$[credential]',
        test_connection     => '$[test_connection]',
        test_connection_res => '$[test_connection_res]',
        log_level           => '$[log_level]',
        java_opts           => '$[java_opts]'
    }
);

my %result;
my $stdout;
my $stderr;
my $code;

eval {
    %result = $jboss->run_command(
        command => ":read-attribute(name=product-version)"
    );
};

if ($@) {
    $stdout = "";
    $stderr = "Plugin error: " . $@;
    $code = 1;
}
else {
    $stdout = $result{stdout};
    $stderr = $result{stderr};
    $code = $result{code};
}

$jboss->log_info('STDOUT: ' . $stdout) if ($stdout);
$jboss->log_info('STDERR: ' . $stderr) if ($stderr);
$jboss->log_info('EXIT_CODE: ' . $code);

if ($code) {
    $ec->setProperty('/myJob/configError', $stderr);
    $ec->setProperty('/myJobStep/summary', $stderr);

    $jboss->log_error("Connection failed: $code");

    exit(ERROR);
}
else {
    if ($stderr) {
        $ec->setProperty('/myJob/configError', $stderr);
        $ec->setProperty('/myJobStep/summary', $stderr);

        $jboss->log_warning("Connection succeeded but contains error output is catched during command run");
    }
    else {
        $jboss->log_info("Connection succeeded");
    }

    exit(SUCCESS);
}