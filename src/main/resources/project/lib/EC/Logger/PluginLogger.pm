package EC::Utils::CommanderUtils;

use strict;
use warnings;

use base qw(Exporter);

use EC::Utils::CommanderUtils qw(get_commander);

our @EXPORT_OK = qw(
    get_logger
    );

{
    # similar to 'state' in newer perl
    my $commander = get_commander();
    sub get_logger {
        return $commander;
    }
}

1;