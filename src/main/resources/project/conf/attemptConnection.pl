use strict;
use warnings;

use ElectricCommander;
use ElectricCommander::PropDB;
use ElectricCommander::PropMod;

use Data::Dumper;

#*****************************************************************************
use constant {
    SUCCESS => 0,
    ERROR   => 1,
};

use constant {
    LEVEL_ERROR => -1,
    LEVEL_INFO  => 0,
    LEVEL_DEBUG => 1,
    LEVEL_TRACE => 2,
};

#*****************************************************************************
my $ec = ElectricCommander->new();
# $ec->abortOnError(0);

my $projName   = '$[/myProject/projectName]';
my $pluginName = '@PLUGIN_NAME@';
my $pluginKey  = '@PLUGIN_KEY@';

my $weblogic_url          = '$[weblogic_url]';
my $wlst_path             = '$[wlst_path]';
my $java_home             = '$[java_home]';
my $java_vendor           = '$[java_vendor]';
my $mw_home               = '$[mw_home]';
my $credential            = '$[credential]';
my $enable_named_sessions = '$[enable_named_sessions]';
my $debug_level           = '$[debug_level]';

ElectricCommander::PropMod::loadPerlCodeFromProperty($ec, '/myProject/EC::Plugin::Core');
ElectricCommander::PropMod::loadPerlCodeFromProperty($ec, '/myProject/EC::WebLogic');

my $wl = EC::WebLogic->new(
    project_name => $projName,
    plugin_name  => $pluginName,
    plugin_key   => $pluginKey
);

my $cred_xpath = $ec->getFullCredential($credential);
my $username   = $cred_xpath->findvalue("//userName");
my $password   = $cred_xpath->findvalue("//password");

$wl->logger->level($debug_level);
$wl->debug_level($debug_level + 1);

# $wl->logger->debug(Dumper(['#001', ''.$username, ''.$password]));
# $wl->logger->debug(Dumper(['#002', $projName, $pluginName, $pluginKey]));
# $wl->logger->debug(Dumper(['#003', $weblogic_url, $wlst_path, $java_home, $java_vendor, $mw_home, $credential, $enable_named_sessions]));

#*****************************************************************************
sub genFileName {
    my ($key, $ext) = @_;

    return File::Spec->catfile($ENV{COMMANDER_WORKSPACE}, sprintf('%s-%06s.%s', $key, int(rand(1_000_000)), $ext || 'txt'));
}

#*****************************************************************************
sub isWin {
    return ($^O eq 'MSWin32') ? 1 : 0;
}

#*****************************************************************************
sub escArgs {
    my (@args) = @_;

    for my $arg (@args) {
        next unless (defined($arg));
        next unless ($arg =~ m/[\s'"]/s);

        my $esca = (isWin) ? q{"} : q{'};

        $arg =~ s/$esca/\\$esca/gs;
        $arg = qq{$esca$arg$esca};
    }

    return @args;
}

#*****************************************************************************
sub hidePwd {
    my (@args) = @_;

    for my $arg (@args) {
        next unless (defined($arg));
        $arg =~ s/-password.+?\s/-password *** /s;
    }

    return @args;
}

#*****************************************************************************
sub readOut {
    my ($fileName) = @_;

    my $msg = '';
    if (open(my $file, $fileName)) {
        local $/ = undef;
        $msg = <$file>;
        close($file);
    }
    else {
        $msg = "Can't open file ($fileName) : $!";
    }

    return $msg;
}

#*****************************************************************************
sub runCommand {
    my (@args) = escArgs(@_);

    printf("Run Command: %s\n", join(' ', hidePwd(@args)));

    if (isWin) {
        print("MSWin32 detected\n");
        $ENV{NOPAUSE} = 1;
    }

    my $fileOut = genFileName('out');
    my $fileErr = genFileName('err');

    local $! = undef;

    system(join(' ', @args, "1>$fileOut", "2>$fileErr"));

    my $ret    = $?;
    my $errmsg = $!;
    my $code   = $ret >> 8;

    my $stdout = readOut($fileOut);
    my $stderr = readOut($fileErr);

    if ($ret == -1) {
        $code = 1;
    }
    elsif ($ret & 0x7f) {
        $code = 1;
        my $err = sprintf("Command died with signal %d, %s coredump\n", ($ret & 0x7f), ($ret & 0x80) ? 'with' : 'without');
        if ($errmsg) {
            $errmsg .= "\n$err";
        }
        else {
            $errmsg .= $err;
        }
    }

    return ($code, $stdout, $stderr, $errmsg);
} ## end sub runCommand

#*****************************************************************************
sub checkConnection {
    if ($java_home) {
        $ENV{JAVA_HOME} = $java_home;
        $wl->out(LEVEL_INFO, "JAVA_HOME was set to '$java_home'");
    }

    if ($java_vendor) {
        $ENV{JAVA_VENDOR} = $java_vendor;
        $wl->out(LEVEL_INFO, "JAVA_VENDOR was set to '$java_vendor'");
    }

    if ($mw_home) {
        $ENV{MW_HOME} = $mw_home;
        $wl->out(LEVEL_INFO, "MW_HOME was set to '$mw_home'");
    }

    my $script = $ENV{COMMANDER_WORKSPACE} . '/do_ls';

    open FH, '>', $script;
    print FH "connect('$username','$password','$weblogic_url'); ls(); disconnect()\n";
    close FH;

    return runCommand($wlst_path, $script);
} ## end sub checkConnection

#*****************************************************************************
my ($code, $stdout, $stderr, $errmsg) = eval {checkConnection();};

my $evalError = $@;
if ($evalError) {
    if ($errmsg) {
        $errmsg .= "\n$evalError";
    }
    else {
        $errmsg = $evalError;
    }

    $code ||= 1;
}

$wl->out(LEVEL_INFO, 'STDOUT: ', $stdout) if ($stdout);
$wl->out(LEVEL_INFO, 'STDERR: ', $stderr) if ($stderr);
$wl->out(LEVEL_INFO, 'ERRMSG: ', $errmsg) if ($errmsg);
$wl->out(LEVEL_INFO, 'EXIT_CODE: ', $code);

if ($code) {
    $errmsg ||= $stderr || $stdout;
    $errmsg =~ s/^(.+?)WLSTException:\s+//s;
    $errmsg =~ s/^(.+?)(?:nested exception is:.*)$/\1/s;
    $errmsg =~ s/^(.+?)(?:Use dumpStack.*)$/\1/s;

    $ec->setProperty('/myJob/configError', $errmsg);
    $ec->setProperty('/myJobStep/summary', $errmsg);

    $wl->logger->error("Connection failed: $code");

    exit(ERROR);
}
else {
    $wl->logger->info("Connection succeeded");
    exit(SUCCESS);
}

#*****************************************************************************
