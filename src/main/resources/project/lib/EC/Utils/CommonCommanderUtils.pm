package EC::Utils::CommonCommanderUtils;

use strict;
use warnings;

use base qw(Exporter);
use Carp;

use ElectricCommander;

our @EXPORT_OK = qw(
    get_commander
    get_property_value
    get_current_procedure_param_value
    get_current_procedure_params_hashref
    get_credential
    );

{
    # similar to 'state' in newer perl
    my $commander = ElectricCommander->new();
    sub get_commander {
        return $commander;
    }
}

sub get_property_value {
    my $property_name = shift;
    my $property_value = get_commander()->getProperty($property_name)->findvalue('//value') . '';
    return $property_value;
}

sub get_current_procedure_param_value {
    my $parameter_name = shift;
    # check for the relative property in current context (property name without "/"), this includes parameters for the current procedure.
    return get_property_value($parameter_name);
}

sub get_current_procedure_params_hashref {
    my @parameters_names_list = @_;
    my %parameters_hash;
    for my $parameter_name (@parameters_names_list) {
        my $parameter_value = get_param_value($parameter_name);
        $parameters_hash{$parameter_name} = $parameter_value;
    }
    return \%parameters_hash;
}

sub get_credential {
    my $credential_name = shift;
    my $xpath = get_commander()->getFullCredential($credential_name);
    my %credential;
    $credential{user_name} = '' . $xpath->findvalue("//userName");
    $credential{password} = '' . $xpath->findvalue("//password");
    return \%credential;
}

1;