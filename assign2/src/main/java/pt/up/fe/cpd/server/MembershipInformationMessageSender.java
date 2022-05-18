package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.cpd.networking.TCPMessageSender;

public class MembershipInformationMessageSender extends TCPMessageSender {
    public MembershipInformationMessageSender(InetAddress address, int port){
        super(address, port);
    }

    public void send(List<Node> nodes, MembershipLog log) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
            .append(nodes.stream().map(n -> n.toString()).collect(Collectors.joining(", ")))
            .append('\n')
            .append(log.toString());

        super.send(stringBuilder.toString().getBytes());
    }
}