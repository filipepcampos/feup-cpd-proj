package pt.up.fe.cpd.server;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.membership.MembershipService;
import pt.up.fe.cpd.server.store.KeyValueStore;
import pt.up.fe.cpd.server.tasks.StoreOperationListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store implements KeyValueStore {
    String address;
    int storagePort;
    TCPListener listener;

    public Store(String address, int storagePort) {
        this.address = address;
        this.storagePort = storagePort;
    }

    public void open(){
        try {
            InetAddress address = InetAddress.getByName(this.address);
            this.listener = new TCPListener(address, storagePort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive(ExecutorService executor){
        executor.execute(new StoreOperationListener(this, this.listener, executor));
    }

    public void close(){
        this.listener.close();
    }

    @Override
    public void put(String key, DataInputStream data) {     // TODO: Change return to Booleean for success or failure
        // Transfer file
        FileOutputStream fileOutputStream;
        try{
            fileOutputStream = new FileOutputStream(address + "_" + storagePort + "/" + key); // TODO: What name should the file have
        } catch(FileNotFoundException e){   // TODO: This is quite irrelevant
            System.out.println("File cannot be found.");
            try {
                data.close();
            } catch(IOException e1){
                e1.printStackTrace();
            }
            return;
        }
        
        DataOutputStream outputStream = new DataOutputStream(fileOutputStream);

        int count;
        byte[] buffer = new byte[4096];

        try {
            while((count = data.read(buffer)) > 0){
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            data.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void get(String key, DataOutputStream data) {
        return;
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
        String address = args[2]; // TODO: Use correct format
        String storagePort = args[3];

        System.out.println("["+address+"] Store:: creating new Store (" + multicastIP + ", " + multicastPort + ", " + storagePort + ") ");

        Node store = new Node(multicastIP, Integer.parseInt(multicastPort), address, Integer.parseInt(storagePort));
        try {
            MembershipService stub = (MembershipService) UnicastRemoteObject.exportObject(store, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(address, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
