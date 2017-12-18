package EC::LogTrapper;
use strict;
no warnings;

our $real_stdout;
$| = 1;

sub open_handle {
    # raw filehandle assignment.
    *STDERR = *STDOUT;
    open(REALSTDOUT, ">&STDOUT");
    $real_stdout = *REALSTDOUT;
}

sub TIEHANDLE {
    my($class, %params) = @_;
    my $self = {
        print => $params{print} || sub { print @_; },
        syswrite => $params{syswrite} || sub {
            my($buf, $len, $offset) = @_;
            syswrite(STDOUT, $buf, $len, defined($offset) ? $offset : 0);
        }
    };
    $self->{printf} = $params{printf} || sub {
        $self->{print}->(sprintf($_[0], @_[1 .. $#_]))
    };
    bless($self, $class);
}

sub _with_real_STDOUT {
    # open(local *STDOUT, ">&", REALSTDOUT) or die "$!";
    open(local *STDOUT, ">&", $real_stdout) or die "$!";
    $_[0]->(@_[1 .. $#_]);
}
sub BINMODE {return 1;};
sub PRINT  { _with_real_STDOUT(shift()->{print},  @_); }
sub PRINTF { _with_real_STDOUT(shift()->{printf}, @_); }
sub WRITE  { _with_real_STDOUT(shift()->{syswrite}, @_); }

1;
