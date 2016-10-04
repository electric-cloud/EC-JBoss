=head1 NAME

preamble.pl

=head1 DESCRIPTION

Preamble for JBoss plugins. Imports necessary modules for JBoss integration.

=cut

use strict;
use warnings;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;
use Carp;

$| = 1;

my $ec = ElectricCommander->new();

my $load = sub {
    my $property_path = shift;

    ElectricCommander::PropMod::loadPerlCodeFromProperty(
        $ec, $property_path
    ) or do {
        croak "Can't load property $property_path";
    };
};

$load->('/myProject/jboss_driver/EC::JBoss');
