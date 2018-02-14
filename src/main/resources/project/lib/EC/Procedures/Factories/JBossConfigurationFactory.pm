package EC::Procedures::Factories::JBossConfigurationFactory;

use strict;
use warnings;

use base qw(Exporter);

use EC::Procedures::Models::JBossConfiguration;
use EC::Utils::JBossCommanderUtils qw(get_config_param_value);
use EC::Utils::CommonCommanderUtils qw(get_credential);

our @EXPORT_OK = qw(get_jboss_configuration_model);

sub get_jboss_configuration_model {
    my %args = @_;
    my $configuration_name = $args{configuration_name} || croak "'configuration_name' is required param";
    my $credential_name = get_config_param_value($configuration_name, 'credential');
    my $credential = get_credential($credential_name);

    my $jboss_configuration = EC::Procedures::Models::JBossConfiguration->new(
        configuration_name      => $configuration_name,
        controller_url          => get_config_param_value($configuration_name, 'jboss_url'),
        cli_script_location     => get_config_param_value($configuration_name, 'scriptphysicalpath'),
        user_name               => $credential->{user_name},
        password                => $credential->{password},
        log_level               => get_config_param_value($configuration_name, 'log_level'),
        additional_java_options => get_config_param_value($configuration_name, 'java_opts')
    );

    return $jboss_configuration;
}

1;