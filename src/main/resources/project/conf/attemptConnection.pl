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
ElectricCommander::PropMod::loadPerlCodeFromProperty($ec, '/myProject/jboss_driver/EC::Logger');

my $logger = EC::Logger->new(log_level_old_api_value => '$[log_level]');

my $cred_xpath = $ec->getFullCredential('$[credential]');
my $username = $cred_xpath->findvalue("//userName");
my $password = $cred_xpath->findvalue("//password");

my %result;

eval {
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
            user                => $username,
            password            => $password,
            test_connection     => '$[test_connection]',
            test_connection_res => '$[test_connection_res]',
            log_level           => '$[log_level]',
            java_opts           => '$[java_opts]'
        }
    );
    %result = $jboss->run_command(":read-attribute(name=product-version)");
};

my $suggestions_on_error = q{Connection to JBoss CLI cannot be established. Reasons could be due to one or more of the following. Please ensure the following are correct and try again:
1. JBoss controller location (JBoss URL) - Is JBoss URL complete and reachable from the agent?
2. Physical location of the jboss client script (JBoss CLI script path)  - Is JBoss CLI script path correct?
3. Check Connection Resource - Is the resource correctly wired with Flow?  Is the resource correctly setup with JBoss?
4. Credentials - Are credentials correct? Is it possible to use these credentials to log in to JBoss using its console or CLI?
};

my $suggestions_on_warning = q{Connection to JBoss CLI can be established but error output is caught during command run:
1. It is suggested to investigate possible reasons why error output is present during command run
};

my $plugin_error = $@;
if ($plugin_error) {
    my $summary .= "Error reported during plugin usage: [$plugin_error]";
    $logger->info("Error reported during plugin usage: $summary");

    $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions_on_error);
    $ec->setProperty('/myJobStep/summary', $summary . "\n\n" . $suggestions_on_error);
    $ec->setProperty('/myJobStep/outcome', 'error');

    $logger->diagnostic_error("Create Configuration failed.\n\n$summary");
    $logger->diagnostic_info($suggestions_on_error);

    $logger->error("Cannot check connection to JBoss");

    exit(ERROR);
}

my $stdout = $result{stdout};
my $stderr = $result{stderr};
my $code = $result{code};

my $summary = "";
$summary .= "Output: [$stdout]\n" if ($stdout);
$summary .= "Error output: [$stderr]\n" if ($stderr);
$summary .= "Return code: $code";
$logger->info("Test command run summary: $summary");

if ($code) {
    $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions_on_error);
    $ec->setProperty('/myJobStep/summary', $summary . "\n\n" . $suggestions_on_error);
    $ec->setProperty('/myJobStep/outcome', 'error');

    $logger->diagnostic_error("Create Configuration failed.\n\n$summary");
    $logger->diagnostic_info($suggestions_on_error);

    $logger->error("Connection failed");

    exit(ERROR);
}
else {
    if ($stderr) {
        $ec->setProperty('/myJob/configError', $summary . "\n\n" . $suggestions_on_warning);
        $ec->setProperty('/myJobStep/summary', $summary . "\n\n" . $suggestions_on_warning);
        $ec->setProperty('/myJobStep/outcome', 'warning');

        $logger->diagnostic_warning("Connection succeeded but error output is caught during command run.\n\n$summary");
        $logger->diagnostic_info($suggestions_on_warning);

        $logger->warning("Connection succeeded but error output is caught during command run");
    }
    else {
        $logger->info("Connection succeeded");
    }

    exit(SUCCESS);
}