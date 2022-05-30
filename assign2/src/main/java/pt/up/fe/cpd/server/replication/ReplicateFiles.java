package pt.up.fe.cpd.server.replication;

import pt.up.fe.cpd.networking.FileTransfer;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.utils.HashUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ReplicateFiles implements Runnable {
    private final NodeInfo currentNode;
    private final NodeInfo targetNode;
    private final byte[] lowestKey;
    private final byte[] highestKey;

    public ReplicateFiles(NodeInfo currentNode, NodeInfo targetNode, byte[] lowestKey, byte[] highestKey){
        this.currentNode = currentNode;
        this.targetNode = targetNode;
        this.lowestKey = lowestKey;
        this.highestKey = highestKey;
    }

    @Override
    public void run(){
        File directory = new File(currentNode.getAddress() + "_" + currentNode.getPort());

        System.out.println(this.currentNode + " Opening " + directory.getName() + " and searching for matching files...");
        System.out.println(this.currentNode + " keys:" + HashUtils.keyByteToString(lowestKey) + " to " + HashUtils.keyByteToString(highestKey));
        File[] matchingFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // lowestKey < fileKey <= highestKey
                byte[] fileKey = HashUtils.keyStringToByte(name);
                int comparison = HashUtils.compare(fileKey, lowestKey);
                if(comparison <= 0){
                    return false;
                }
                return HashUtils.compare(fileKey, highestKey) <= 0;
            }
        });

        if(matchingFiles != null){
            System.out.println("not null");
            for (File file : matchingFiles) {
                System.out.println("file");
                this.sendReplicationOperation(file);
            }
        }
        System.out.println("might be null");
    }

    private void sendReplicationOperation(File file){
        System.out.println(currentNode + " replicating " + file.getName() + " to node " + targetNode);
        try {
            InetAddress targetNodeAddress = InetAddress.getByName(targetNode.getAddress());
            Socket socket = new Socket(targetNodeAddress, targetNode.getPort());

            FileInputStream fileInputStream = new FileInputStream(file);

            DataInputStream inputStream = new DataInputStream(fileInputStream);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

            outputStream.write(("REPLICATE PUT " + file.getName() + "\n\n").getBytes("UTF-8"));

            boolean transferSuccessful = FileTransfer.transfer(inputStream, outputStream);

            inputStream.close();
            outputStream.close();
            socket.close();
        } catch(IOException e){
            e.printStackTrace();
        }
        
    }
}
