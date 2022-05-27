package pt.up.fe.cpd.server.tasks;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.Connection;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.MembershipEvent;
import pt.up.fe.cpd.server.membership.MembershipMessageSender;
import pt.up.fe.cpd.server.membership.log.MembershipLog;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MulticastMembershipSender implements Runnable {
    final private InetAddress multicastAddress;
    final private Connection connection;
    final private int multicastPort;
    final private int membershipCounter;
    final private Set<NodeInfo> nodeSet;
    final private MembershipLog log;

    public MulticastMembershipSender(InetAddress multicastAddress, int multicastPort, int membershipCounter,
                                     Connection connection, Set<NodeInfo> nodeSet, MembershipLog log){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.membershipCounter = membershipCounter;
        this.connection = connection;
        this.nodeSet = nodeSet;
        this.log = log;
    }

    @Override
    public void run() {
        while(connection.getStatus() == ConnectionStatus.CONNECTED){
            try {
                TimeUnit.SECONDS.sleep(nodeSet.size());
            } catch (InterruptedException e) { // TODO: Ver melhor
                return;
            }

            MembershipMessageSender message = new MembershipMessageSender(MembershipEvent.MEMBERSHIP, membershipCounter, multicastAddress, multicastPort);
            try {
                message.send("", log.toString());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}