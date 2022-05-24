package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

public class Node implements MembershipService {
    private HashSet<NodeInfo> nodeList;
    private MembershipLog log;
    private InetAddress multicastAddress;
    private int multicastPort;
    private InetAddress address;
    private int storagePort;

    private byte[] nodeId;              // Hashed address
    private String nodeIdString;        // Printable hashed address
    
    private int membershipCounter;
    private ExecutorService executor;   // ThreadPool
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
        this.nodeIdString = MembershipUtils.parseNodeId(this.nodeId);

        this.membershipCounter = 0; // TODO: Write/Read from file
        this.executor = Executors.newFixedThreadPool(8);
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    public byte[] getNodeId() {
        return this.nodeId;
    }

    public int getStoragePort() {
        return this.storagePort;
    }

    public void join() {
        /*
        - Before MC start accepting connections on port whose number it sends in JOIN message
        - Multicast JOIN message with membership counter
        - Receive 3 membership messages: list of current cluster members and 32 most recent membership events (logs) 
        - If it didn't receive from 3 retransmit JOIN message at most 2 times (3 transmissions counting with original message)
        - Update membership counter (+1)
        - Receive key-value from two adjacent nodes
        [After joining the cluster the node should probably start listening to the Multicast Address for 1 second appart MEMBERSHIP messages]
        */

        if (!(this.connectionStatus == ConnectionStatus.DISCONNECTED)) return;

        this.connectionStatus = ConnectionStatus.CONNECTING;
        printDebugInfo("Trying to join");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.membershipCounter, this.address.getHostAddress(), this.storagePort);
        try {
            for (int i = 0; i < 3; ++i) {
                Future<Boolean> futureResult = executor.submit(new MembershipInformationListener());
                message.send(this.multicastAddress, this.multicastPort);
                printDebugInfo("Multicast message sent");
                Boolean joinedSuccessfully = false;
                try {
                    joinedSuccessfully = futureResult.get();
                } catch (InterruptedException e){
                    e.printStackTrace();
                } catch (ExecutionException e){
                    e.printStackTrace();
                }
                
                if (joinedSuccessfully) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.connectionStatus = ConnectionStatus.CONNECTED;
        this.log.addEntry(new MembershipLogEntry(this.nodeId, this.membershipCounter));
        this.membershipCounter++;
        executor.execute(new MulticastListener());
    }

    public void leave() {
        /*
        - Multicast LEAVE message
        - Update membership counter
        */

        printDebugInfo("Leaving");

        if (this.connectionStatus == ConnectionStatus.CONNECTED) {
            this.connectionStatus = ConnectionStatus.DISCONNECTING;
            MulticastMessage message = new MulticastMessage(MembershipEvent.LEAVE, this.membershipCounter, this.address.getHostAddress(), this.storagePort);
            try {
                message.send(this.multicastAddress, this.multicastPort);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            this.membershipCounter++;
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
                String receivedAddress = splitString[1];
                int receivedPort = Integer.valueOf(splitString[2]);
                int receivedCounter = Integer.valueOf(splitString[3]);
                NodeInfo nodeInfo = new NodeInfo(receivedAddress, receivedPort);
                
                if (receivedAddress.equals(address.getHostAddress()) && receivedPort == storagePort) {
                    continue;
                }

                printDebugInfo("Multicast message received: " + received);
                
                switch(receivedCounter % 2){
                    case 0: // Joining
                        executor.execute(new MembershipInformationSender(receivedAddress, receivedPort));
                        nodeList.add(nodeInfo);
                        log.addEntry(new MembershipLogEntry(nodeInfo.getNodeId(), receivedCounter));
                        break;
                    case 1: // Leaving
                        nodeList.remove(nodeInfo);
                        log.addEntry(new MembershipLogEntry(nodeInfo.getNodeId(), receivedCounter));
                        break;
                }

                // TODO: Update the node list
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
    }

    private class MembershipInformationSender implements Runnable {
        private InetAddress address;
        private int port;

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
                    if(!splitMessage[0].equals("MEMBERSHIP_INFORMATION")){
                        // TODO: Error
                    }
                    
                    String[] nodeListInfo = splitMessage[2].split(", ");
                    for(String nodeInfo : nodeListInfo){
                        String[] splitNodeInfo  = nodeInfo.split(" ");
                        String receivedAddress  = splitNodeInfo[0];
                        int receivedPort        = Integer.valueOf(splitNodeInfo[1]);
                        nodeList.add(new NodeInfo(receivedAddress, receivedPort));
                    }

                    String[] logInfo = splitMessage[3].split(", ");
                    for(String logData : logInfo){
                        String[] splitLog               = logData.split(" ");
                        byte[] receivedNodeId           = MembershipUtils.parseNodeIdString(splitLog[0]);
                        int receivedMembershipCounter   = Integer.valueOf(splitLog[1]);
                        log.addEntry(new MembershipLogEntry(receivedNodeId, receivedMembershipCounter));
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
        System.out.println("[" + this.address.getHostAddress() + "/" + nodeIdString.substring(0, 5) + "] " + message);
    }

    public String toString() {
        return this.address.getHostAddress() + " " + this.storagePort;
    }
}
