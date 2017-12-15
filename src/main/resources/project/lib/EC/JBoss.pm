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
use subs qw/is_win is_positive_int/;
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

$| = 1;

{
    my $tec = ElectricCommander->new();
    my $log_property_path;
    eval {
        $log_property_path = $tec->getProperty('/plugins/EC-JBoss/project/ec_debug_logToProperty')->findvalue('//value')->string_value();
    };
    if ($log_property_path) {
        ElectricCommander::PropMod::loadPerlCodeFromProperty($ec,"/myProject/jboss_driver/EC::LogTrapper");

        EC::LogTrapper::open_handle();
        tie *STDOUT, "EC::LogTrapper", (
                print =>
                sub {
                    my $value = '';
                    eval {
                        $value = $tec->getProperty($log_property_path)->findvalue('//value')->string_value();
                    };
                    $value .= join '', @_;
                    $tec->setProperty($log_property_path, $value);
                    print @_;
                    # print map { uc } @_;
                }
            );
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

    my $self = {
        plugin_key => '',
        plugin_name => '',
        # do_we_have_error_on_interact_support => 0,
    };
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

    $params{config_name} ||= $self->get_param('serverconfig');

    if ($params{config_name}) {
        $self->{config_name} = $params{config_name};
    }

    my $creds = $self->get_plugin_configuration();

    if ($creds->{log_level} && is_positive_int($creds->{log_level})) {
        $self->{log_level} = $creds->{log_level};
        $creds->{log_level} = 4 if $creds->{log_level} > 4;
    }

    my $java_opts = $creds->{java_opts};
    if ($java_opts) {
        $self->out("Found JAVA_OPTS parameter: ", $java_opts);
        my $new_java_opts = $java_opts;
        if ($ENV{JAVA_OPTS}) {
            $new_java_opts = $ENV{JAVA_OPTS} . ' ' . $java_opts;
        }

        $ENV{JAVA_OPTS} = $new_java_opts;
        $self->out("New JAVA_OPTS value: $new_java_opts");
    }
    # script path parameter is empty.
    if (!$params{script_path}) {
        if ($creds->{scriptphysicalpath}) {
            $params{script_path} = $creds->{scriptphysicalpath};
        }
        if ($self->get_param('scriptphysicalpath')) {
            $params{script_path} = $self->get_param('scriptphysicalpath');
        }
    }
    unless ($params{script_path}) {
        $self->bail_out("No script physical path were found neither in configuration nor in procedure.");
    }
    # $params{script_path} ||= $self->get_param('scriptphysicalpath');

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
        $self->{log_level} = 4;
    }

    if ($params{script_path}) {
        if (!$self->{dryrun} && (!-e $params{script_path} || !-s $params{script_path})) {
            print "(test print) File $params{script_path} doesn't exist or empty";
            croak "(test croak) File $params{script_path} doesn't exist or empty";
        }
        if (-d $params{script_path}) {
            croak "$params{script_path} is a directory";
        }

        $self->{script_path} = $params{script_path};
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

    my $credentials = $self->get_plugin_configuration();
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

    # if ($self->{do_we_have_error_on_interact_support}) {
    #     $command .= ' --error-on-interact';
    # }
    $self->out("Executing command: ", $self->safe_command($command));

    my $result;
    if ($self->{dryrun}) {
        my $dryrun_response = $self->get_dryrun_response(@command);
        if ($dryrun_response) {
            $result = {
                code => $dryrun_response->{code},
                stderr => $dryrun_response->{stderr},
                stdout => $dryrun_response->{stdout},
            };
        }
        else {
            $result = {
                code    =>  0,
                stderr  =>  '',
                stdout  =>  'DUMMY-STDOUT',
            };
        }
    }
    else {
        if (is_win) {
            $result = $self->_syscall_win32($command);
        }
        else {
            $result = $self->_syscall($command);
        }
    }
    $self->log_debug("Code: '$result->{code}', STDOUT: '$result->{stdout}', STDERR: '$result->{stderr}'");
    # now we need to check jboss response. In some cases jboss-cli could return exit code 0 when command is finished with error.
    if (is_win && $result->{code} == 0 && $result->{stdout}) {
        my $decoded_answer = $self->decode_answer($result->{stdout});
        if ($decoded_answer) {
            $self->log_debug("Checking output for possible errors...\n");
            my @flat = ();
            eval {
                my %t = %$decoded_answer;
                @flat = %t;
            };
            for my $row (@flat) {
                if ($row =~ m/^(JBAS\d{6}):/s) {
                    $self->log_debug("Triggered error on $1");
                    $result->{code} = 1;
                }
            }
        }
        else {
            if ($result->{stdout} =~ m/(JBAS\d{6}):/s) {
                $result->{code} = 1;
            }
        }
    }
    # Another workaround for windows
    if (is_win) {
        if (is_win && $result->{stderr}) {
            $result->{code} = 1;
        }

        if ($result->{code} == 0 && $command =~ m/command="deploy/s && $result->{stdout} =~ m/already\sexists\sin\sthe\sdeployment\srepository/s) {
            $result->{code} = 1;
        }
        if ($result->{code} == 0 && $result->{stdout} =~ m/is\snot\sfound\samong\sthe\sregistered\sdeployments\./s) {
            $result->{code} = 1;
        }
    }
    # end of check
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
        $self->get_plugin_configuration();
    }

    # $self->do_we_have_error_on_interact_support();
    return 1;
}


=item B<get_plugin_configuration>

Returns credentials by credentials name specified at object creation.

=cut

sub get_plugin_configuration {
    my ($self) = @_;

    if ($self->{_credentials} && ref $self->{_credentials} eq 'HASH' && %{$self->{_credentials}}) {
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
    if ($config_row{scriptphysicalpath}) {
        $retval->{scriptphysicalpath} = '' . $config_row{scriptphysicalpath};
    }
    if ($config_row{java_opts}) {
        $retval->{java_opts} = '' . $config_row{java_opts};
    }
    if ($config_row{log_level}) {
        $retval->{log_level} = '' . $config_row{log_level};
    }

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
        $self->logit(3, "Error '$@' was occured while getting property: $param");
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

    my $cred = $self->get_plugin_configuration();
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
    $response =~ s/\n/ /gs;
    # $response =~ s/"\n"/"\\n"/gs;
    $response =~ s/:expression\s/: /gs;

    return $response;
}


=item B<decode_answer>

Decodes jboss answer to Perl object.

    my $object = $jboss->decode_answer($stdout);
    print $object->{status};

=cut

sub decode_answer {
    my ($self, $response) = @_;

    $self->log_debug("Going to decode JBoss response: $response");
    $response = $self->convert_response_to_json($response);
    $self->log_debug("Converted response: ", $response);
    my $json;
    eval {
        $json = decode_json($response);
        1;
    } or do {
        $self->log_error("Error occured during decoding converted JBoss answer: $@");
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
    if (!$result_folder) {
        $self->out(1, "Missing ENV for result folder. Result folder set to .");
        $result_folder = '.';
    }
    my $stderr_filename = 'command_' . gen_random_numbers(42) . '.stderr';
    my $stdout_filename = 'command_' . gen_random_numbers(42) . '.stdout';
    $command .= qq| 1> "$result_folder/$stdout_filename" 2> "$result_folder/$stderr_filename"|;
    if (is_win) {
        $self->out("MSWin32 detected");
        $ENV{NOPAUSE} = 1;
    }

    my $pid = system($command);
    my $retval = {
        stdout => '',
        stderr => '',
        code => $? >> 8,
    };

    open my $stderr, "$result_folder/$stderr_filename" or croak "Can't open stderr file ($stderr_filename) : $!";
    open my $stdout, "$result_folder/$stdout_filename" or croak "Can't open stdout file ($stdout_filename) : $!";
    $retval->{stdout} = join '', <$stdout>;
    $retval->{stderr} = join '', <$stderr>;
    close $stdout;
    close $stderr;

    # Cleaning up
    unlink("$result_folder/$stderr_filename");
    unlink("$result_folder/$stdout_filename");

    return $retval;
}

# sub _syscall_win32 {
#     my ($self, @command) = @_;

#     my $command = join '', @command;

#     my $result_folder = $ENV{COMMANDER_WORKSPACE};
#     $command .= qq| 1> "$result_folder/command.stdout" 2> "$result_folder/command.stderr"|;
#     if (is_win) {
#         $self->logit(1, "Windows detected");
#         $ENV{NOPAUSE} = 1;
#     }

#     my $pid = system($command);
#     my $retval = {
#         stdout => '',
#         stderr => '',
#         code => $? >> 8,
#     };

#     open my $stderr, "$result_folder/command.stderr" or croak "Can't open stderr: $@";
#     open my $stdout, "$result_folder/command.stdout" or croak "Can't open stdout: $@";
#     $retval->{stdout} = join '', <$stdout>;
#     $retval->{stderr} = join '', <$stderr>;
#     close $stdout;
#     close $stderr;
#     return $retval;
# }


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

    $self->log_debug("Setting property '$key' = '$value'");
    $self->ec()->setProperty("/myCall/$key", $value);
}


=item B<success>

Sets outcome step status to success.

    $jboss->success();

=cut

sub success {
    my ($self, @message) = @_;

    if (@message) {
        my $msg = join '', @message;
        $self->set_property(summary => $msg);
    }
    return $self->_set_outcome('success');
}


=item B<error>

Sets outcome step status to error.

    $jboss->error();

=cut

sub error {
    my ($self, @message) = @_;

    if (@message) {
        my $msg = join '', @message;
        $self->set_property(summary => $msg);
    }
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
    my $success_msg = '';
    if ($self->{success_message}) {
        $success_msg = $self->{success_message};
        $self->out($success_msg);
    }
    $self->success($success_msg);
    return 1;
}


=item B<logit>

Prints message to step output. If current log_level lower than
specified runlevel it just returns.

Newline will be added automatically.

    $jboss->logit(4, "Deep debug message");

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
    # $string =~ s|"|\\\\\\"|gs;
    $string =~ s|"|\\"|gs;
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

    return 1 if ($self->{silent});
    return $self->logit(1, @msg);
}


=item B<log_info>

Alias to logit(1, @msg)

    $jboss->log_info("Hello world!");

Is equivalent to

    $jboss->logit(1, 'INFO: ', "Hello world!");

=cut

sub log_info {
    my ($self, @msg) = @_;

    unshift @msg, 'INFO: ';
    return $self->out(@msg);
}


=item B<log_warning>

Alias to logit(1, @msg)

    $jboss->log_warning("Hello world!");

Is equivalent to

    $jboss->logit(2, 'WARNING:', "Hello world!");

=cut

sub log_warning {
    my ($self, @msg) = @_;

    return 1 if ($self->{silent});
    unshift @msg, 'WARNING: ';
    return $self->logit(2, @msg);
}


=item B<log_error>

Alias to logit(3, @msg)

    $jboss->log_error("Hello world!");

Is equivalent to

    $jboss->logit(3, 'ERROR: ', "Hello world!");

=cut

sub log_error {
    my ($self, @msg) = @_;

    return 1 if ($self->{silent});
    unshift @msg, 'ERROR: ';
    return $self->logit(3, @msg);
}


=item B<log_debug>

Alias to logit(4, @msg)

    $jboss->log_debug("Hello world!");

Is equivalent to

    $jboss->logit(4, 'DEBUG', "Hello world!");

=cut

sub log_debug {
    my ($self, @msg) = @_;

    unshift @msg, 'DEBUG: ';
    return $self->logit(3, @msg);
}


=item B<out_warning>

Triggers postprocessor to set this message to summary with warning outcome.

    $jboss->out_warning("Deprecated");

=cut

sub out_warning {
    my ($self, @msg) = @_;

    my $warning = "WARNING:";
    unshift @msg, $warning;
    return $self->out(@msg);
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

=item B<get_servergroup_status>

Returns servergroup status in hashref format and statuses list in arrayref format.

=cut

sub get_servergroup_status {
    my ($self, $server_group_name) = @_;

    my $servers = {};
    my @states = ();
    if (!$server_group_name) {
        $self->bail_out("Servergroup parameter is mandatory");
    }
    my %hosts_response = $self->run_command(':read-children-names(child-type=host)');
    my $hosts_json = $self->decode_answer($hosts_response{stdout});

    for my $host_name (@{$hosts_json->{result}}) {
        my $command = sprintf '/host=%s:read-children-resources(child-type=server-config,include-runtime=true)', $host_name;
        my %response = $self->run_command($command);
        my $children_json = $self->decode_answer($response{stdout});

        for my $server_name ( keys %{$children_json->{result} } ) {
            my $group = $children_json->{result}->{$server_name}->{group};
            next unless $group eq $server_group_name;

            my $status = $children_json->{result}->{$server_name}->{status};
            $servers->{$host_name}->{$server_name} = {status => $status};
            push @states, $status;
            $self->out("Found server $server_name in state $status");
        }
    }

    return ($servers, \@states);
}

=item B<trim>

Trim function.

=cut

sub trim {
    my ($jboss, $string) = @_;

    $string =~ s/^\s+//gs;
    $string =~ s/\s+$//gs;
    return $string;
}


=item B<run_jboss_command_and_bail_out_on_error>

Executes jboss command and bails out if error occured.

=cut

# Bailing out if the command failed
sub run_jboss_command_and_bail_out_on_error {
    my ($jboss, $command) = @_;

    my %response = $jboss->run_command($command);
    if ( $response{code} ) {
        $jboss->process_response(%response);
        $jboss->bail_out('An error occured while running jboss command');
    }
    my $json = $jboss->decode_answer($response{stdout});
    return $json;
}


=item B<gen_random_numbers>

Generates random numbers by seed provided.

=cut

sub gen_random_numbers {
    my ($mod) = @_;

    my $rand = rand($mod);
    $rand =~ s/\.//s;
    return $rand;
}


sub run_commands_until_done {
    my ($self, $params, $action) = @_;

    my $time_limit = $params->{time_limit};
    my $sleep_time = $params->{sleep_time};

    if ($time_limit eq '') {
        $time_limit = undef;
    }
    elsif (!is_positive_int($time_limit)) {
        $self->out("time_limit should be a positive integer.");
        exit 1;
    }

    if (!$sleep_time) {
        $self->out("sleep_time parameter is mandatory.");
    }

    unless(is_positive_int($sleep_time)) {
        $self->out("sleep_time should be a positive integer.");
    }
    if (!$action || ref $action ne 'CODE') {
        $self->out("Action param is mandatory");
        exit 1;
    }

    my $time_start = time();

    my $done = 0;
    my $retval = 0;

    while (!$done) {
        my $diff = time() - $time_start;
        # time limit undefined, so, only 1 iteration.
        if (!defined $time_limit) {
            $done = 1;
        }
        elsif ($time_limit && $diff >= $time_limit) {
            $done = 1;
            last;
        }
        # else time limit is 0, so it is unlimited.
        $retval = $action->($self);
        if ($retval) {
            last;
        }
        sleep $sleep_time unless $done;
    }
    return $retval;
}


sub is_positive_int {
    my ($check) = @_;

    if ($check !~ m/^\d+$/s) {
        return 0;
    }
    return 1;
}

=item B<get_launch_type>

Returns JBoss launch type.

    return $jboss->get_launch_type();

=cut

sub get_launch_type {
    my ($self) = @_;

    my $command = ':read-attribute(name=launch-type)';
    my %r = $self->run_command($command);
    my $temp = $self->decode_answer($r{stdout});
    unless ($temp) {
        print "Return nothing\n";
        return '';
    }
    my $jboss_type = $temp->{result};
    $jboss_type = lc $jboss_type;

    if ($jboss_type ne 'domain' && $jboss_type ne 'standalone') {
        $self->bail_out("Unknown JBoss Launch Type: $jboss_type");
    }

    return $jboss_type;
}


=item B<get_server_groups>

Returns server groups from domain setup. Should be used in domain mode only.

=cut

sub get_server_groups {
    my ($self) = @_;

    my $command = '/:read-children-names(child-type=server-group)';

    my %r = $self->run_command($command);
    my $decoded_response = $self->decode_answer($r{stdout});

    if ($decoded_response->{result} && ref $decoded_response->{result} eq 'ARRAY') {
        return $decoded_response->{result};
    }
    $self->bail_out("Should be an array reference");
}

sub get_hosts {
    my ($self) = @_;
    my %hosts_response = $self->run_command(':read-children-names(child-type=host)');
    my $hosts_json = $self->decode_answer($hosts_response{stdout});

    return $hosts_json->{result};
}


sub get_servers {
    my ($self, %params) = @_;

    my $hosts = [];
    my $servers = [];
    my $groups = [];

    if ($params{hosts}) {
        $hosts = $params{hosts};
    }
    if ($params{servers}) {
        $servers = $params{servers};
    }
    if ($params{groups}) {
        $groups = $params{groups};
    }

    if ($hosts && ref $hosts ne 'ARRAY') {
        $self->bail_out("Hosts should be an array reference.");
    }

    my $got_hosts = $self->get_hosts();

    if (@$hosts) {
        @$got_hosts = grep {$self->in_array($_ => $hosts)} @$got_hosts;
    }

    if (!@$got_hosts) {
        $self->bail_out("No hosts were found by your request.");
    }
    my $retval = {};
    for my $host (@$got_hosts) {
        # my $command = sprintf "/host=%s:read-children-resources(child-type=server)", $host;
        my $command = sprintf "/host=%s:read-children-resources(child-type=server-config,include-runtime=true)", $host;
        my %result = $self->run_command($command);
        my $json = $self->decode_answer($result{stdout});
        $self->log_debug("Found servers on host:");
        if (@$groups) {
            my @keys = keys %{$json->{result}};
            for my $k (@keys) {
                my $group = $json->{result}->{$k}->{group};
                if (!$group || !$self->in_array($group => $groups)) {
                    delete $json->{result}->{$k};
                }
            }
        }
        if (@$servers) {
            my @keys = keys %{$json->{result}};
            for my $k (@keys) {
                if (!$self->in_array($k => $servers)) {
                   delete $json->{result}->{$k};
                }
            }
        }
        my @servers_list = keys %{$json->{result}};
        $retval->{$host} = \@servers_list;
    }
    return $retval;
}


sub in_array {
    my ($self, $what, $where)  = @_;

    if (!$what || !$where) {
        $self->bail_out("Missing arguments for in_array");
    }

    if (!ref $where || ref $where ne 'ARRAY') {
        $self->bail_out("2nd argument should be an ARRAY reference");
    }
    for my $elem (@$where) {
        return 1 if $elem eq $what;
    }
    return 0;
}


sub is_servergroup_has_status {
    my ($self, $servergroup, $status_list) = @_;

    # will return an array reference for easy iteration.
    my $retval = [];

    my ($servers, $states) = $self->get_servergroup_status($servergroup);

    my $found = 0;
    for my $status (@$status_list) {
        if ($self->in_array($status => $states)) {
            $found = 1;
        }
    }

    if ($found) {
        for my $host (keys %$servers) {
            for my $server (keys %{$servers->{$host}}) {
                if ($self->in_array($servers->{$host}->{$server}->{status}, $status_list)) {
                    push @$retval, {
                        status => $servers->{$host}->{$server}->{status},
                        host => $host,
                        server => $server
                    };
                }
            }
        }
    }
    return $retval;
}

sub fix_url {
    my ($self, $url) = @_;

    if ($url !~ m/^http/s) {
        $url = 'http://' . $url;
    }
    return $url;
}

sub get_dryrun_response {
    my ($self, @command) = @_;

    my $command = join ' ', @command;
    my $property = '';
    eval {
        $property = $self->ec()->getProperty(
            "/plugins/$self->{plugin_key}/project/dryrun_response"
        )->findvalue('//value')->string_value();
    };

    return undef unless $property;
    my $json = decode_json($property);
    my $retval = undef;
    for my $row (@{$json->{commands}}) {
        my $regexp = quotemeta($row->{command});
        if ($command =~ m/$regexp/s) {
            $retval = $row;
            last;
        }
    }
    # add search mechanism there.
    return $retval;
}

sub is_6_0_0 {
    my ($self) = @_;

    my $version = $self->get_jboss_server_version();

    if (!$version->{product_version}) {
        $self->bail_out("Wrong JBoss version");
    }

    if ($version->{product_version} =~ m/^6\.0\.0/s) {
        return 1;
    }
    return 0;
}


sub get_jboss_server_version {
    my ($self) = @_;

    my $version = {
        product_name => '',
        product_version => ''
    };

    if ($self->{dryrun}) {
        $version->{product_name} = 'JBoss EAP';
        $version->{product_version} = '7.0.0.GA';

        return $version;
    }

    my %r;
    my $t;
    %r = $self->run_command(":read-attribute(name=product-name)");
    $t = $self->decode_answer($r{stdout});
    if ($t->{outcome} eq 'success') {
        $version->{product_name} = $t->{result};
    }
    %r = $self->run_command(":read-attribute(name=product-version)");
    $t = $self->decode_answer($r{stdout});
    if ($t->{outcome} eq 'success') {
        $version->{product_version} = $t->{result};
    }

    if ($r{code} > 0) {
        $self->bail_out("Error occured during version check.");
    }
    $self->log_debug("Determined product version: ", $version->{product_name}, ': ', $version->{product_version});
    return $version;

}


sub do_we_have_error_on_interact_support {
    my ($self) = @_;

    my $version = $self->get_jboss_server_version();
    my $major;
    if ($version->{product_version} =~ m/^(\d+)\./s) {
        $major = $1;
        $self->log_debug("Major version is: $major");
    }
    if ($version->{product_name} eq 'JBoss EAP' && $major >= 7) {
        $self->log_debug("We have --error-on-interact support");
        $self->{do_we_have_error_on_interact_support} = 1;
        return 1;
    }
    $self->log_debug("We don't have --error-on-interact support");
    return 0;
}

1;

=back

=cut
