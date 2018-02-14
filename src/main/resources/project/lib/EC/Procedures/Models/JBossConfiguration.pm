package EC::Procedures::Models::JBossConfiguration;

use strict;
use warnings;

use Carp;

sub new {
    my ($class, %params) = @_;

    my $configuration_name = $params{configuration_name};
    my $controller_url = $params{controller_url};
    my $cli_script_location = $params{cli_script_location};
    my $user_name = $params{user_name};
    my $password = $params{password};
    my $log_level = $params{log_level};
    my $additional_java_options = $params{additional_java_options};

    if (!$configuration_name) {
        croak "'configuration_name' is required parameter";
    }
    if (!$controller_url) {
        croak "'controller_url' is required parameter";
    }

    my $self = bless {
            configuration_name      => $configuration_name,
            controller_url          => $controller_url,
            cli_script_location     => $cli_script_location,
            user_name               => $user_name,
            password                => $password,
            log_level               => $log_level,
            additional_java_options => $additional_java_options,
        }, $class;

    return $self;
}

sub get_configuration_name {
    my $this = shift;
    return $this->{configuration_name};
}

sub get_controller_url {
    my $this = shift;
    return $this->{controller_url};
}

sub get_cli_script_location {
    my $this = shift;
    return $this->{cli_script_location};
}

sub get_user_name {
    my $this = shift;
    return $this->{user_name};
}

sub get_password {
    my $this = shift;
    return $this->{password};
}

sub get_log_level {
    my $this = shift;
    return $this->{log_level};
}

sub get_additional_java_options {
    my $this = shift;
    return $this->{additional_java_options};
}

1;