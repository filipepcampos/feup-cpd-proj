package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;
import java.util.concurrent.ThreadLocalRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class Node implements MembershipService {
    private List<Node> nodeList;
    private InetAddress multicastAddress;
    private int multicastPort;
    private byte[] nodeId;
    private String nodeIdString;
    private int storagePort;
    private int membershipCounter;
    private ExecutorService executor;
    private Boolean connected;

    public Node(String multicastAddress, int multicastPort, String nodeId, int storagePort) {
        try {
            this.multicastAddress = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.connected = false;
        this.multicastPort = multicastPort;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            this.nodeId = digest.digest(nodeId.getBytes(StandardCharsets.UTF_8));
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            // TODO: What to do
        }
        this.nodeIdString = parseNodeId();

        this.storagePort = storagePort;
        this.membershipCounter = 0; // TODO: Write/Read from file
        this.nodeList = new ArrayList<>();
        
        this.executor = Executors.newFixedThreadPool(8);
    }

    private String parseNodeId() {
        StringBuilder result = new StringBuilder();
        for (byte b : this.nodeId) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
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

        printDebugInfo("join");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.nodeIdString, this.membershipCounter);
        try {
            executor.execute(new MembershipInformationListener());
            message.send(this.multicastAddress, this.multicastPort);
        } catch (IOException e) {
            // TODO:
            System.out.println(e.getCause() + " " + e.getMessage());
            e.printStackTrace();
        }
        this.connected = true;
        executor.execute(new MulticastListener());
    }

    public void leave() {
        /*
        - Multicast LEAVE message
        - Update membership counter
        */
        printDebugInfo("leave");
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
            while (connected) {
                DatagramPacket packet = new DatagramPacket(buf, 1024); // TODO: Try out of loop
                try {
                    socket.receive(packet);
                } catch(IOException e) {
                    socket.close();
                    e.printStackTrace();
                    return;
                }
                String received = new String(packet.getData(), 0, packet.getLength());
                printDebugInfo("receivedmulticast " + received);
                executor.execute(new MembershipInformationSender());
                
                
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
            printDebugInfo("sending TCP membership info (waited " + randomNum + " ms)");
        }
    }

    private class MembershipInformationListener implements Runnable {
        @Override
        public void run(){
            printDebugInfo("listening for TCP membership info");
        }
    }

    private class OperationListener implements Runnable {
        @Override
        public void run(){
            
        }
    }

    private void printDebugInfo(String message){
        System.out.println("[" + nodeIdString.substring(0, 5) + "] " + message);
    }
}
