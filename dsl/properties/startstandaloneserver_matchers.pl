
push (@::gMatchers,
  {
   id =>        "serverResponding",
   pattern =>          q{Server is up and running},
   action =>           q{
    
              my $description = "JBoss Server is up and running";
              setProperty("summary", $description . "\n");
    
   },
  },
);

