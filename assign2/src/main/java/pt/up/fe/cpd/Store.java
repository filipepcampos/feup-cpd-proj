package pt.up.fe.cpd;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
        try {
            System.setProperty("java.rmi.server.hostname", nodeId); // TODO: I'm not 100% sure if this is what we should do
            MembershipService stub = (MembershipService) UnicastRemoteObject.exportObject(store, 0); // TODO: change port?
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("MembershipService", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
