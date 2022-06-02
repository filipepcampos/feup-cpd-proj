package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.membership.*;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class MulticastMembershipSender implements Runnable {
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private int membershipCounter;
    final private ClusterViewer clusterViewer;

    public MulticastMembershipSender(InetAddress multicastAddress, int multicastPort, int membershipCounter, ClusterViewer clusterViewer){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.membershipCounter = membershipCounter;
        this.clusterViewer = clusterViewer;
    }

    @Override
    public void run() {
        while(clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED){
            try {
                TimeUnit.SECONDS.sleep(clusterViewer.getNodeCount());
            } catch (InterruptedException e) { // TODO: Ver melhor
                return;
            }

            MembershipMessenger message = new MembershipMessenger(MembershipEvent.MEMBERSHIP, membershipCounter, multicastAddress, multicastPort);
            try {
                System.out.println("Sending membershipLog via multicast");
                message.send("", clusterViewer.getLogRepresentation());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}