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

}