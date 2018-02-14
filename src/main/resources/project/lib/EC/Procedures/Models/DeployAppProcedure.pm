package EC::Procedures::Models::DeployAppProcedure;

use strict;
use warnings;

use Carp;

sub new {
    my ($class, %params) = @_;

    my $configuration_name = $params{configuration_name};
    my $application_content_source_path = $params{application_content_source_path};
    my $deployment_name = $params{deployment_name};
    my $runtime_name = $params{runtime_name};
    my $enabled_server_groups = $params{enabled_server_groups};
#    my $disabled_server_groups = $params{disabled_server_groups};
    my $additional_options = $params{additional_options};

    if (!$configuration_name) {
        croak "'configuration_name' is required parameter";
    }
    if (!$application_content_source_path) {
        croak "'application_content_source_path' is required parameter";
    }

    my $self = bless {
            configuration_name              => $configuration_name,
            application_content_source_path => $application_content_source_path,
            deployment_name                 => $deployment_name,
            runtime_name                    => $runtime_name,
            enabled_server_groups           => $enabled_server_groups,
#            disabled_server_groups          => $disabled_server_groups,
            additional_options              => $additional_options,
        }, $class;

    return $self;
}

sub get_configuration_name {
    my $this = shift;
    return $this->{configuration_name};
}

sub get_application_content_source_path {
    my $this = shift;
    return $this->{application_content_source_path};
}

sub get_deployment_name {
    my $this = shift;
    return $this->{deployment_name};
}

sub get_runtime_name {
    my $this = shift;
    return $this->{runtime_name};
}

sub get_enabled_server_groups {
    my $this = shift;
    return $this->{enabled_server_groups};
}

sub get_disabled_server_groups {
    my $this = shift;
    return $this->{disabled_server_groups};
}

sub get_additional_options {
    my $this = shift;
    return $this->{additional_options};
}

1;