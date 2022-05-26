package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import java.util.concurrent.ThreadLocalRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.utils.Pair;

public class Node implements MembershipService {
    final private HashSet<NodeInfo> nodeList;
    final private MembershipLog log;
    private InetAddress multicastAddress;
    final private int multicastPort;
    private InetAddress address;
    final private int storagePort;

    private byte[] nodeId;              // Hashed address
    
    private int membershipCounter;
    final private ExecutorService executor;   // ThreadPool
    private ConnectionStatus connectionStatus;

    public Node(String multicastAddress, int multicastPort, String address, int storagePort) {

        this.nodeList = new HashSet<>();
        this.nodeList.add(new NodeInfo(address, storagePort));
        this.log = new MembershipLog();

        try {
            this.multicastAddress = InetAddress.getByName(multicastAddress);
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.multicastPort = multicastPort;
        this.storagePort = storagePort;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.nodeId = digest.digest(address.getBytes(StandardCharsets.UTF_8));
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            // TODO: What to do
        }

        this.membershipCounter = 0; // TODO: Write/Read from file
        this.executor = Executors.newFixedThreadPool(8);
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    public void join() {
        synchronized (this.connectionStatus){
            if (this.connectionStatus != ConnectionStatus.DISCONNECTED) return;
            this.connectionStatus = ConnectionStatus.CONNECTING;
        }
        
        printDebugInfo("Joining the cluster");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.membershipCounter, this.address.getHostAddress(), this.storagePort);
        try {
            for (int i = 0; i < 3; ++i) {
                Future<Boolean> futureResult = executor.submit(new MembershipInformationListener());
                message.send(this.multicastAddress, this.multicastPort);
                printDebugInfo("JOIN multicast message sent (" + (i+1) + "/3)");
                Boolean joinedSuccessfully = false;
                try {
                    joinedSuccessfully = futureResult.get();
                } catch (InterruptedException | ExecutionException e){
                    e.printStackTrace();
                }

                if (joinedSuccessfully) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (this.connectionStatus){
            this.connectionStatus = ConnectionStatus.CONNECTED;   
        }
        
        this.log.addEntry(new MembershipLogEntry(this.address.getHostAddress(), this.storagePort, this.membershipCounter));
        this.membershipCounter++;
        executor.execute(new MulticastListener());
        executor.execute(new MulticastMembershipSender());
    }

    public void leave() {
        /*
        - Multicast LEAVE message
        - Update membership counter
        */

        printDebugInfo("Leaving the cluster");

        synchronized (this.connectionStatus){
            if (this.connectionStatus != ConnectionStatus.CONNECTED) return;
            this.connectionStatus = ConnectionStatus.DISCONNECTING;
        }
        
        MulticastMessage message = new MulticastMessage(MembershipEvent.LEAVE, this.membershipCounter, this.address.getHostAddress(), this.storagePort);
        try {
            message.send(this.multicastAddress, this.multicastPort);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.membershipCounter++;

        synchronized (this.connectionStatus){
            this.connectionStatus = ConnectionStatus.DISCONNECTED;
        }
    }

    private class MulticastListener implements Runnable {
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
            while (connectionStatus == ConnectionStatus.CONNECTED) {
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

                printDebugInfo("Multicast message received: " + received);

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
            NodeInfo nodeInfo = parsedData.first;
            int receivedCounter = parsedData.second;

            if (nodeInfo.getAddress().equals(address.getHostAddress()) && nodeInfo.getStoragePort() == storagePort) {
                return;
            }

            executor.execute(new MembershipInformationSender(nodeInfo.getAddress(), nodeInfo.getStoragePort()));
            nodeList.add(nodeInfo);
            log.addEntry(new MembershipLogEntry(nodeInfo.getAddress(), nodeInfo.getStoragePort(), receivedCounter));
        }

        private void handleLeave(String receivedData){
            Pair<NodeInfo, Integer> parsedData = parseJoinLeaveMessage(receivedData);
            NodeInfo nodeInfo = parsedData.first;
            int receivedCounter = parsedData.second;

            if (nodeInfo.getAddress().equals(address.getHostAddress()) && nodeInfo.getStoragePort() == storagePort) {
                return;
            }

            nodeList.remove(nodeInfo);
            log.addEntry(new MembershipLogEntry(nodeInfo.getAddress(), nodeInfo.getStoragePort(), receivedCounter));
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

    private class MulticastMembershipSender implements Runnable {
        @Override
        public void run() {
            while(connectionStatus == ConnectionStatus.CONNECTED){
                try {
                    TimeUnit.SECONDS.sleep(nodeList.size());
                } catch (InterruptedException e) { // TODO: Ver melhor
                    return;
                }

                MulticastMessage message = new MulticastMessage(MembershipEvent.MEMBERSHIP, membershipCounter, address.getHostAddress(), storagePort);
                try {
                    message.send(multicastAddress, multicastPort, "", log.toString());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MembershipInformationSender implements Runnable {
        private InetAddress address;
        final private int port;

        public MembershipInformationSender(String address, int port){
            try {
                this.address = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            this.port = port;
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
            printDebugInfo("Sending TCP membership info (waited " + randomNum + " ms)");
            MembershipInformationMessageSender sender = new MembershipInformationMessageSender(this.address, this.port);
            try {
                sender.send(nodeList, log);
            } catch (IOException e) {
                return;     // TODO: Handle it better
            }
            
        }
    }

    private class MembershipInformationListener implements Callable<Boolean> {
        @Override
        public Boolean call(){
            TCPListener listener;
            try {
                listener = new TCPListener(address, storagePort);
            } catch (IOException e) {
                return false;
            }

            for(int i = 0; i < 3; ++i){
                try {
                    String message = listener.receive();
                    printDebugInfo("TCP membership message received: " + message);

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
                } catch(SocketTimeoutException e) {
                    printDebugInfo("Connection timeout");
                    listener.close();
                    return false;
                } catch(IOException e){
                    listener.close();
                    return false;
                }
            }
            listener.close();  
            return true;
        }
    }

    private class OperationListener implements Runnable {
        @Override
        public void run(){
            
        }
    }

    private void printDebugInfo(String message){
        System.out.println("[" + this.address.getHostAddress() + ":" + this.storagePort  + "] " + message);
    }

    public String toString() {
        return this.address.getHostAddress() + " " + this.storagePort;
    }
}
