package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;

public class MulticastMessage {
    MembershipEvent event;
    String nodeId;
    int membershipCounter;
    String address;
    int port;

    public MulticastMessage(MembershipEvent event, String nodeId, int membershipCounter, String address, int port){
        this.event = event;
        this.nodeId = nodeId;
        this.membershipCounter = membershipCounter;
        this.address = address;
        this.port = port;
    }

    public void send(InetAddress multicastAddress, int port) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);

        byte[] buf = this.buildBuffer();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastAddress, port);
        socket.send(packet);
        socket.close();
    }

    private byte[] buildBuffer(){
        StringBuilder builder = new StringBuilder();
        builder.append(event).append(" ")
            .append(nodeId).append(" ")
            .append(membershipCounter).append(" ")
            .append(address).append(" ")
            .append(port);
        return builder.toString().getBytes();
    }
}
