package EC::Models::RunCommandResult;

use strict;
use warnings;

use Carp;

sub new {
    my ($class, %params) = @_;

    my $code = $params{code};
    my $stdout = $params{stdout};
    my $stderr = $params{stderr};

    my $self = bless {
            code   => $code,
            stdout => $stdout,
            stderr => $stderr,
        }, $class;

    return $self;
}

sub get_code {
    my $this = shift;
    return $this->{code};
}

sub get_stdout {
    my $this = shift;
    return $this->{stdout};
}

sub get_stderr {
    my $this = shift;
    return $this->{stderr};
}

sub has_error {
    my $this = shift;
    if ($this->get_code() || $this->get_stderr()) {
        return 1;
    }
    else {
        return 0;
    }
}

1;