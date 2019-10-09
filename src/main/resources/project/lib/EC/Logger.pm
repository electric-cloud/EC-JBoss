package EC::Logger;

use strict;
use warnings;

use Carp;

my $LOG_LEVEL_OLD_API_VALUE_DEBUG = 4;
my $LOG_LEVEL_OLD_API_VALUE_INFO = 1;
my $LOG_LEVEL_OLD_API_VALUE_WARNING = 2;
my $LOG_LEVEL_OLD_API_VALUE_ERROR = 3;

my $LOG_LEVEL_PRIORITY_INT_DEBUG = 10000;
my $LOG_LEVEL_PRIORITY_INT_INFO = 20000;
my $LOG_LEVEL_PRIORITY_INT_WARNING = 30000;
my $LOG_LEVEL_PRIORITY_INT_ERROR = 40000;

my %LOG_LEVEL_PRIORITY_RESOLVER_FOR_OLD_API = (
    $LOG_LEVEL_OLD_API_VALUE_DEBUG   => $LOG_LEVEL_PRIORITY_INT_DEBUG,
    $LOG_LEVEL_OLD_API_VALUE_INFO    => $LOG_LEVEL_PRIORITY_INT_INFO,
    $LOG_LEVEL_OLD_API_VALUE_WARNING => $LOG_LEVEL_PRIORITY_INT_WARNING,
    $LOG_LEVEL_OLD_API_VALUE_ERROR   => $LOG_LEVEL_PRIORITY_INT_ERROR,
);

sub new {
    my ($class, %params) = @_;
    my $log_level_old_api_value = $params{log_level_old_api_value} || $LOG_LEVEL_OLD_API_VALUE_INFO;

    my $log_level_priority_int = $LOG_LEVEL_PRIORITY_INT_INFO;
    if (exists $LOG_LEVEL_PRIORITY_RESOLVER_FOR_OLD_API{$log_level_old_api_value}) {
        $log_level_priority_int = $LOG_LEVEL_PRIORITY_RESOLVER_FOR_OLD_API{$log_level_old_api_value};
    }

    my $self = { log_level_priority_int => $log_level_priority_int };
    return bless $self, $class;
}

sub info {
    my ($self, @messages) = @_;
    $self->_log($LOG_LEVEL_PRIORITY_INT_INFO, 'INFO: ', @messages);
}

sub debug {
    my ($self, @messages) = @_;
    $self->_log($LOG_LEVEL_PRIORITY_INT_DEBUG, 'DEBUG: ', @messages);
}

sub warning {
    my ($self, @messages) = @_;

    $self->_log($LOG_LEVEL_PRIORITY_INT_WARNING, 'WARNING: ', @messages);
}

sub error {
    my ($self, @messages) = @_;
    $self->_log($LOG_LEVEL_PRIORITY_INT_ERROR, 'ERROR: ', @messages);
}

sub _log {
    my ($self, $log_level_priority_int, @messages) = @_;

    return 0 unless $self->_is_logger_enabled_for_level(
        log_level_priority_int => $log_level_priority_int
    );

    my $messages = join '', @messages;
    $messages .= "\n";
    print $messages;
}

sub _is_logger_enabled_for_level {
    my $self = shift;
    my %args = @_;
    my $checked_log_level_priority_int = $args{log_level_priority_int} || croak "'log_level_priority_int' is required param";
    my $current_log_level_priority_int = $self->{log_level_priority_int};

    return 1 if $checked_log_level_priority_int >= $current_log_level_priority_int;
    return 0;
}

sub diagnostic_info {
    my ($self, @params) = @_;

    return $self->_diagnostic_log('INFO', @params);
}

sub diagnostic_error {
    my ($self, @params) = @_;

    return $self->_diagnostic_log('ERROR', @params);
}

sub diagnostic_warning {
    my ($self, @params) = @_;

    return $self->_diagnostic_log('WARNING', @params);
}

sub _diagnostic_log {
    my ($self, @params) = @_;

    my $level = shift @params;

    if (!$level || !@params) {
        return 0;
    }

    $level = uc $level;
    if ($level !~ m/^(?:ERROR|WARNING|INFO)$/s) {
        return 0;
    }

    my $begin = "\n[OUT][$level]: ";
    my $end = " :[$level][OUT]\n";

    my $msg = join '', @params;
    $msg = $begin . $msg . $end;

    return $self->info($msg);
}

1;