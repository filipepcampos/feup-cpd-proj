package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

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

        StringBuilder builder = new StringBuilder();
        builder.append(address).append(" ")
            .append(port).append(" ")
            .append(membershipCounter);

        this.send(multicastAddress, multicastPort, builder.toString(), "");
    }

    public void send(InetAddress multicastAddress, int multicastPort, String header, String body) throws IOException {
        
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(multicastAddress);
        
        StringBuilder message = new StringBuilder();
        message.append(event);
        
        if(!header.isEmpty()){
            message.append(" ").append(header);
        }
        
        if(!body.isEmpty()){
            message.append("\n\n").append(body);
        }

        byte[] buf = message.toString().getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastAddress, multicastPort);
        socket.send(packet);
        socket.close();
    }
}
