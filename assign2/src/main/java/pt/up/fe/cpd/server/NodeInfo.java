package pt.up.fe.cpd.server;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.security.MessageDigest;

public class NodeInfo implements Comparable<NodeInfo> {
    final private String address;
    final private int port;
    private byte[] nodeId;
    
    public NodeInfo(String address, int port) {
        this.address = address;
        this.port = port;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            this.nodeId = digest.digest(address.getBytes(StandardCharsets.UTF_8));
        } catch(NoSuchAlgorithmException e){    // This should never happen
            e.printStackTrace();
        }
    }

    public NodeInfo(byte[] key){
        this.nodeId = key;
        this.address = "";
        this.port = 0;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public String toString() {
        return this.address + ":" + this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NodeInfo)) return false;
        NodeInfo entry = (NodeInfo) obj;
        return this.address.equals(entry.getAddress()) && this.port == entry.getPort();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.port);
    }

    @Override
    public int compareTo(NodeInfo other){
        byte[] otherNodeId = other.getNodeId();
        for (int i = 0; i < this.nodeId.length; ++i) {
            if (this.nodeId[i] < otherNodeId[i]){
                return -1;  // This object is less than the specified object
            } else if (this.nodeId[i] > otherNodeId[i]){
                return 1;  // This object is greater than the specified object
            }
        }
        return 0;  // They're equal
    }
}
