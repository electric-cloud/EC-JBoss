package EC::Utils::JBossUtils;

use strict;
use warnings;

use Carp;
use JSON;

sub convert_jboss_dmr_to_json_text {
    my $jboss_dmr = shift;

    my $json_text = $jboss_dmr;
    $json_text =~ s/\s=>\s/:/gs;
    $json_text =~ s/undefined/null/gs;
    $json_text =~ s/\n/ /gs;
    $json_text =~ s/:expression\s/: /gs;

    return $json_text;
}

# this function can fail on the decode_json step - to be handled by caller
sub convert_jboss_dmr_to_json_object {
    my $jboss_dmr = shift;

    my $json_text = convert_jboss_dmr_to_json_text($jboss_dmr);
    my $json_object = decode_json($json_text);

    return $json_object;
}

1;