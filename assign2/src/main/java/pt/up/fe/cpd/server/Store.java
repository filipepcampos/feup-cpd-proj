package pt.up.fe.cpd.server;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.membership.MembershipService;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.store.KeyValueStore;
import pt.up.fe.cpd.server.store.tasks.StoreOperationListener;
import pt.up.fe.cpd.utils.HashUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store extends Node implements KeyValueStore {
    private final String directory;

    public Store(String multicastAddress, int multicastPort, String address, int storagePort) throws UnknownHostException {
        super(multicastAddress, multicastPort, address, storagePort);
        this.directory = "store_" + HashUtils.keyByteToString(this.getNodeId());

        // Clean directory
        File directoryFile = new File(directory);
        if(directoryFile.exists()){
            File[] content = directoryFile.listFiles();
            if(content != null){
                for(File f : content){
                    f.delete();
                }
            }
        } else {
            directoryFile.mkdir();
        }
    }

    @Override
    public void receive(){
        this.getExecutor().execute(new StoreOperationListener(this, this.getListener(), this.getExecutor(), (ClusterSearcher) getCluster(), (ClusterViewer) getCluster(), 
                                                              this.getMulticastAddress(), this.getMulticastPort()));
    }

    @Override
    public boolean put(String key, DataInputStream data) {     // TODO: Change return to Booleean for success or failure
        // Transfer file
        FileOutputStream fileOutputStream;
        try{
            File tombstoneFile = new File(this.directory + "/" + key + ".tombstone");
            if(tombstoneFile.exists()){
                data.close();
                return false;
            }

            fileOutputStream = new FileOutputStream(this.directory + "/" + key); // TODO: What name should the file have
        } catch(FileNotFoundException e){   // TODO: This is quite irrelevant
            System.out.println("File cannot be found.");
            try {
                data.close();
            } catch(IOException e1){
                e1.printStackTrace();
            }
            return false;
        } catch(IOException e){
            return false;
        }
        
        DataOutputStream outputStream = new DataOutputStream(fileOutputStream);

        boolean transferSuccessful = FileTransfer.transfer(data, outputStream);
        try{
            outputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return transferSuccessful;
    }

    @Override
    public boolean get(String key, DataOutputStream data) {
        // Transfer file
        FileInputStream fileInputStream;
        try{
            fileInputStream = new FileInputStream(this.directory + "/" + key);
        } catch(FileNotFoundException e){
            System.out.println("File cannot be found.");
            try {
                data.close();
            } catch(IOException e1){
                e1.printStackTrace();
            }
            return false;
        }
        
        DataInputStream inputStream = new DataInputStream(fileInputStream);

        boolean transferSuccessful = FileTransfer.transfer(inputStream, data);
        try{
            inputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return transferSuccessful;
    }

    @Override
    public boolean delete(String key) {
        File file = new File(this.directory + "/" + key);
        File tombstoneFile = new File(this.directory + "/" + key + ".tombstone");
        try {
            tombstoneFile.createNewFile();
        } catch(IOException e){
            e.printStackTrace();
        }

        return file.delete();
    }

    // A service node should be invoked as follows: $ java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>
    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 4) {
            throw new RuntimeException("Usage: Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
        }

        String multicastIP = args[0];
        String multicastPort = args[1];
        String address = args[2]; // TODO: Use correct format
        String storagePort = args[3];

        System.out.println("["+address+"] Store:: creating new Store (" + multicastIP + ", " + multicastPort + ", " + storagePort + ") ");

        Store store = new Store(multicastIP, Integer.parseInt(multicastPort), address, Integer.parseInt(storagePort));
        try {
            MembershipService stub = (MembershipService) UnicastRemoteObject.exportObject(store, 0);
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(address, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
