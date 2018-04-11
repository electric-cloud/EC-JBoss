package dsl

def projName = args.projName
def resName = args.resName
def procNameDownloadArtifact = args.procNameDownloadArtifact
def procNameCheckUrl = args.procNameCheckUrl
def procNameMkdir = args.procNameMkdir
def someShell = args.shell
def procNameRunCustomCliCommand = args.procNameRunCustomCliCommand


project projName, {

    procedure procNameDownloadArtifact, {
        resourceName = resName
        step procNameDownloadArtifact, {
            command = '''
use strict;
use warnings;
use LWP::UserAgent;
use Data::Dumper;
use Cwd qw(chdir);
use File::Basename;

my $ua = LWP::UserAgent->new;
my $url = \'$[url]\';

my $request = HTTP::Request->new(GET => $url);
print Dumper $request;

my $path = \'$[artifactPath]\';
my $dir = dirname($path);
chdir($dir);

if (-f $path) {
    unlink $path;
}
print "$path\\n";
open my $fh, \">$path\" or die $!;
binmode($fh);
my $size = 0;
my $response = $ua->request($request, sub {
    my ($bytes, $res) = @_;

    $size += length($bytes);

    if ($res->is_success) {
        print $fh $bytes;
    }
    else {
        print $res->code;
        exit -1;
    }
});

close $fh;
            '''
            shell = 'ec-perl'
        }
        formalParameter 'url', defaultValue: '', {
            type = "textarea"
        }

        formalParameter 'artifactPath', defaultValue: '', {
            type = 'entry'
        }
    }

    procedure procNameCheckUrl, {
        resourceName = resName
        step procNameCheckUrl, {
            command = '''
use strict;
use warnings;
use LWP::UserAgent;
use Data::Dumper;

my $ua = LWP::UserAgent->new;
my $url = \'$[url]\';

my $resp = $ua->get($url);
if ( $resp->is_success() ) {
    print "response is success";
}
else {
    print "http response status code: " . $resp->code;
    exit -1;
}
            '''
            shell = 'ec-perl'
        }
        formalParameter 'url', defaultValue: '', {
            type = "textarea"
        }
    }

    procedure procNameMkdir, {
        shell = someShell
         resourceName = resName
          step procNameMkdir , {
           command = 'mkdir "\$[directory]"'

           }
          formalParameter 'directory', defaultValue: '', {
          type = 'entry'
              }
                 }

    procedure procNameRunCustomCliCommand, {
        resourceName = resName
        step procNameRunCustomCliCommand , {
            resourceName = '\$[stepRes]'
            command = '\$[cli_command]'
            shell = 'bash'
            }
            formalParameter 'cli_command', defaultValue: '', {
            type = 'entry'
            }
            formalParameter 'stepRes', defaultValue: '', {
            type = 'entry'
            }
        }
    

}
