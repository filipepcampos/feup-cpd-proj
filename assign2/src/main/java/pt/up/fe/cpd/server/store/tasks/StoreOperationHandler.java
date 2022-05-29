package pt.up.fe.cpd.server.store.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationHandler implements Runnable {
    final private KeyValueStore keyValueStore;
    final private Socket socket;
    final private ClusterSearcher searcher;
    
    public StoreOperationHandler(KeyValueStore keyValueStore, Socket socket, ClusterSearcher searcher) {
        this.keyValueStore = keyValueStore;
        this.socket = socket;
        this.searcher = searcher;
    }

    @Override
    public void run() {        
        try {
            System.out.println("[StoreOperationHandler] opened");
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            
            Scanner scanner = new Scanner(dataInputStream);            
            String header = scanner.nextLine();

            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];
            // TODO: Ignore membership messages
            String key = splitHeader[1];

            NodeInfo node = this.searcher.findNodeByKey(keyStringToByte(key));
            if(this.searcher.isActiveNode(node)){
                System.out.println("THIS KEY BELONGS TO ME!!!");
            } else {
                System.out.println("not my responsibility... " + node.toString() + " this one's for you");
            }

            switch(operation){
                case "GET":
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    keyValueStore.get(key, dataOutputStream);
                    dataOutputStream.close();
                    break;
                case "DELETE":
                    keyValueStore.delete(key);
                    break;
                case "PUT":
                    keyValueStore.put(key, dataInputStream);
                    break;
            }
            
            scanner.close();
            dataInputStream.close();
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] keyStringToByte(String key){
        byte[] result = new byte[32];
        for (int i = 0; i < key.length(); i += 2) {
            result[i/2] = (byte) ((Character.digit(key.charAt(i), 16) << 4)
                                + Character.digit(key.charAt(i+1), 16));
        }
        return result;
    }
}