package Services

import Models.JBoss.Domain.*

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
}