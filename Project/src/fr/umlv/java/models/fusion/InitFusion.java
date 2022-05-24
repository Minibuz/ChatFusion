package fr.umlv.java.models.fusion;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

public class InitFusion {
    private final String serverName;
    private final InetSocketAddress socketAddress;
    private final List<String> members;

    public InitFusion(String serverName, InetSocketAddress socketAddress, List<String> members) {
        this.serverName = serverName;
        this.socketAddress = socketAddress;
        this.members = members;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getServerName() {
        return serverName;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }
}
