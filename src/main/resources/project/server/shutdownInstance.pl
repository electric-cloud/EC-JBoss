# -------------------------------------------------------------------------
   # File
   #    shutdownInstance.pl
   #
   # Dependencies
   #    None
   #
   # Template Version
   #    1.0
   #
   # Date
   #    09/01/2011
   #
   # Engineer
   #    Alonso Blanco
   #
   # Copyright (c) 2011 Electric Cloud, Inc.
   # All rights reserved
   # -------------------------------------------------------------------------
   
   
   # -------------------------------------------------------------------------
   # Includes
   # -------------------------------------------------------------------------
   use ElectricCommander;
   use warnings;
   use strict;
   use Cwd;
   use File::Spec;
   use diagnostics;
   use ElectricCommander::PropDB;
   $|=1;
   
   # -------------------------------------------------------------------------
   # Constants
   # -------------------------------------------------------------------------
   use constant {
       SUCCESS => 0,
       ERROR   => 1,              SQUOTE => q{'},       DQUOTE => q{"},       BSLASH => q{\\},
       
       PLUGIN_NAME => 'EC-JBoss',
       WIN_IDENTIFIER => 'MSWin32',
       CREDENTIAL_ID => 'credential',       
       
  };
  ########################################################################
  # trim - deletes blank spaces before and after the entered value in 
  # the argument
  #
  # Arguments:
  #   -untrimmedString: string that will be trimmed
  #
  # Returns:
  #   trimmed string
  #
  ########################################################################  
  sub trim($) {
   
      my ($untrimmedString) = @_;
      
      my $string = $untrimmedString;
      
      #removes leading spaces
      $string =~ s/^\s+//;
      
      #removes trailing spaces
      $string =~ s/\s+$//;
      
      #returns trimmed string
      return $string;
  }
  
  # -------------------------------------------------------------------------
  # Variables
  # -------------------------------------------------------------------------
  $::gEC = new ElectricCommander();
      $::gEC->abortOnError(0);      
  $::gScriptPhysicalLocation = ($::gEC->getProperty("scriptphysicalpath") )->findvalue("//value");
  $::gServerConfig = ($::gEC->getProperty("serverconfig") )->findvalue("//value");  
  # -------------------------------------------------------------------------
  # Main functions
  # -------------------------------------------------------------------------  
  ########################################################################
  # main - contains the whole process to be done by the plugin, it builds 
  #        the command line, sets the properties and the working directory
  #
  # Arguments:
  #   none
  #
  # Returns:
  #   none
  #
  ########################################################################
  sub main() {
   
    my $cmdLine = '';
    
    my %props;        my $rawUrl = '';    my $user = '';    my $pass = '';    my %configuration;        my $content;    #getting all info from the configuration, url, user and pass    if($::gServerConfig ne ''){                %configuration = getConfiguration($::gServerConfig);        if(%configuration){                        $rawUrl = $configuration{'jboss_url'};        	            my $url;            my $port;            print "$rawUrl\n";                        #checking if raw url comes in the format http(s)://whatever(:port)/(path)            if($rawUrl =~ m/http(\w*):\/\/(\S[^:]*)(:*)(\d*)(\/*)(.*)/){                $url = $2;                $port = $4;            }elsif($rawUrl =~ m/(\S[^:]*)(:*)(\d*)(\/*)(.*)/){                $url = $1;                $port = $3;        	}else{        		print "Error: Not a valid URL.\n";        		exit ERROR;        	}        	        	print "url: $url port: $port\n";        	        	$cmdLine = "\"$::gScriptPhysicalLocation\" --connect controller=$url:$port command=:shutdown";                }            }else{    	        $cmdLine = "\"$::gScriptPhysicalLocation\" --connect command=:shutdown";    	    }        $content = `$cmdLine`;    print $content;    #evaluates if exit was successful to mark it as a success or fail the step    if($? == SUCCESS){		if ($content =~ m/\"outcome\" => \"success\"(.+)/){    
			#server was turned off			$::gEC->setProperty("/myJobStep/outcome", 'success');		}elsif ($content =~ m/You are disconnected at the moment(.+)/){            
			#if not, an exception was reached
			$::gEC->setProperty("/myJobStep/outcome", 'error');
		}    }else{        $::gEC->setProperty("/myJobStep/outcome", 'error');    }    #add masked command line to properties object    $props{'cmdLine'} = $cmdLine;
    setProperties(\%props);
  }        ########################################################################  # startServer - uses ecdaemon for starting a Server  #  # Arguments:  #   -jboss server script: absolute path to managed server script  #   -server name: name of the instance of the managed server  #   -URL: URL (including protocol and port) of the Admin Server of the domain  #   -user: user of the admin server  #   -password: password of the admin server  #  # Returns:  #   none  #  ########################################################################  sub startServer($){         my ($scriptPhysicalLocation, $alternateConfig) = @_;         # $The quote and backslash constants are just a convenient way to represtent literal literal characters so it is obvious      # in the concatentations. NOTE: BSLASH ends up being a single backslash, it needs to be doubled here so it does not      # escape the right curly brace.            my $operatingSystem = $^O;      print qq{OS: $operatingSystem\n};                # Ideally, the logs should exist in the step's workspace directory, but because the ecdaemon continues after the step is      # completed the temporary drive mapping to the workspace is gone by the time we want to write to it. Instead, the log      # and errors get the JOBSTEPID appended and it goes in the Tomcat root directory.      my $LOGNAMEBASE = "jbossstartmanagedserver";            # If we try quoting in-line to get the final string exactly right, it will be confusing and ugly. Only the last      # parameter to our outer exec() needs _literal_ single and double quotes inside the string itself, so we build that      # parameter before the call rather than inside it. Using concatenation here both substitutes the variable values and      # puts literal quote from the constants in the final value, but keeps any other shell metacharacters from causing      # trouble.            my @systemcall;            if($operatingSystem eq WIN_IDENTIFIER) {                 # Windows has a much more complex execution and quoting problem. First, we cannot just execute under "cmd.exe"          # because ecdaemon automatically puts quote marks around every parameter passed to it -- but the "/K" and "/C"          # option to cmd.exe can't have quotes (it sees the option as a parameter not an option to itself). To avoid this, we          # use "ec-perl -e xxx" to execute a one-line script that we create on the fly. The one-line script is an "exec()"          # call to our shell script. Unfortunately, each of these wrappers strips away or interprets certain metacharacters          # -- quotes, embedded spaces, and backslashes in particular. We end up escaping these metacharacters repeatedly so          # that when it gets to the last level it's a nice simple script call. Most of this was determined by trial and error          # using the sysinternals procmon tool.          my $commandline = "sh " . BSLASH . BSLASH . BSLASH . DQUOTE . $scriptPhysicalLocation . BSLASH . BSLASH . BSLASH . DQUOTE;                    if ($alternateConfig && $alternateConfig ne ''){              $commandline .= "--server-config=" . BSLASH . BSLASH . BSLASH . DQUOTE . $alternateConfig . BSLASH . BSLASH . BSLASH . DQUOTE;          }                    my $logfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".log";          my $errfile = $LOGNAMEBASE . "-" . $ENV{'COMMANDER_JOBSTEPID'} . ".err";          $commandline = SQUOTE . $commandline .  " 1>" . $logfile . " 2>" . $errfile . SQUOTE;          $commandline = "exec(" . $commandline . ");";          $commandline = DQUOTE . $commandline . DQUOTE;          @systemcall = ("ecdaemon", "--", "ec-perl", "-e", $commandline);                } else {                 # Linux is comparatively simple, just some quotes around the script name in case of embedded spaces.          # IMPORTANT NOTE: At this time the direct output of the script is lost in Linux, as I have not figured out how to          # safely redirect it. Nothing shows up in the log file even when I appear to get the redirection correct; I believe          # the script might be putting the output to /dev/tty directly (or something equally odd). Most of the time, it's not          # really important since the vital information goes directly to $CATALINA_HOME/logs/catalina.out anyway. It can lose          # important error messages if the paths are bad, etc. so this will be a JIRA.          my $commandline = "sh " . DQUOTE . $scriptPhysicalLocation . DQUOTE;                    if ($alternateConfig ne ''){              $commandline .= "--server-config=" . DQUOTE . $alternateConfig . DQUOTE;          }                    @systemcall = ("ecdaemon", "--", "sh", "-c", $commandline);                }            #print "Command Parameters:\n" . Dumper(@systemcall) . "--------------------\n";            my %props;          my $cmdLine = createCommandLine(\@systemcall);      $props{'startStandaloneServerLine'} = $cmdLine;      setProperties(\%props);            print "cmd line: $cmdLine\n";      system($cmdLine);  }  
  ########################################################################
  # createCommandLine - creates the command line for the invocation
  # of the program to be executed.
  #
  # Arguments:
  #   -arr: array containing the command name (must be the first element) 
  #         and the arguments entered by the user in the UI
  #
  # Returns:
  #   -the command line to be executed by the plugin
  #
  ########################################################################
  sub createCommandLine($) {
      
      my ($arr) = @_;
      
      my $commandName = @$arr[0];
      
      my $command = $commandName;
      
      shift(@$arr);
      
      foreach my $elem (@$arr) {
          $command .= " $elem";
      }
      
      return $command;
         
  }
  
  ########################################################################
  # setProperties - set a group of properties into the Electric Commander
  #
  # Arguments:
  #   -propHash: hash containing the ID and the value of the properties 
  #              to be written into the Electric Commander
  #
  # Returns:
  #   none
  #
  ########################################################################
  sub setProperties($) {  
      my ($propHash) = @_;
     
      foreach my $key (keys % $propHash) {
          my $val = $propHash->{$key};
          $::gEC->setProperty("/myCall/$key", $val);
      }
  }
  ##########################################################################
  # getConfiguration - get the information of the configuration given
  #
  # Arguments:
  #   -configName: name of the configuration to retrieve
  #
  # Returns:
  #   -configToUse: hash containing the configuration information
  #
  #########################################################################
  sub getConfiguration($){
      my ($configName) = @_;
      my %configToUse;
      my $proj = "$[/myProject/projectName]";
      my $pluginConfigs = new ElectricCommander::PropDB($::gEC,"/projects/$proj/jboss_cfgs");
      my %configRow = $pluginConfigs->getRow($configName);
      # Check if configuration exists
      unless(keys(%configRow)) {
          print 'Error: Configuration doesn\'t exist';
          exit ERROR;
      }
      # Get user/password out of credential
      my $xpath = $::gEC->getFullCredential($configRow{credential});
      $configToUse{'user'} = $xpath->findvalue("//userName");
      $configToUse{'password'} = $xpath->findvalue("//password");
      
      foreach my $c (keys %configRow) {
          #getting all values except the credential that was read previously
          if($c ne CREDENTIAL_ID){
              $configToUse{$c} = $configRow{$c};
          }
          
      }

      return %configToUse;
  }  
  main();
  1;
