package pt.up.fe.cpd.server.tasks;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.log.MembershipLog;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class MembershipInformationListener implements Callable<Boolean> {
    InetAddress address;
    int port;
    Set<NodeInfo> nodeList;
    MembershipLog log;

    public MembershipInformationListener(InetAddress address, int port, Set<NodeInfo> nodeList, MembershipLog log){
        this.address = address;
        this.port = port;
        this.nodeList = nodeList;
        this.log = log;
    }

    @Override
    public Boolean call(){
        TCPListener listener;
        try {
            listener = new TCPListener(address, port);
        } catch (IOException e) {
            return false;
        }

        for(int i = 0; i < 3; ++i){
            try {
                String message = listener.receive();
                System.out.println("[debug] TCP membership message received: " + message);

                String[] splitMessage = message.split("\n");
                if(!splitMessage[0].equals("MEMBERSHIP")){
                    // TODO: Error
                }

                String[] nodeListInfo = splitMessage[2].split(", ");
                for(String nodeInfo : nodeListInfo){
                    String[] splitNodeInfo  = nodeInfo.split(":");
                    String receivedAddress  = splitNodeInfo[0];
                    int receivedPort        = Integer.parseInt(splitNodeInfo[1]);
                    nodeList.add(new NodeInfo(receivedAddress, receivedPort));
                }

                String[] logInfo = splitMessage[3].split(", ");
                for(String logData : logInfo){
                    String[] splitLog               = logData.split(" ");
                    String[] splitNodeId            = splitLog[0].split(":");
                    String receivedAddress          = splitNodeId[0];
                    int receivedPort                = Integer.parseInt(splitNodeId[1]);
                    int receivedMembershipCounter   = Integer.parseInt(splitLog[1]);
                    log.addEntry(new MembershipLogEntry(receivedAddress, receivedPort, receivedMembershipCounter));
                }
            } catch(IOException e) {
                listener.close();
                return false;
            }
        }
        listener.close();
        return true;
    }
}