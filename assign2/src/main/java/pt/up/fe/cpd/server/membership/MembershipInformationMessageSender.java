package pt.up.fe.cpd.server.membership;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.cpd.networking.TCPMessageSender;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.log.MembershipLog;

/*
Sends membership information over TCP
 */
public class MembershipInformationMessageSender extends TCPMessageSender {
    public MembershipInformationMessageSender(InetAddress address, int port){
        super(address, port);
    }

    public void send(Set<NodeInfo> nodes, MembershipLog log) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
            .append("MEMBERSHIP\n\n")
            .append(nodes.stream().map(n -> n.toString()).collect(Collectors.joining(", ")))
            .append('\n')
            .append(log.toString());

        super.send(stringBuilder.toString().getBytes());
    }
}