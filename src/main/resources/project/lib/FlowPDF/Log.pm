package FlowPDF::Log;
use base qw/Exporter/;

our @EXPORT = (
    'logInfo',
    'logDebug',
    'logTrace',
    'logError',
    'logWarning',
    'logInfoDiag',
    'logWarningDiag',
    'logErrorDiag'
);
use strict;
use warnings;
use Data::Dumper;
use Carp;
use Try::Tiny;

our $LOG_LEVEL = 0;
our $LOG_TO_PROPERTY = '';
our $MASK_PATTERNS = [];

use constant {
    ERROR => -1,
    INFO  => 0,
    DEBUG => 1,
    TRACE => 2,
};

sub setMaskPatterns {
    my (@params) = @_;

    unless (@params) {
        croak "Missing mask patterns for setMaskPatterns";
    }
    if ($params[0] eq __PACKAGE__ || ref $params[0] eq __PACKAGE__) {
        shift @params;
    }
    for my $p (@params) {
        next if isCommonPassword($p);
        $p = quotemeta($p);
        # avoiding duplicates
        if (inArray($p, @$MASK_PATTERNS)) {
            next;
        }

        push @$MASK_PATTERNS, $p;
    }
    return 1;
}

sub isCommonPassword {
    my ($password) = @_;

    # well, huh.
    if ($password eq 'password') {
        return 1;
    }
    if ($password =~ m/^(?:TEST)+$/is) {
        return 1;
    }
    return 0;
}

sub maskLine {
    my ($self, $line) = @_;

    if (!ref $self || $self eq __PACKAGE__) {
        $line = $self;
    }

    for my $p (@$MASK_PATTERNS) {
        $line =~ s/$p/[PROTECTED]/gs;
    }
    return $line;
}

sub setLogToProperty {
    my ($param1, $param2) = @_;

    # 1st case, when param 1 is a reference, we are going to set log to property for current object.
    # but if this reference is not a FlowPDF::Log reference, it will bailOut
    if (ref $param1 and ref $param1 ne __PACKAGE__) {
        croak "Unexpected usage of setLogToProperty: unexpected context";
    }

    if (ref $param1) {
        if (!defined $param2) {
            croak "Unexpected usage of setLogToProperty: Property Path is not provided";
        }
        $param1->{logToProperty} = $param2;
        return $param1;
    }
    else {
        if ($param1 eq __PACKAGE__) {
            $param1 = $param2;
        }
        if (!defined $param1) {
            croak "Property path is mandatory parameter";
        }
        $LOG_TO_PROPERTY = $param1;
        return 1;
    }
}

sub getLogProperty {
    my ($self) = @_;

    if (ref $self && ref $self eq __PACKAGE__) {
        return $self->{logToProperty};
    }
    return $LOG_TO_PROPERTY;
}

sub getLogLevel {
    my ($self) = @_;

    if (ref $self && ref $self eq __PACKAGE__) {
        return $self->{level};
    }

    return $LOG_LEVEL;
}


sub setLogLevel {
    my ($param1, $param2) = @_;

    if (ref $param1 and ref $param1 ne __PACKAGE__) {
        croak ("Unexpected usage of setLogLevel: unexpected context");
    }

    if (ref $param1) {
        if (!defined $param2) {
            croak "Log level is mandatory parameter";
        }
        $param1->{level} = $param2;
        return $param1;
    }
    else {
        if ($param1 eq __PACKAGE__) {
            $param1 = $param2;
        }
        if (!defined $param1) {
            croak "Property path is mandatory parameter";
        }
        $LOG_LEVEL = $param1;
        return 1;
    }
}
sub new {
    my ($class, $opts) = @_;

    my ($level, $logToProperty);

    if (!defined $opts->{level}) {
        $level = $LOG_LEVEL;
    }
    else {
        $level = $opts->{level};
    }

    if (!defined $opts->{logToProperty}) {
        $logToProperty = $LOG_TO_PROPERTY;
    }
    else {
        $logToProperty = $opts->{logToProperty};
    }
    my $self = {
        level           => $level,
        logToProperty   => $logToProperty
    };
    bless $self, $class;
    return $self;
}

# TODO: Refactor this a bit
# now it is forbidden to call it with full function name.
sub logInfoDiag {
    return printDiagMessage('INFO', @_);
}
sub logErrorDiag {
    return printDiagMessage('ERROR', @_);
}
sub logWarningDiag {
    return printDiagMessage('WARNING', @_);
}


sub printDiagMessage {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }

    my $logObject = shift @params;
    my $level = shift @params;

    if (!$logObject || !$level || !@params) {
        return 0;
    }

    $level = uc $level;
    if ($level !~ m/^(?:ERROR|WARNING|INFO)$/s) {
        return 0;
    }

    # \n[OUT][%s]: %s :[%s][OUT]\n
    my $begin = "\n[POSTP][$level]: ";
    my $end   = " :[$level][POSTP]\n";

    my $msg = join '', @params;
    $msg = $begin . $msg . $end;
    return info($logObject, $msg);
}
sub logInfo {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }
    return info(@params);
}
sub info {
    my ($self, @messages) = @_;
    $self->_log(INFO, @messages);
}


sub logDebug {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }
    return debug(@params);
}
sub debug {
    my ($self, @messages) = @_;
    $self->_log(DEBUG, '[DEBUG]', @messages);
}


sub logError {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }
    return error(@params);
}
sub error {
    my ($self, @messages) = @_;
    $self->_log(ERROR, '[ERROR]', @messages);
}


sub logWarning {
    my @params = @_;

    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }
    return warning(@params);
}
sub warning {
    my ($self, @messages) = @_;
    $self->_log(INFO, '[WARNING]', @messages);
}


sub logTrace {
    my @params = @_;
    if (!ref $params[0] || ref $params[0] ne __PACKAGE__) {
        unshift @params, __PACKAGE__->new();
    }
    return trace(@params);
}
sub trace {
    my ($self, @messages) = @_;
    $self->_log(TRACE, '[TRACE]', @messages);
}

sub level {
    my ($self, $level) = @_;

    if (defined $level) {
        $self->{level} = $level;
    }
    else {
        return $self->{level};
    }
}

sub logToProperty {
    my ($self, $prop) = @_;

    if (defined $prop) {
        $self->{logToProperty} = $prop;
    }
    else {
        return $self->{logToProperty};
    }
}


my $length = 40;

sub divider {
    my ($self, $thick) = @_;

    if ($thick) {
        $self->info('=' x $length);
    }
    else {
        $self->info('-' x $length);
    }
}

sub header {
    my ($self, $header, $thick) = @_;

    my $symb = $thick ? '=' : '-';
    $self->info($header);
    $self->info($symb x $length);
}

sub _log {
    my ($self, $level, @messages) = @_;

    return if $level > $self->level;
    my @lines = ();
    for my $message (@messages) {
        if (ref $message) {
            my $t = Dumper($message);
            $t = $self->maskLine($t);
            print $t;
            push @lines, $t;
        }
        else {
            $message = $self->maskLine($message);
            print "$message\n";
            push @lines, $message;
        }
    }

    if ($self->{logToProperty}) {
        my $prop = $self->{logToProperty};
        my $value = "";
        try {
            $value = $self->ec->getProperty($prop)->findvalue('//value')->string_value;
            1;
        };
        unshift @lines, split("\n", $value);
        $self->ec->setProperty($prop, join("\n", @lines));
    }
}


sub ec {
    my ($self) = @_;
    unless($self->{ec}) {
        require ElectricCommander;
        my $ec = ElectricCommander->new;
        $self->{ec} = $ec;
    }
    return $self->{ec};
}

sub inArray {
    my ($elem, @array) = @_;

    if (!defined $elem) {
        croak "Missing 1st parameter for inArray";
    }
    for my $e (@array) {
        return 1 if $elem eq $e;
    }

    return 0;
}

1;
