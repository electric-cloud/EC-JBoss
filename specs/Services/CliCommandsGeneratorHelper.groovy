package Services

import Models.JBoss.Domain.*
import Utils.EnvPropertiesHelper

class CliCommandsGeneratorHelper {
    static String addServerGroupCmd(ServerGroupHelper serverGroup) {
        return addServerGroupCmd(serverGroup.getServerGroupName(), serverGroup.getProfile(), serverGroup.getSocketBindingGroup())
    }

    static String addServerGroupCmd(String serverGroupName, String profile = "default", String socketBindingGroup = "standard-sockets") {
        String command = "/server-group=$serverGroupName:add(profile=$profile, socket-binding-group=$socketBindingGroup)"
        return command
    }

    static String removeServerGroupCmd(ServerGroupHelper serverGroup) {
        return removeServerGroupCmd(serverGroup.getServerGroupName())
    }

    static String removeServerGroupCmd(String serverGroupName) {
        String command = "/server-group=$serverGroupName/:remove"
        return command
    }

    static String addServerCmd(ServerHelper server) {
        return addServerCmd(server.getServerName(), server.getServerGroupName(), server.getHostName(), server.getAutoStart() ? "true" : "false", server.getSocketBindingPortOffset())
    }

    static String addServerCmd(String serverName, String serverGroupName, String hostName, String autoStart = "true", int socketBindingPortOffset = 0) {
        String command = "/host=$hostName/server-config=$serverName/:add(group=$serverGroupName,auto-start=$autoStart,socket-binding-port-offset=$socketBindingPortOffset)"
        return command
    }

    static String removeServerCmd(ServerHelper server) {
        return removeServerCmd(server.getServerName(), server.getHostName())
    }

    static String removeServerCmd(String serverName, String hostName) {
        String command = "/host=$hostName/server-config=$serverName/:remove"
        return command
    }

    static String startServerCmd(ServerHelper server) {
        return startServerCmd(server.getServerName(), server.getHostName());
    }

    static String startServerCmd(String serverName, String hostName) {
        String command = "/host=$hostName/server-config=$serverName:start"
        return command
    }

    static String stopServerCmd(ServerHelper server) {
        return stopServerCmd(server.getServerName(), server.getHostName());
    }

    static String stopServerCmd(String serverName, String hostName) {
        String command = "/host=$hostName/server-config=$serverName:stop"
        return command
    }

    static String getServerStatusInDomain(ServerHelper server) {
        return getServerStatusInDomain(server.getServerName(), server.getHostName());
    }

    static String getServerStatusInDomain(String serverName, String hostName) {
        String command = "/host=$hostName/server-config=$serverName/:read-attribute(name=status)"
        return command
    }

    static String getDeploymentRuntimeName(String appName) {
        String command = "/deployment=$appName/:read-attribute(name=runtime-name)"
        return command
    }

    static String getDeploymentInfoOnServerGroup(String serverGroup, String appName) {
        String command = "/server-group=$serverGroup/deployment=$appName/:read-resource(recursive=false)"
        return command
    }

    static String getDeploymentInfoOnContentRepo(String appName) {
        String command = "/deployment=$appName/:read-resource(recursive=false,include-runtime=true)"
        return command
    }

    static String getDeploymentEnabledTimeOnServer(String host, String server, String appName) {
        String command = "/host=$host/server=$server/deployment=$appName/:read-attribute(name=enabled-time)"
        return command
    }

    static String undeployFromStandalone(String appName) {
        String command = "undeploy $appName"
        return command
    }

    static String undeployFromAllRelevantServerGroups(String appName) {
        String command = "undeploy $appName --all-relevant-server-groups"
        return command
    }

    static String deployToStandalone(String pathToSourceFile, String name = "", String runtimeName = "") {
        String command = "deploy $pathToSourceFile" + (name.isEmpty() ? "" : " --name=$name") + (runtimeName.isEmpty() ? "" : " --runtime-name=$runtimeName")
        return command
    }

    static String deployToAllServerGroup(String pathToSourceFile, String name = "", String runtimeName = "") {
        String command = "deploy $pathToSourceFile" + (name.isEmpty() ? "" : " --name=$name") + (runtimeName.isEmpty() ? "" : " --runtime-name=$runtimeName") + " --all-server-groups"
        return command
    }

    static String deployToServerGroups(String[] serverGroups, String pathToSourceFile, String name = "", String runtimeName = "") {
        if (!serverGroups) {
            throw new Exception("serverGroups should be non empty array")
        }
        String serverGroupsStr = Arrays.toString(serverGroups).replaceAll(", ", ",").replaceAll("[\\[\\]]", "");
        String command = "deploy $pathToSourceFile" + (name.isEmpty() ? "" : " --name=$name") + (runtimeName.isEmpty() ? "" : " --runtime-name=$runtimeName") + " --server-groups=$serverGroupsStr"
        return command
    }

    static String getServerGroupInfo(String serverGroup) {
        String command = "/server-group=$serverGroup/:read-resource(recursive=false)"
        return command
    }

    static String reloadHostDomain(String hostName) {
        String command = "/host=$hostName:reload"
        if(EnvPropertiesHelper.getVersion() =~ '6.0'){
            command = "/host=$hostName:shutdown(restart=true)"
        }
        return command
    }

    static String getJMSQueueInfoStandalone(String queueName) {
        String subsystem = getJMSsubsystem();
        String command = "/$subsystem/jms-queue=$queueName:read-resource()"
        return command
    }

    static String getJMSQueueInfoDomain(String queueName, String profile) {
        String subsystem = getJMSsubsystem();
        String command = "/profile=$profile/$subsystem/jms-queue=$queueName:read-resource()"
        return command
    }

    static String getJMSTopicInfoStandalone(String topicName) {
        String subsystem = getJMSsubsystem();
        String command = "/$subsystem/jms-topic=$topicName:read-resource()"
        return command
    }

    static String getJMSTopicInfoDomain(String topicName, String profile) {
        String subsystem = getJMSsubsystem();
        String command = "/profile=$profile/$subsystem/jms-topic=$topicName:read-resource()"
        return command
    }

    static String removeJMSQueueStandalone(String queueName) {
        String command = "jms-queue remove --queue-address=$queueName"
        return command
    }

    static String removeJMSQueueDomain(String queueName, String profile) {
        String command = "jms-queue remove --queue-address=$queueName --profile=$profile"
        return command
    }

    static String removeJMSTopicStandalone(String topicName) {
        String command = "jms-topic remove --topic-address=$topicName"
        return command
    }

    static String removeJMSTopicDomain(String topicName, String profile) {
        String command = "jms-topic remove --topic-address=$topicName --profile=$profile"
        return command
    }

    static String reloadStandalone() {
        String command = "reload"
        return command
    }

    static String reloadServerGroupDomain(String serverGroup) {
        String command = "/server-group=$serverGroup:restart-servers"
        return command
    }

    static String addJMSQueueDefaultStandalone(String queueName, String jndiName) {
        addJMSQueue(queueName, jndiName, "--durable=false", "", "")
    }

    static String addJMSQueueDefaultDomain(String queueName, String jndiName, String profile) {
        addJMSQueue(queueName, jndiName, "--durable=false", "", " --profile=$profile")
    }

    static String addJMSQueue(String queueName, String jndiName, String durable, String messageSelector, String profile) {
        String command = "jms-queue add --queue-address=$queueName --entries=[$jndiName] " + durable + messageSelector + profile
        return command
    }

    static String addJMSTopicStandalone(String topicName, String jndiName) {
        return addJMSTopicDomain(topicName, jndiName, "")
    }

    static String addJMSTopicDomain(String topicName, String jndiName, String profile) {
        String command = "jms-topic add --topic-address=$topicName --entries=[$jndiName]  $profile"
        return command
    }

    static String getJMSsubsystem() {
        String subsystem_part = "subsystem=messaging-activemq"
        String provider_part = "server=default"

        logger.info("version "+EnvPropertiesHelper.getVersion())
        if(EnvPropertiesHelper.getVersion() =~ "6.*") {
            subsystem_part = "subsystem=messaging"
            provider_part = "hornetq-server=default"
        }

        String command = "$subsystem_part/$provider_part"
        logger.info("JMS info command "+command)
        return command
    }


}