package pt.up.fe.cpd.server.store.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.replication.RemoveFiles;
import pt.up.fe.cpd.server.store.KeyValueStore;
import pt.up.fe.cpd.utils.HashUtils;

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
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            
            Scanner scanner = new Scanner(dataInputStream);
            String header = scanner.nextLine();

            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];

            if (operation.equals("MEMBERSHIP")) {
                scanner.close();
                dataInputStream.close();
                socket.close();
                return;
            }

            System.out.println("[StoreOperationHandler] opened");
            
            // REPLICATE PUT key
            // REPLICATE DELETE key
            // REPLICATE DELETE_RANGE key1 key2
            if (operation.equals("REPLICATE")) {
                operation = splitHeader[1];
                if(operation.equals("DELETE_RANGE")){
                    handleDeleteRange(splitHeader[1], splitHeader[2]);
                } else {
                    String key = splitHeader[2];
                    handleRequest(operation, key, dataInputStream);    
                }
            } else {
                String key = splitHeader[1];
                NodeInfo node = this.searcher.findNodeByKey(HashUtils.keyStringToByte(key));
                if(this.searcher.isActiveNode(node)){
                    System.out.println("THIS KEY BELONGS TO ME!!!");
                    handleRequest(operation, key, dataInputStream);
                } else {
                    System.out.println("not my responsibility... " + node.toString() + " this one's for you");
                    handleRedirect(node, operation, key, dataInputStream);
                }
            }

            scanner.close();
            dataInputStream.close();
            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleDeleteRange(String lowestKey, String highestKey) {
        ActiveNodeInfo activeNode = this.searcher.getActiveNode();
        RemoveFiles task = new RemoveFiles(activeNode, HashUtils.keyStringToByte(lowestKey), HashUtils.keyStringToByte(highestKey));
        task.run();
    }

    private void handleRequest(String operation, String key, DataInputStream dataInputStream) throws IOException {
        switch(operation) {
            case "GET":
                DataOutputStream dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
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
    }

    private void handleRedirect(NodeInfo node, String operation, String key, DataInputStream clientInputStream) throws IOException {
        InetAddress nodeAddress = InetAddress.getByName(node.getAddress());
        Socket nodeSocket = new Socket(nodeAddress, node.getPort());
        DataOutputStream nodeOutputStream = new DataOutputStream(nodeSocket.getOutputStream());

        nodeOutputStream.write((operation + " " + key + "\n").getBytes("UTF-8"));
        switch(operation) {
            case "GET":
                DataOutputStream clientOutputStream = new DataOutputStream(socket.getOutputStream());
                DataInputStream nodeInputStream = new DataInputStream(nodeSocket.getInputStream());
                FileTransfer.transfer(nodeInputStream, clientOutputStream);
                nodeInputStream.close();
                clientOutputStream.close();
                break;
            case "PUT":
                nodeOutputStream.write(("\n").getBytes("UTF-8"));
                FileTransfer.transfer(clientInputStream, nodeOutputStream);
                break;
        }
        nodeSocket.close();
    }
}