package pt.up.fe.cpd.client;

import pt.up.fe.cpd.server.membership.MembershipService;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import pt.up.fe.cpd.utils.Pair;

// The test client should be invoked as follows: $ java TestClient <node_ap> <operation> [<opnd>]
public class TestClient {

    public static void main(String[] args) throws IOException {
        if(args.length < 2){
            System.out.println("Invalid arguments");
            System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
        }
        String node_ap =  args[0]; // TODO: Change node_ap (If the service uses RMI, this must be the IP address and the name of the remote object providing the service.)
        String operation = args[1];

        switch(operation){
            case "join": join(node_ap); break;
            case "leave": leave(node_ap); break;
            case "get": get(node_ap, args[2]); break;
            case "put": put(node_ap, args[2]); break;
            case "delete": delete(node_ap, args[2]); break;
        }
    }

    public static void get(String node_ap, String key) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp = parseNodeAp(node_ap);
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
   
        Socket socket = new Socket(address, port);
        DataInputStream socketInputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());

        // Send GET request to server
        socketOutputStream.write(("GET " + key + "\n").getBytes("UTF-8"));

        // Transfer file
        DataOutputStream fileOutputStream;
        try{
            fileOutputStream = new DataOutputStream(new FileOutputStream(key)); // TODO: What name should the file have
        } catch(FileNotFoundException e){
            System.out.println("File cannot be found.");
            socket.close();
            return;
        }

        int count;
        byte[] buffer = new byte[4096];
        while((count = socketInputStream.read(buffer)) > 0){
            fileOutputStream.write(buffer, 0, count);
        }

        fileOutputStream.close();
        socketInputStream.close();
        socketOutputStream.close();
        socket.close();
    }

    public static String put(String node_ap, String file_path) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp = parseNodeAp(node_ap);
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
        
        byte[] key;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");    
            key = digest.digest(file_path.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { // This should never happen
            e.printStackTrace();
            return "";
        }

        Socket socket = new Socket(address, port);

        FileInputStream fileInputStream;
        try{
            fileInputStream = new FileInputStream(file_path);
        } catch(FileNotFoundException e){
            System.out.println("File " + file_path + " cannot be found.");
            socket.close();
            return "";
        }
        
        DataInputStream inputStream = new DataInputStream(fileInputStream);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        outputStream.write(("PUT " + keyByteToString(key) + "\n\n").getBytes("UTF-8"));

        int count;
        byte[] buffer = new byte[4096];
        while((count = inputStream.read(buffer)) > 0){
            outputStream.write(buffer, 0, count);
        }
        inputStream.close();
        outputStream.close();
        socket.close();

        return keyByteToString(key);
    }

    public static void delete(String node_ap, String key) throws IOException {
        Pair<InetAddress, Integer> parsedNodeAp = parseNodeAp(node_ap);
        InetAddress address = parsedNodeAp.first;
        int port = parsedNodeAp.second;
   
        Socket socket = new Socket(address, port);

        // Send GET request to server
        DataOutputStream socketOutputStream = new DataOutputStream(socket.getOutputStream());
        socketOutputStream.write(("DELETE " + key + "\n").getBytes("UTF-8"));
        socketOutputStream.close();
        socket.close();
    }

    public static void join(String host){
        try {
            Registry registry = LocateRegistry.getRegistry();
            System.out.println("Locating host " + host);
            MembershipService stub = (MembershipService) registry.lookup(host);
            stub.join();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public static void leave(String host){
        try {
            Registry registry = LocateRegistry.getRegistry();
            System.out.println("Locating host " + host);
            MembershipService stub = (MembershipService) registry.lookup(host);
            stub.leave();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private static Pair<InetAddress, Integer> parseNodeAp(String node_ap) throws IOException {
        // Host: ip_addr:port
        String[] splitHost = node_ap.split(":");
        String addressString = splitHost[0];
        int port = Integer.parseInt(splitHost[1]);
        InetAddress address = InetAddress.getByName(addressString);
        return new Pair<>(address, port);
    }

    private static String keyByteToString(byte[] key) {
        StringBuilder result = new StringBuilder();
        for (byte b : key) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}