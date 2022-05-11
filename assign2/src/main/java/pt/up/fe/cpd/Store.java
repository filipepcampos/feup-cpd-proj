package pt.up.fe.cpd;

import java.util.List;
import java.util.ArrayList;

public class Store extends Node implements KeyValueStore {
    private List<Store> storeList;

    public Store(String nodeId, String storagePort) {
        super(nodeId, storagePort);
        this.storeList = new ArrayList<>();
    }

    @Override
    public String put(String key, byte[] value) {
        return null;
    }

    @Override
    public byte[] get(String key) {
        return new byte[0];
    }

    @Override
    public void delete(String key) {
        return;
    }

    // A service node should be invoked as follows: $ java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>
    public static void main(String[] args) {
        if (args.length != 4) {
            throw new RuntimeException("Usage: Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
        }

        String multicastIP = args[0];
        String multicastPort = args[1];
        String nodeId = args[2];
        String storagePort = args[3];

        Store store = new Store(nodeId, storagePort);
    }
}