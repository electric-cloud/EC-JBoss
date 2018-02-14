package EC::Procedures::Factories::DeployAppFactory;

use strict;
use warnings;

use base qw(Exporter);

use EC::Procedures::Models::DeployAppProcedure;
use EC::Utils::CommonCommanderUtils qw(get_current_procedure_param_value);

our @EXPORT_OK = qw(get_current_deploy_app_model);

# inflexible factory method
sub get_current_deploy_app_model {
    my $deploy_app_procedure = EC::Procedures::Models::DeployAppProcedure->new(
        configuration_name              => get_current_procedure_param_value('serverconfig'),
        application_content_source_path => get_current_procedure_param_value('warphysicalpath'),
        deployment_name                 => get_current_procedure_param_value('appname'),
        runtime_name                    => get_current_procedure_param_value('runtimename'),
        enabled_server_groups           => get_current_procedure_param_value('assignservergroups'),
        #        disabled_server_groups          => get_current_procedure_param_value('disabledServerGroups'),
        additional_options              => get_current_procedure_param_value('additional_options')
    );

    return $deploy_app_procedure;
}

1;