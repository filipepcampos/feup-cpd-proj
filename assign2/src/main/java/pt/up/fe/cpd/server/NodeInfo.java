package pt.up.fe.cpd.server;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.security.MessageDigest;

public class NodeInfo {
    private String address;
    private int storagePort;
    private byte[] nodeId;
    
    public NodeInfo(String address, int storagePort) {
        this.address = address;
        this.storagePort = storagePort;     
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            this.nodeId = digest.digest(address.getBytes(StandardCharsets.UTF_8));
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }   
    }

    public String getAddress() {
        return address;
    }

    public int getStoragePort() {
        return storagePort;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public String toString() {
        return this.address + ":" + this.storagePort;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NodeInfo)) return false;
        NodeInfo entry = (NodeInfo) obj;
        return this.address.equals(entry.getAddress()) && this.storagePort == entry.getStoragePort();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.storagePort);
    }
}
