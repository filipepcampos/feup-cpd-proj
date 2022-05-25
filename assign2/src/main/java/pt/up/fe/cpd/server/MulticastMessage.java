package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;

public class MulticastMessage {
    MembershipEvent event;
    int membershipCounter;
    String address;
    int port;

    public MulticastMessage(MembershipEvent event, int membershipCounter, String address, int port){
        this.event = event;
        this.membershipCounter = membershipCounter;
        this.address = address;
        this.port = port;
    }

    public void send(InetAddress multicastAddress, int multicastPort) throws IOException {
        byte[] buf = this.buildBuffer();
        this.send(multicastAddress, multicastPort, buf);
    }

    public void send(InetAddress multicastAddress, int multicastPort, byte[] data) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);

        byte[] buf = (event + data.toString()).getBytes();  // TODO: Verify if this works

        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastAddress, multicastPort);
        socket.send(packet);
        socket.close();
    }

    private byte[] buildBuffer(){
        StringBuilder builder = new StringBuilder();
        builder.append(address).append(" ")
            .append(port).append(" ")
            .append(membershipCounter);
        return builder.toString().getBytes();
    }
}
