package pt.up.fe.cpd.server.membership;

import pt.up.fe.cpd.networking.MulticastMessageSender;
import java.io.IOException;
import java.net.*;

public class MembershipMessageSender {
    MulticastMessageSender messageSender;
    MembershipEvent event;
    int membershipCounter;

    public MembershipMessageSender(MembershipEvent event, int membershipCounter, InetAddress address, int port){
        this.messageSender = new MulticastMessageSender(address, port);
        this.event = event;
        this.membershipCounter = membershipCounter;
    }

    public void send(String storeAddress, int storePort) throws IOException {
        String header = storeAddress+ " " + storePort + " " +  membershipCounter;
        this.send(header, "");
    }

    public void send(String header, String body) throws IOException {
        StringBuilder message = new StringBuilder();
        message.append(event);
        
        if(!header.isEmpty()){
            message.append(" ").append(header);
        }
        
        if(!body.isEmpty()){
            message.append("\n\n").append(body);
        }

        byte[] data = message.toString().getBytes();
        messageSender.send(data);
    }
}
