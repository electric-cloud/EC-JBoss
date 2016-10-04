use strict;
use warnings;
no warnings qw/once/;
use ECMock qw/
    ElectricCommander
    ElectricCommander::PropMod
    ElectricCommander::PropDB
/;

use JSON;
use Data::Dumper;
use Test::More tests => 10;

use lib 'src/main/resources/project/lib';

use EC::JBoss;
use Cwd;

require ECMock;


# mock section here
ECMock->mock_sub('ElectricCommander',
    new         => sub {
        %{*ElectricCommander::prop_base} = (
            scriptphysicalpath  =>  '/path/to/jboss-cli.sh',
            config_name         =>  'jboss_test',
        );
        return bless {}, 'ElectricCommander'
    },
    getProperty => sub {
        my ($caller, $key) = @_;
        return ${ElectricCommander::prop_base}{$key};
    },
    setProperty => sub {
        my ($caller, $key, $value) = @_;
        
        ${ElectricCommander::prop_base}{$key} = $value;
    },
);

ECMock->mock_sub('EC::JBoss',
    get_credentials => sub {
        my ($self) = @_;

        if (!$self->{_credentials}) {
            $self->{_credentials} = {
                user        =>  'admin',
                password    =>  'changeme',
                jboss_url   =>  'localhost:9999',
            };
        }

        return $self->{_credentials};
    },
);
### End of the mock section

### Tests section

my $jboss = EC::JBoss->new(
    project_name    =>  'JBOSS_TEST',
    script_path     =>  '/path/to/jboss-cli.sh',
    config_name     =>  'jboss_test',
    dryrun          =>  1,
);

$jboss->{log_level} = 0;

ok(ref $jboss eq 'EC::JBoss', 'EC::JBoss initialized');
ok(ref $jboss->ec() eq 'ElectricCommander', "ElectricCommander object exists");
ok($jboss->{_credentials} && ref $jboss->{_credentials} eq 'HASH', 'Credentials ok');
ok($jboss->{dryrun}, 'Dryrun enabled');
ok($jboss->{script_path}, 'JBoss client is ok');

my %result = $jboss->run_command('help --commands');
ok($result{stdout} && defined $result{stderr} && defined $result{code}, "Command result is ok");

$jboss->process_response(%result);

my $outcome = $jboss->ec()->getProperty('/myJobStep/outcome');
is ($outcome, 'success', 'Postprocessor is ok');

my $commands_history = $jboss->ec()->getProperty('/myCall/commands_history');
ok($commands_history, "Commands History was set");

my $object = undef;
eval {
    $object = decode_json($commands_history);
    1;
};
ok($object, 'Commands History was successfully decoded');
ok($object->[0]->{command} =~ m/jboss-cli.sh.*?--command="help\s--commands"/s, 'Command was proper');
