package pt.up.fe.cpd.server.tasks;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.Connection;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.log.MembershipLog;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;
import pt.up.fe.cpd.utils.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class MulticastListener implements Runnable {
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private NodeInfo nodeInfo;
    final private Connection connection;
    final private Set<NodeInfo> nodeSet;
    final private MembershipLog log;
    final private ExecutorService executor;

    public MulticastListener(NodeInfo nodeInfo, InetAddress multicastAddress, int multicastPort
        , Connection connection, MembershipLog log, Set<NodeInfo> nodeSet, ExecutorService executor){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeInfo = nodeInfo;
        this.connection = connection;
        this.log = log;
        this.nodeSet = nodeSet;
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
        while (connection.getStatus() == ConnectionStatus.CONNECTED) {
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

            System.out.println("[debug] Multicast message received: " + received);

            switch(eventType){
                case "JOIN": // Joining
                    handleJoin(received);
                    break;
                case "LEAVE": // Leaving
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

    private Pair<NodeInfo, Integer> parseJoinLeaveMessage(String receivedData){
        String[] splitString = receivedData.split(" ");
        String receivedAddress = splitString[1];
        int receivedPort = Integer.parseInt(splitString[2]);
        int receivedCounter = Integer.parseInt(splitString[3]);
        return new Pair<>(new NodeInfo(receivedAddress, receivedPort), receivedCounter);
    }

    private void handleJoin(String receivedData){
        Pair<NodeInfo, Integer> parsedData = parseJoinLeaveMessage(receivedData);
        NodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getStoragePort() == this.nodeInfo.getStoragePort()) {
            return;
        }

        System.out.println("[debug] executing membershipInfoSender");
        executor.execute(new MembershipInformationSender(parsedNodeInfo.getAddress(), parsedNodeInfo.getStoragePort(), nodeSet, log));
        nodeSet.add(parsedNodeInfo);
        log.addEntry(new MembershipLogEntry(parsedNodeInfo.getAddress(), parsedNodeInfo.getStoragePort(), receivedCounter));
        System.out.println("[debug] executing membershipInfoSender");
    }

    private void handleLeave(String receivedData){
        Pair<NodeInfo, Integer> parsedData = parseJoinLeaveMessage(receivedData);
        NodeInfo parsedNodeInfo = parsedData.first;
        int receivedCounter = parsedData.second;

        if (parsedNodeInfo.getAddress().equals(this.nodeInfo.getAddress()) &&
                parsedNodeInfo.getStoragePort() == this.nodeInfo.getStoragePort()) {
            return;
        }

        nodeSet.remove(parsedNodeInfo);
        log.addEntry(new MembershipLogEntry(parsedNodeInfo.getAddress(), parsedNodeInfo.getStoragePort(), receivedCounter));
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
            log.addEntry(new MembershipLogEntry(receivedAddress, receivedPort, receivedMembershipCounter));
        }
    }
}