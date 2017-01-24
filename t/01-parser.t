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
use Test::More tests => 5;

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
    get_plugin_configuration => sub {
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

### Tests section ###
my $jboss = EC::JBoss->new(
    project_name    =>  'JBOSS_TEST',
    script_path     =>  '/path/to/jboss-cli.sh',
    config_name     =>  'jboss_test',
    dryrun          =>  1,
);

my $str;
my $result;

# 1 - Check parser reaction on minimalistic response from hostcontroller.
$str = q|{"outcome" => "success","result" => {"master" => {"host-state" => "running"}}}|;
$result = $jboss->decode_answer($str);
ok($result, "Decoded correct OK answer for CheckHostControllerStatus procedure.");

# 2 - Check paster reaction on JBoss mode. Domain, or standalone.
$str = q|{"outcome" => "success","result" => "DOMAIN"}|;
$result = $jboss->decode_answer($str);
ok($result, "Decoded JBoss response on launch mode request");

# 3 - Check parser reaction on JBoss gethost command response.
$str = q|{"outcome" => "success","result" => ["master"]}|;
$result = $jboss->decode_answer($str);
ok($result, "Decoded JBoss response on launch mode request");

# 4 - Check parser reaction on JBoss get server runtime response.
$str = q|{"outcome" => "success","result" => {"server-one" => {"group" => "main-server-group","name" => "server-one","status" => "STARTED"}}}|;
$result = $jboss->decode_answer($str);
ok($result, "Decoded JBoss response on launch mode request");

# 5 - Check parser reaction on JBoss get application runtime status.
$str = q|{"outcome" => "success","result" => "OK"}|;
$result = $jboss->decode_answer($str);
ok($result, "Decoded JBoss response on launch mode request");
