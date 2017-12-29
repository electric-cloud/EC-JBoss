package Models.JBoss.Domain

class ServerGroupHelper {
    String serverGroupName
    String profile = "default"
    String socketBindingGroup = "standard-sockets"

    ServerGroupHelper(String serverGroupName) {
        this.serverGroupName = serverGroupName
    }

    String getProfile() {
        return profile
    }

    void setProfile(String profile) {
        this.profile = profile
    }

    String getSocketBindingGroup() {
        return socketBindingGroup
    }

    void setSocketBindingGroup(String socketBindingGroup) {
        this.socketBindingGroup = socketBindingGroup
    }
}