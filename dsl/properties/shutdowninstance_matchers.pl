
push (@::gMatchers,
  {
   id =>        "serverTurnedOff",
   pattern =>          q{\"outcome\" => \"success\"(.+)},
   action =>           q{
    
                 my $description = "Server was turned off successfully.";                 	
              setProperty("summary", $description . "\n");
    
   },
  },    {   id =>        "serverNotRunning",   pattern =>          q{You are disconnected at the moment(.+)},   action =>           q{                  my $description = "Server not responding, make sure instance is running.";                            setProperty("summary", $description . "\n");       },  },      
);

