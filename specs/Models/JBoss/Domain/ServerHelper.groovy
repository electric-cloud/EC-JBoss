package Models.JBoss.Domain

class ServerHelper {
    String serverName
    String serverGroupName
    String hostName
    boolean autoStart = true
    int socketBindingPortOffset = 0

    ServerHelper(String serverName, String serverGroupName, String hostName) {
        this.serverName = serverName
        this.serverGroupName = serverGroupName
        this.hostName = hostName
    }

    boolean getAutoStart() {
        return autoStart
    }

    void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart
    }

    int getSocketBindingPortOffset() {
        return socketBindingPortOffset
    }

    void setSocketBindingPortOffset(int portOffset) {
        this.socketBindingPortOffset = portOffset
    }
}