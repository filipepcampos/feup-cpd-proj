package pt.up.fe.cpd.server.store.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.ActiveNodeInfo;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.replication.RemoveFiles;
import pt.up.fe.cpd.server.replication.SendReplicateFileMessage;
import pt.up.fe.cpd.server.replication.SendDeleteMessage;
import pt.up.fe.cpd.server.store.KeyValueStore;
import pt.up.fe.cpd.utils.HashUtils;
import pt.up.fe.cpd.utils.Pair;

public class StoreOperationHandler implements Runnable {
    final private KeyValueStore keyValueStore;
    final private Socket socket;
    final private ClusterSearcher searcher;
    final private ExecutorService executor;
    
    public StoreOperationHandler(KeyValueStore keyValueStore, Socket socket, ClusterSearcher searcher, ExecutorService executor) {
        this.keyValueStore = keyValueStore;
        this.socket = socket;
        this.searcher = searcher;
        this.executor = executor;
    }

    @Override
    public void run() {        
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            
            Scanner scanner = new Scanner(dataInputStream);
            String header = scanner.nextLine();

            String[] splitHeader = header.split(" ");
            String operation = splitHeader[0];

            System.out.println("Opened an operation handler");
            
            // REPLICATE PUT key
            // REPLICATE DELETE key
            // REPLICATE DELETE_RANGE key1 key2
            if (operation.equals("REPLICATE")) {
                operation = splitHeader[1];
                if(operation.equals("DELETE_RANGE")){
                    handleDeleteRange(splitHeader[2], splitHeader[3]);
                } else {
                    String key = splitHeader[2];
                    handleReplicationRequest(operation, key, dataInputStream);    
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
        this.handleReplicationRequest(operation, key, dataInputStream);
        NodeInfo currentNode = this.searcher.getActiveNode();
        Pair<NodeInfo, NodeInfo> neighbours = searcher.findTwoClosestNodes(currentNode);

        switch(operation){
            case "PUT":
                // Replicate files to the two adjacent nodes
                if(!neighbours.first.equals(currentNode)){ // More than 1 node in the cluster
                    executor.execute(new SendReplicateFileMessage(currentNode, neighbours.first, HashUtils.keyStringToByte(key)));
                    if(!neighbours.first.equals(neighbours.second)){ // If there's only 2 nodes the neighbours will be the same node
                        executor.execute(new SendReplicateFileMessage(currentNode, neighbours.second, HashUtils.keyStringToByte(key)));
                    }
                }
                break;
            case "DELETE":
                // Replicate tombstone files to the two adjacent neighbours
                if(!neighbours.first.equals(currentNode)){ // More than 1 node in the cluster
                    executor.execute(new SendDeleteMessage(neighbours.first, HashUtils.keyStringToByte(key)));
                    if(!neighbours.first.equals(neighbours.second)){ // If there's only 2 nodes the neighbours will be the same node
                        executor.execute(new SendDeleteMessage(neighbours.second, HashUtils.keyStringToByte(key)));
                    }
                }
                break;
        }
    }

    private void handleReplicationRequest(String operation, String key, DataInputStream dataInputStream) throws IOException {
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

        Socket nodeSocket;
        try {
            nodeSocket = new Socket(nodeAddress, node.getPort());
        } catch(ConnectException e){
            System.out.println("Connection to " + node + " refused.");
            return;
        }
        
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