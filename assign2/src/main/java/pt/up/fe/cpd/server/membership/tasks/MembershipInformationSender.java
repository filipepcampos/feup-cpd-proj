package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.MembershipInformationMessenger;
import pt.up.fe.cpd.server.membership.log.MembershipLog;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MembershipInformationSender implements Runnable {
    final private ActiveNodeInfo nodeInfo;
    final private Set<NodeInfo> nodeSet;
    final private MembershipLog log;

    public MembershipInformationSender(ActiveNodeInfo nodeInfo, Set<NodeInfo> nodeSet, MembershipLog log){
        this.nodeInfo = nodeInfo;
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
        MembershipInformationMessenger sender = new MembershipInformationMessenger(this.nodeInfo.getInetAddress(), this.nodeInfo.getPort());
        try {
            sender.send(nodeSet, log);
        } catch (IOException e) {
            return;     // TODO: Handle it better
        }

    }
}