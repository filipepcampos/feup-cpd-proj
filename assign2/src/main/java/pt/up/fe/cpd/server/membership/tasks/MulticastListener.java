package pt.up.fe.cpd.server.membership.tasks;

import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterManager;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;
import pt.up.fe.cpd.utils.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

public class MulticastListener implements Runnable {
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private ActiveNodeInfo nodeInfo;
    final private ClusterViewer clusterViewer;
    final private ClusterManager clusterManager;
    final private ExecutorService executor;

    public MulticastListener(ActiveNodeInfo nodeInfo, InetAddress multicastAddress, int multicastPort,
                             ClusterViewer clusterViewer, ClusterManager clusterManager, ExecutorService executor){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeInfo = nodeInfo;
        this.clusterViewer = clusterViewer;
        this.clusterManager = clusterManager;
        this.executor = executor;
    }

    @Override
    public void run() {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(multicastPort);
            socket.joinGroup(multicastAddress);
        } catch(IOException e) {
            System.out.println("IO Exception"); // TODO: Error
            e.printStackTrace();
            return;
        }

        byte[] buf = new byte[1024];
        while (clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            DatagramPacket packet = new DatagramPacket(buf, 1024); // TODO: Try out of loop
            try {
                socket.receive(packet);
            } catch(IOException e) {
                socket.close();
                e.printStackTrace();
                return;
            }

            String received = new String(packet.getData(), 0, packet.getLength());
            String[] splitString = received.split(" ");
            String eventType = splitString[0];

            switch(eventType){
                case "JOIN": // Joining
                    System.out.println("[" + this.nodeInfo +"] Received JOIN multicast message");
                    handleJoin(received);
                    break;
                case "LEAVE": // Leaving
                    System.out.println("[" + this.nodeInfo +"] Received LEAVE multicast message");
                    handleLeave(received);
                    break;
                case "MEMBERSHIP": // TODO: Deal with membership info
                    handleMembership(received);
                    break;
            }
        }

        try {
            socket.leaveGroup(multicastAddress);
        } catch(IOException e) {
            socket.close();
            e.printStackTrace();
            return;
        }

        socket.close();
    }

    private Pair<ActiveNodeInfo, Integer> parseJoinLeaveMessage(String receivedData) throws UnknownHostException {
        String[] splitString = receivedData.split(" ");
        String receivedAddress = splitString[1];
        int receivedPort = Integer.parseInt(splitString[2]);
        int receivedCounter = Integer.parseInt(splitString[3]);
        return new Pair<>(new ActiveNodeInfo(receivedAddress, receivedPort), receivedCounter);
    }

    private void handleJoin(String receivedData){
        Pair<ActiveNodeInfo, Integer> parsedData;
        try {
            parsedData = parseJoinLeaveMessage(receivedData);
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

        ActiveNodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getPort() == this.nodeInfo.getPort()) {
            return;
        }

        executor.execute(new MembershipInformationSender(parsedNodeInfo, clusterViewer));
        clusterManager.registerJoinNode(parsedNodeInfo, receivedCounter);
    }

    private void handleLeave(String receivedData){
        Pair<ActiveNodeInfo, Integer> parsedData;
        try {
            parsedData = parseJoinLeaveMessage(receivedData);
        } catch(IOException e){
            e.printStackTrace();
            return;
        }

        NodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getPort() == this.nodeInfo.getPort()) {
            return;
        }

        clusterManager.registerLeaveNode(parsedNodeInfo, receivedCounter);
    }

    private void handleMembership(String receivedData){
        String[] splitMessage = receivedData.split("\n");
        String[] logInfo = splitMessage[2].split(", ");
        for(String logData : logInfo){
            String[] splitLog               = logData.split(" ");
            String[] splitNodeId            = splitLog[0].split(":");
            String receivedAddress          = splitNodeId[0];
            int receivedPort                = Integer.parseInt(splitNodeId[1]);
            int receivedMembershipCounter   = Integer.parseInt(splitLog[1]);
            clusterManager.addLogEntry(new MembershipLogEntry(receivedAddress, receivedPort, receivedMembershipCounter));
        }
    }
}