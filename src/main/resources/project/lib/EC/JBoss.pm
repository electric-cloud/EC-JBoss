=head1 NAME

EC::JBoss

=head1 DESCRIPTION

EC JBoss integration plugin logic.

=head1 METHODS

=over

=cut

package EC::JBoss;
use strict;
use warnings;
use subs qw/is_win/;
use Carp;
use JSON;

use ElectricCommander;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;
use Data::Dumper;
use IPC::Open3;
use Symbol qw/gensym/;
use IO::Select;

our $VERSION = 0.02;

BEGIN {
    if ($^O eq 'MSWin32') {
        # just to prevent hangs on windows
        $ENV{NOPAUSE} = 'true';
    }
};

=item B<new> 

Constructor. Returns EC::JBoss object. 

    my $jboss = EC::JBoss->new(
        project_name => 'PROJECT_NAME',
    );

Available constructor params:

B<project_name> => name of project in EC

B<script_path> => path to the jboss-cli.sh or jboss-cli.bat, by default it uses scriptphysicalpath property.

B<serverconfig> => configuration name. By default it uses serverconfig property.

B<log_level> => specifies log level for current object. By default 1. You shouldn't set log_level less than 1.

B<dryrun>  => returns command output dummy instead of executing command by run_command. Ok for tests.

B<debug> => enables debug mode.

=cut

sub new {
    my ($class, %params) = @_;

    my $self = {};
    bless $self, $class;

    $self->{log_level} = $params{log_level};
    $self->{log_level} ||= 1;

    unless ($params{project_name}) {
        croak "Missing project_name";
    }

    if ($params{plugin_name}) {
        $self->{plugin_name} = $params{plugin_name};
    }
    if ($params{plugin_key}) {
        $self->{plugin_key} = $params{plugin_key};
    }

    $self->{project_name} = $params{project_name};
    $params{script_path} ||= $self->get_param('scriptphysicalpath');
    $params{config_name} ||= $self->get_param('serverconfig');

    my $dryrun = undef;
    if ($self->{plugin_key}) {
        eval {
            $dryrun = $self->ec()->getProperty(
                "/plugins/$self->{plugin_key}/project/dryrun"
            )->findvalue('//value')->string_value();
        };
    }

    if ($dryrun || $params{dryrun}) {
        $self->{dryrun} = 1;
        $self->out("Dryrun enabled.");
    }

    $self->ec();

    if ($params{debug}) {
        $self->{debug} = 1;
    }

    if ($params{script_path}) {
        if (!$self->{dryrun} && (!-e $params{script_path} || !-s $params{script_path})) {
            croak "File $params{script_path} doesn't exist or empty";
        }
        if (-d $params{script_path}) {
            croak "$params{script_path} is a directory";
        }

        $self->{script_path} = $params{script_path};
    }
    if ($params{config_name}) {
        $self->{config_name} = $params{config_name};
    }

    $self->{script_path} = qq|"$params{script_path}"|;
    $self->{config_name} = $params{config_name};

    $self->init();

    return $self;
}


=item B<run_command>

Runs specified command in jboss-cli context.

Usage with param /system-property=foo3:add(value=bar)

    $jboss->run_command('/system-property=foo3:add(value=bar)');

Will execute next command:

jboss-cli.sh  --user=user --password=password -c controller=localhost:10000 --command="/system-property=foo3:add(value=bar)"

This function, also, differentiates call context. In scalar context it returns only stdout, it list conext it returns
full command result.

For example:

    # list context call
    my %result = $jboss->run_command('help --commands');
    # %result now is:
    # %result = (
    #     code    =>  0,
    #     stdout  =>  'command stdout',
    #     stderr  =>  'command stdout',
    # );

    # scalar context call
    my $scalar_result = $jboss->run_command('help --commands');
    # $scalar_result = 'command strout';

=cut


sub run_command {
    my ($self, @command) = @_;

    my $credentials = $self->get_credentials();
    my $command = $self->{script_path} . ' ';

    my $controller_address = $self->get_controller_location();

    $command .= " -c controller=$controller_address ";

    if ($credentials->{user}) {
        if (is_win) {
            $command .= " --user=$credentials->{user} ";
        }
        else {
            $command .= " --user='$credentials->{user}' ";
        }
    }
    if ($credentials->{password}) {
        if (is_win) {
            $command .= " --password=$credentials->{password} ";
        }
        else {
            $command .= " --password='$credentials->{password}' ";
        }
    }

    $command .= '--command="' . $self->escape_string(join (' ', @command)) . '"';

    $self->out("Executing command: ", $self->safe_command($command));

    my $result;
    if ($self->{dryrun}) {
        $result = {
            code    =>  0,
            stderr  =>  '',
            stdout  =>  'DUMMY-STDOUT',
        };
    }
    else {
        if (is_win) {
            $result = $self->_syscall_win32($command);
        }
        else {
            $result = $self->_syscall($command);
        }
    }

    unshift @{$self->{history}}, {
        command     =>  $self->safe_command($command),
        result      =>  $result,
    };

    if (wantarray) {
        return %$result;
    }
    if ($result->{stderr}) {
        $self->out("Warnings: $result->{stderr}");
    }
    return $result->{stdout};
}


=item B<safe_command>

Returns command with masked password.

=cut

sub safe_command {
    my ($self, $command) = @_;

    $command =~ s/password=.*?\s/password=*** /gs;
    return $command;
}


=item B<init>

Initializes additional EC::JBoss object properties after conctruction.

=cut

sub init {
    my ($self) = @_;

    if ($self->{config_name}) {
        $self->get_credentials();
    }
    return 1;
}


=item B<get_credentials>

Returns credentials by credentials name specified at object creation.

=cut

sub get_credentials {
    my ($self) = @_;

    if ($self->{_credentials}) {
        return $self->{_credentials};
    }
    if (!$self->{config_name}) {
        croak "Configuration_name doesn't exist";
    }

    my $ec = $self->ec();
    my $config_name = $self->{config_name};
    my $project = $self->{project_name};

    my $pattern = sprintf '/projects/%s/jboss_cfgs', $project;
    my $plugin_configs;
    eval {
        $plugin_configs = ElectricCommander::PropDB->new($ec, $pattern);
        1;
    } or do {
        $self->out("Can't access credentials.");
        # bailing out if can't access credendials.
        $self->bail_out("Can't access credentials.");
    };

    my %config_row;
    eval {
        %config_row = $plugin_configs->getRow($config_name);
        1;
    } or do {
        $self->out("Configuration $config_name doesn't exist.");
        # bailing out if configuration specified doesn't exist.
        $self->bail_out("Configuration $config_name doesn't exist.");
    };

    unless (%config_row) {
        croak "Configuration doesn't exist";
    }

    my $retval = {};

    my $xpath = $ec->getFullCredential($config_row{credential});
    $retval->{user} = '' . $xpath->findvalue("//userName");
    $retval->{password} = '' . $xpath->findvalue("//password");
    $retval->{jboss_url} = '' . $config_row{jboss_url};

    return $retval;

}


=item B<ec>

Returns ElectricCommander object.

=cut

sub ec {
    my ($self) = @_;

    if (!$self->{_ec}) {
        $self->{_ec} = ElectricCommander->new();
    }
    return $self->{_ec};
}


=item B<get_param>

Returns request params from step configuration.

    # extracting path to jboss-cli.sh
    my $script_path = $jboss->get_param('scriptphysicalpath');

=cut

sub get_param {
    my ($self, $param) = @_;

    my $ec = $self->ec();
    my $retval;
    eval {
        $retval = $ec->getProperty($param)->findvalue('//value') . '';
        1;
    } or do {
        $self->logit(2, "Error '$@' was occured while getting property: $param");
        $retval = undef;
    };

    return $retval;
}


=item B<get_params_as_hashref>

Returns request params as hashref by list of param names.

    my $params = $jboss->get_params_as_hashref('param1', 'param2', 'param3');
    # $params = {
    #     param1  =>  'value1',
    #     param2  =>  'value2',
    #     param3  =>  'value3'
    # }

=cut

sub get_params_as_hashref {
    my ($self, @params_list) = @_;

    my $retval = {};
    my $ec = $self->ec();
    for my $param_name (@params_list) {
        my $param = $self->get_param($param_name);
        next unless defined $param;
        $retval->{$param_name} = $param;
    }
    return $retval;
}


=item B<get_controller_location>

Returns controller location by urls specified in config in the reason of
backward compatibility.

=cut

sub get_controller_location {
    my ($self) = @_;

    my $cred = $self->get_credentials();
    my $controller = $cred->{jboss_url};
    return undef unless $controller;
    $controller =~ s|^(?:.*?://)||s;
    $controller =~ s|/.+$||s;
    return $controller;
}


=item B<convert_response_to_json>

Converts JBoss response to json.

=cut

sub convert_response_to_json {
    my ($self, $response) = @_;

    $response =~ s/\s=>\s/:/gs;
    $response =~ s/undefined/null/gs;
    $response =~ s/"\n"/"\\n"/gs;

    return $response;
}


=item B<decode_answer>

Decodes jboss answer to Perl object.

    my $object = $jboss->decode_answer($stdout);
    print $object->{status};

=cut

sub decode_answer {
    my ($self, $response) = @_;

    $response = $self->convert_response_to_json($response);
    my $json;
    eval {
        $json = decode_json($response);
        1;
    } or do {
        print "Error occured: $@\n";
        $json = undef;
    };

    return $json;
}


=item B<_syscall_win32>

Older win systems and our older perl are unable to use pipe for external commands.
So, when we able to use _syscall, we'll use _syscall. If not, we will use _syscall_win32

    $jboss->_syscall_win32("ls -la");

=cut

sub _syscall_win32 {
    my ($self, @command) = @_;

    my $command = join '', @command;

    my $result_folder = $ENV{COMMANDER_WORKSPACE};
    $command .= qq| 1> "$result_folder/command.stdout" 2> "$result_folder/command.stderr"|;
    if (is_win) {
        $self->logit(1, "Windows detected");
        $ENV{NOPAUSE} = 1;
    }

    my $pid = system($command);
    my $retval = {
        stdout => '',
        stderr => '',
        code => $? >> 8,
    };

    open my $stderr, "$result_folder/command.stderr" or croak "Can't open stderr: $@";
    open my $stdout, "$result_folder/command.stdout" or croak "Can't open stdout: $@";
    $retval->{stdout} = join '', <$stdout>;
    $retval->{stderr} = join '', <$stderr>;
    close $stdout;
    close $stderr;
    return $retval;
}


=item B<_syscall>

Internal function. Performes system call. Accepts as parameter
command in list context and executes this param in a correct way.

Returns hash with 3 fields(code, stdout, stderr), where code is exit code,
stdout and stderr is a output of standard streams.

=cut

sub _syscall {
    my ($self, @command) = @_;

    my $command = join '', @command;
    unless ($command) {
        croak  "Missing command";
    }
    my ($infh, $outfh, $errfh, $pid, $exit_code);
    $errfh = gensym();
    eval {
        $pid = open3($infh, $outfh, $errfh, $command);
        waitpid($pid, 0);
        $exit_code = $? >> 8;
        1;
    } or do {
        croak "Error occured during command execution: $@";
    };

    my $retval = {
        code => $exit_code,
        stderr => '',
        stdout => '',
    };
    my $sel = IO::Select->new();
    $sel->add($outfh, $errfh);

    while(my @ready = $sel->can_read) { # read ready
        foreach my $fh (@ready) {
            my $line = <$fh>; # read one line from this fh
            if (not defined $line) {
                $sel->remove($fh);
                next;
            }
            if ($fh == $outfh) {
                $retval->{stdout} .= $line;
            }
            elsif ($fh == $errfh) {
                $retval->{stderr} .= $line;
            }

            if (eof $fh) {
                $sel->remove($fh);
            }
        }
    }
    return $retval;
}


=item B<setProperties>

Sets properties. Accepts hashref of properties.

    $jboss->setProperties({
        key => 'value',
    });

=cut

sub setProperties {
    my ($self, $propHash) = @_;
    foreach my $key (keys %{$propHash}) {
        my $val = $propHash->{$key};
        $self->set_property($key, $val);
    }
}


=item B<set_property>

Sets property.

$jboss->set_property(key => 'value');

=cut

sub set_property {
    my ($self, $key, $value) = @_;

    $self->logit(3, "Key: $key => $value");
    $self->ec()->setProperty("/myCall/$key", $value);
}


=item B<success>

Sets outcome step status to success.

    $jboss->success();

=cut

sub success {
    my ($self) = @_;

    return $self->_set_outcome('success');
}


=item B<error>

Sets outcome step status to error.

    $jboss->error();

=cut

sub error {
    my ($self) = @_;

    return $self->_set_outcome('error');
}


=item B<warning>

Sets outcome step status to warning.

    $jboss->waring();

=cut

sub warning {
    my ($self) = @_;

    return $self->_set_outcome('warning');
}


=item B<_set_outcome>

Sets outcome status to desired status.

    $jboss->_set_outcome('aborted');

=cut

sub _set_outcome {
    my ($self, $status) = @_;

    $self->ec()->setProperty('/myJobStep/outcome', $status);
}


=item B<process_response>

Postprocessor for JBoss responses.

    $jboss->process_response();

=cut

sub process_response {
    my ($self, %params) = @_;

    my $silent = 0;

    $silent = 1 if $self->{silent};
    if (!exists $params{code} || !exists $params{stdout} || !exists $params{stderr}) {
        croak "Wrong response hash";
        return ;
    }

    if (!$silent && $params{stdout}) {
        $self->out("Command output: $params{stdout}");
    }

    if (!$silent && $params{stderr}) {
        $self->out("Error stream: $params{stderr}");
    }

    # code is > 1, so it's an error
    my $stdout = {};
    if ($params{stdout}) {
        $self->decode_answer($params{stdout});
    }
    $stdout = $params{stdout} unless defined $stdout;

    $self->set_property('commands_history', encode_json($self->{history}));

    if ($params{code}) {
        $self->error();
        my $prop = '';

        if (ref $stdout eq 'HASH') {
            if (ref $stdout->{'failure-description'}) {
                $prop = $stdout->{'failure-description'}->{'domain-failure-description'};
            }
            else {
                $prop = $stdout->{'failure-description'};
            }
            # still have no props, maybe answer is broken.
            $prop ||= $params{stdout};
        }
        else {
            $prop = $stdout;
        }
        $self->set_property(summary => $prop);
        return 1;
    }

    if (!$params{code} && $params{stderr}) {
        $self->warning();
        return 1;
    }
    $self->success();
    return 1;
}


=item B<logit>

Prints message to step output. If current log_level lower than
specified runlevel it just returns.

Newline will be added automatically.

    $jboss->logit(10, "Deep debug message");

=cut

sub logit {
    my ($self, $level, @msg) = @_;

    return 0 if ($self->{log_level} < $level);

    my $msg = join '', @msg;
    $msg =~ s/\n$//gs;
    $msg .= "\n";
    print $msg;
}


=item B<escape_string>

Escapes specified string for jboss command-line interface
and returns escaped string.

    $self->escape_string(qq|This is\\unesca"ped ' string|);

=cut

sub escape_string {
    my ($self, $string) = @_; 

    croak "Missing string" unless $string;

    $string =~ s|\\|\\\\|;
    # $string =~ s/(\s)/\\$1/gs;
    $string =~ s|"|\\\\\\"|gs;
    return $string;
}


=item B<out>

Alias to logit(1, @msg)

    $jboss->out("Hello world!");

Is equivalent to

    $jboss->logit(1, "Hello world!");

=cut


sub out {
    my ($self, @msg) = @_;

    return $self->logit(1, @msg);
}


=item B<bail_out>

Terminating execution immediately with error message.

    $jboss->bail_out("Something was VERY wrong");

=cut

sub bail_out {
    my ($self, @msg) = @_;

    my $msg = join '', @msg;

    $msg ||= 'Bailed out.';
    $msg .= "\n";

    $self->error();
    $self->set_property(summary => $msg);
    exit 1;
}


=item B<is_win>

Returns true if current OS is windows

    return is_win;

=cut

sub is_win {
    if ($^O eq 'MSWin32') {
        return 1;
    }
    return 0;
}


1;

=back

=cut
