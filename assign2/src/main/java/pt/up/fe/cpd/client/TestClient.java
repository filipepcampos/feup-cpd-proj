package pt.up.fe.cpd.client;

import pt.up.fe.cpd.server.MembershipService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {
    // The test client should be invoked as follows: $ java TestClient <node_ap> <operation> [<opnd>]

    private TestClient() {}

    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Invalid arguments");
            System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
        }
        String host =  args[0]; // TODO: Change node_ap
        String operation = args[1];

        switch(operation){
            case "join": join(host); break;
            case "leave": leave(host); break;
        }
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
}