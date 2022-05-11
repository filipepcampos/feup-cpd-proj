package pt.up.fe.cpd;

public class Node {
    private String nodeId;
    private String storagePort;
    private int membershipCounter;

    public Node(String nodeId, String storagePort) {
        this.nodeId = nodeId;
        this.storagePort = storagePort;
        this.membershipCounter = 0;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getStoragePort() {
        return this.storagePort;
    }

    public void join() {
        // - Before MC start accepting connections on port whose number it sends in JOIN message
        // - Multicast JOIN message with membership counter
        // - Receive 3 membership messages: list of current cluster members and 32 most recent membership events (logs) 
        // - If it didn't receive from 3 retransmit JOIN message at most 2 times (3 transmissions counting with original message)
        // - Update membership counter (+1)

        // - Receive key-value from two adjacent nodes
        
        // [After joining the cluster the node should probably start listening to the Multicast Address for 1 second appart MEMBERSHIP messages]
    }

    public void leave() {
        // - Multicast LEAVE message
        // - Update membership counter        
    }
}
