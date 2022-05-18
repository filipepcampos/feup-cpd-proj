package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node implements MembershipService {
    private List<Node> nodeList;
    private InetAddress multicastAddress;
    private int multicastPort;
    private String nodeId;
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
        this.nodeId = nodeId; // TODO: hash the nodeId
        // TODO: Create function to print nodeId (?)
        this.storagePort = storagePort;
        this.membershipCounter = 0; // TODO: Write/Read from file
        this.nodeList = new ArrayList<>();
        
        this.executor = Executors.newFixedThreadPool(8);
    }

    public String getNodeId() {
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

        System.out.println("[" + this.nodeId + "] Node::join");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.nodeId, membershipCounter);
        try {
            message.send(multicastAddress, multicastPort);
        } catch (IOException e) {
            // TODO:
            System.out.println(e.getCause() + " " + e.getMessage());
            e.printStackTrace();
        }
        this.connected = true;
        executor.execute(new MulticastListener());
    }

    public void leave() {
        System.out.println("["+nodeId+"] Node::leave");
        // - Multicast LEAVE message
        // - Update membership counter        
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
                System.out.println("["+nodeId+"] Node::receivedmulticast " + received);
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

    private class OperationListener implements Runnable {
        @Override
        public void run(){
            
        }
    }
}
