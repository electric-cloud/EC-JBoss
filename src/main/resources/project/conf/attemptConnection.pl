use strict;
use warnings;

use ElectricCommander;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;

use Data::Dumper;

use constant {
    SUCCESS => 0,
    ERROR   => 1,
};

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
    config_name                     => '$[config]',
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

eval {
    %result = $jboss->run_command(":read-attribute(name=product-version)");
};

my $plugin_error = $@;
if ($plugin_error) {
    my $summary .= "Plugin error: [$plugin_error]";
    $jboss->log_info("Test command run summary: $summary");

    my $suggestions = q{Cannot test connection to JBoss. It seems to be plugin error, please contact plugin developers.
};

    $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions);
    $jboss->add_error_summary( $summary . "\n\n" . $suggestions);

    $jboss->logErrorDiag("Create Configuration failed.\n\n$summary");
    $jboss->logInfoDiag($suggestions);

    $jboss->add_status_error();

    $jboss->log_error("Cannot test connection to JBoss");

    exit(ERROR);
}

my $stdout = $result{stdout};
my $stderr = $result{stderr};
my $code = $result{code};

my $summary = "";
$summary .= "Output: [$stdout]\n" if ($stdout);
$summary .= "Error output: [$stderr]\n" if ($stderr);
$summary .= "Return code: $code";
$jboss->log_info("Test command run summary: $summary");

if ($code) {
    my $suggestions = q{Connection to JBoss CLI cannot be establised. Reasons could be due to one or more of the following. Please ensure the following are correct and try again:
1. JBoss controller location (JBoss URL) - Is JBoss URL complete and reachable from the agent?
2. Physical location of the jboss client script (JBoss CLI script path)  - Is JBoss CLI script path correct?
3. Test Connection Resource - Is test resource correctly wired with Flow?  Is test resource correctly setup with JBoss?
4. Credentials - Are credentials correct? Is it possible to use these credentials to log in to JBoss using its console or CLI?
};

    $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions);
    $jboss->add_error_summary( $summary . "\n\n" . $suggestions);

    $jboss->logErrorDiag("Create Configuration failed.\n\n$summary");
    $jboss->logInfoDiag($suggestions);

    $jboss->add_status_error();

    $jboss->log_error("Connection failed");

    exit(ERROR);
}
else {
    if ($stderr) {
        my $suggestions = q{Connection to JBoss CLI can be establised but error output is catched during command run:
1. It is suggested to investigate possible reasons why error output is present during command run
};

        $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions);
        $jboss->add_warning_summary( $summary . "\n\n" . $suggestions);

        $jboss->logWarningDiag("Create Configuration failed.\n\n$summary");
        $jboss->logInfoDiag($suggestions);

        $jboss->add_status_warning();

        $jboss->log_warning("Connection succeeded but error output is catched during command run");
    }
    else {
        $jboss->log_info("Connection succeeded");
    }

    exit(SUCCESS);
}