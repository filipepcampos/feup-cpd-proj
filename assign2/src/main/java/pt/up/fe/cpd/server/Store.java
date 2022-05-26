package pt.up.fe.cpd.server;

import pt.up.fe.cpd.server.membership.MembershipService;
import pt.up.fe.cpd.server.store.KeyValueStore;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store extends Node implements KeyValueStore {
    public Store(String multicastIp, int multicastPort, String nodeId, int storagePort) {
        super(multicastIp, multicastPort, nodeId, storagePort);
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
        String nodeId = args[2]; // TODO: Use correct format
        String storagePort = args[3];

        System.out.println("["+nodeId+"] Store:: creating new Store (" + multicastIP + ", " + multicastPort + ", " + storagePort + ") ");

        Store store = new Store(multicastIP, Integer.parseInt(multicastPort), nodeId, Integer.parseInt(storagePort));
        try {
            MembershipService stub = (MembershipService) UnicastRemoteObject.exportObject(store, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(nodeId, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
