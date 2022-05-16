package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;

public class MulticastMessage {
    MembershipEvent event;
    String nodeId;
    int membershipCounter;

    public MulticastMessage(MembershipEvent event, String nodeId, int membershipCounter){
        this.event = event;
        this.nodeId = nodeId;
        this.membershipCounter = membershipCounter;
    }

    public void send(InetAddress multicastAddress, int port) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);

        byte[] buf = this.buildBuffer();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastAddress, port);
        System.out.println("["+nodeId+"] MulticastMessage::sending " + event + " message.");
        socket.send(packet);
        System.out.println("["+nodeId+"] MulticastMessage::message sent.");
        socket.close();
    }

    private byte[] buildBuffer(){
        StringBuilder builder = new StringBuilder();
        builder.append(event).append(" ").append(nodeId).append(" ").append(membershipCounter).append("\n");
        return builder.toString().getBytes();
    }
}
