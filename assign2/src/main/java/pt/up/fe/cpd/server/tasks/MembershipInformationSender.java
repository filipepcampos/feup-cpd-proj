package pt.up.fe.cpd.server.tasks;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.MembershipInformationMessenger;
import pt.up.fe.cpd.server.membership.log.MembershipLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MembershipInformationSender implements Runnable {
    private InetAddress address;
    final private int port;
    final private Set<NodeInfo> nodeSet;
    final private MembershipLog log;

    public MembershipInformationSender(String address, int port, Set<NodeInfo> nodeSet, MembershipLog log){
        try {
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;
        this.nodeSet = nodeSet;
        this.log = log;
    }

    @Override
    public void run(){
        int minWait = 50;
        int maxWait = 1000;
        int randomNum = ThreadLocalRandom.current().nextInt(minWait, maxWait);
        try {
            TimeUnit.MILLISECONDS.sleep(randomNum);
        } catch (InterruptedException e) { // TODO: Ver melhor
            return;
        }
        System.out.println("[debug] Sending TCP membership info (waited " + randomNum + " ms)");
        MembershipInformationMessenger sender = new MembershipInformationMessenger(this.address, this.port);
        try {
            sender.send(nodeSet, log);
        } catch (IOException e) {
            return;     // TODO: Handle it better
        }

    }
}