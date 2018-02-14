package EC::Utils::JBossCommanderUtils;

use strict;
use warnings;

use base qw(Exporter);

use EC::Utils::CommonCommanderUtils qw(get_property_value);

our @EXPORT_OK = qw(
    get_config_param_value
    );

sub get_config_param_value {
    my %args = @_;
    my $configuration_name = $args{configuration_name} || croak "'configuration_name' is required param";
    my $parameter_name = $args{parameter_name} || croak "'parameter_name' is required param";

    my $property = "/myProject/jboss_cfgs/$configuration_name/$parameter_name";
    return get_property_value($property);
}

1;