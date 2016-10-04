=head1 NAME

ECMock

=head1 DESCRIPTION

Module for easy mock objects manipulation

=head1 SYNOPSIS

=over

=cut

package ECMock;
use strict;
use warnings;
use Carp;

sub import {
    my ($caller, @module_list) = @_;

    my %hash = map{s/\s+//gs;($_=>1)}@module_list;
    return unless %hash;
    unshift @INC, sub {
        my ($self, $package) = @_;
        $package =~ s|/|::|gs;
        $package =~ s|\.pm||s;
        return unless $hash{$package};
        my $text = qq|package $package;1;|;
        open my $fh, '<', \$text;
        return $fh;
    };
}


=item B<mock_sub>

Alows you to mock subroutines of specified module.

    ECMock->mock_sub(
        'ElectricCommander',
        get_property    =>  sub {
            return 1;
        },
    );
    
    ElectricCommander->get_property(1);

=cut

sub mock_sub {
    my ($caller, $module, %subs) = @_;

    no warnings qw/redefine/;
    no strict qw/refs/;

    for my $name (keys %subs) {
        if (!$subs{$name} || ref $subs{$name} ne 'CODE') {
            croak "Wrong args";
        }

        *{$module . "::$name"} = $subs{$name};
    }

    return 1;
}

=back

=cut

1;
