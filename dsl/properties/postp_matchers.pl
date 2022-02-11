#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
$::gWarningsList = [];
@::gMatchers = (
    {
        id =>        "warning",
        pattern =>          q{WARNING:(.+)},
        action =>           q{incValue("warnings");eval{&replaceSummary(addWarning("Warning: $1"));};setWarnings();},
        # action => q{setWarnings();}
    },
);

sub addSimpleError {
    my ($name, $customError) = @_;
    if(!defined $::gProperties{$name}){
        setProperty ($name, $customError);
        replaceSummary ($customError);
    }
}
sub replaceSummary($) {
    my ($str) = @_;
    setProperty("summary", $str);
    setProperty("/myParent/summary", $str);
}

sub addWarning {
    my ($str) = @_;

    push @{$::gWarningsList}, $str;
    return join "\n", @{$::gWarningsList};
}
sub setWarnings {
    eval {
        setProperty("/myParent/outcome", "warning" )
    };
    eval {
        setProperty("/myJobStep/outcome", "warning" )
    };
    eval {
        setProperty("outcome", "warning" )
    };
    # setProperty("/myJobStep/outcome", 'error');
}
