package pt.up.fe.cpd;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {
    // The test client should be invoked as follows: $ java TestClient <node_ap> <operation> [<opnd>]

    private TestClient() {}

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry();
            System.out.println("Looking up " + host);
            MembershipService stub = (MembershipService) registry.lookup(host);
            stub.join();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}