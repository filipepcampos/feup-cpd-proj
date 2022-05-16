package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Node implements MembershipService {
    private List<Node> nodeList;
    private InetAddress multicastAddress;
    private int multicastPort;
    private String nodeId;
    private int storagePort;
    private int membershipCounter;

    public Node(String multicastAddress, int multicastPort, String nodeId, int storagePort) {
        try {
            this.multicastAddress = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.storagePort = storagePort;
        this.membershipCounter = 0; // TODO: Read from file
        this.nodeList = new ArrayList<>();
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public int getStoragePort() {
        return this.storagePort;
    }

    public void join() {
        System.out.println("["+nodeId+"] Node::join");
        MulticastMessage message = new MulticastMessage(MembershipEvent.JOIN, this.nodeId, membershipCounter);
        try {
            message.send(multicastAddress, multicastPort);
        } catch (IOException e) {
            // TODO:
            System.out.println(e.getCause() + " " + e.getMessage());
            e.printStackTrace();
        }
        // - Before MC start accepting connections on port whose number it sends in JOIN message
        // - Multicast JOIN message with membership counter
        // - Receive 3 membership messages: list of current cluster members and 32 most recent membership events (logs) 
        // - If it didn't receive from 3 retransmit JOIN message at most 2 times (3 transmissions counting with original message)
        // - Update membership counter (+1)

        // - Receive key-value from two adjacent nodes
        
        // [After joining the cluster the node should probably start listening to the Multicast Address for 1 second appart MEMBERSHIP messages]
    }

    public void leave() {
        System.out.println("["+nodeId+"] Node::leave");
        // - Multicast LEAVE message
        // - Update membership counter        
    }
}
