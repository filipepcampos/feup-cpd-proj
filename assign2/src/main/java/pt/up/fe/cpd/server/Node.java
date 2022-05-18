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
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import pt.up.fe.cpd.networking.TCPListener;

public class Node implements MembershipService {
    private List<Node> nodeList;
    private MembershipLog log;
    private InetAddress multicastAddress;
    private int multicastPort;
    private InetAddress address;
    private int storagePort;

    private byte[] nodeId;              // Hashed address
    private String nodeIdString;        // Printable hashed address
    
    private int membershipCounter;
    private ExecutorService executor;   // ThreadPool
    private Boolean connected;          // TODO: Change to enum

    public Node(String multicastAddress, int multicastPort, String address, int storagePort) {
        try {
            this.multicastAddress = InetAddress.getByName(multicastAddress);
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.connected = false;
        this.multicastPort = multicastPort;
        this.storagePort = storagePort;
        this.membershipCounter = 0; // TODO: Write/Read from file
        this.nodeList = new ArrayList<>();
        this.log = new MembershipLog();
        this.executor = Executors.newFixedThreadPool(8);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            this.nodeId = digest.digest(address.getBytes(StandardCharsets.UTF_8));
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            // TODO: What to do
        }
        this.nodeIdString = MembershipUtils.parseNodeId(this.nodeId);
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

        printDebugInfo("Joined");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.nodeIdString, this.membershipCounter, this.address.getHostAddress(), this.storagePort);
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
                printDebugInfo("Multicast message received: " + received);
                
                String[] splitString = received.split(" ");
                String receivedNodeId = splitString[1];
                int receivedCounter = Integer.valueOf(splitString[2]);
                String receivedAddress = splitString[3];
                int receivedPort = Integer.valueOf(splitString[4]);
                
                switch(receivedCounter % 2){
                    case 0: // Joining
                        executor.execute(new MembershipInformationSender(receivedAddress, receivedPort));
                        Node newNode = new Node(multicastAddress.getHostAddress(), multicastPort, receivedAddress, receivedPort);
                        nodeList.add(newNode);
                        log.addEntry(new MembershipLogEntry(newNode.getNodeId(), receivedCounter));
                        break;
                    case 1: // Leaving
                        break;
                    // etc...
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

    private class MembershipInformationListener implements Runnable {

        @Override
        public void run(){
            TCPListener listener;
            try {
                listener = new TCPListener(address, storagePort);
            } catch (IOException e) {
                // TODO: IDK
                return;
            }
            printDebugInfo("Listening for TCP membership info");
            for(int i = 0; i < 3; ++i){
                String message;
                try {
                    message = listener.receive();
                    printDebugInfo("TCP membership message received: " + message);
                } catch(SocketTimeoutException e) {
                    printDebugInfo("CONNECTION TIMED OUT");
                } catch(IOException e){
                    printDebugInfo("GOT IOEXCEPTION");
                } catch(Exception e){
                    printDebugInfo("GOT SOME OTHER EXCEPTION");
                }
            }
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

    public String toString() {
        return this.address.getHostAddress() + " " + this.storagePort;
    }
}
