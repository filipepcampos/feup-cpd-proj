package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.membership.*;
import pt.up.fe.cpd.server.membership.log.MembershipLog;
import pt.up.fe.cpd.server.membership.log.MembershipLogEntry;
import pt.up.fe.cpd.server.tasks.MembershipInformationListener;
import pt.up.fe.cpd.server.tasks.MulticastListener;
import pt.up.fe.cpd.server.tasks.MulticastMembershipSender;

public abstract class Node extends NodeInfo implements MembershipService {
    final private TreeSet<NodeInfo> nodeSet;
    final private MembershipLog log;
    final private InetAddress multicastAddress;
    final private int multicastPort;
    final private InetAddress address;
    
    private int membershipCounter;
    final private ExecutorService executor;   // ThreadPool
    final private Connection connection;

    private TCPListener listener;

    public Node(String multicastAddress, int multicastPort, String address, int storagePort) throws UnknownHostException {
        super(address, storagePort);
        this.nodeSet = new TreeSet<>();
        this.nodeSet.add(new NodeInfo(address, storagePort));
        this.log = new MembershipLog();
        this.multicastAddress = InetAddress.getByName(multicastAddress);
        this.multicastPort = multicastPort;
        this.address = InetAddress.getByName(address);

        this.membershipCounter = 0; // TODO: Write/Read from file
        this.executor = Executors.newFixedThreadPool(8);
        this.connection = new Connection();
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected TCPListener getListener() {
        return this.listener;
    }

    public void open(){
        try {
            this.listener = new TCPListener(this.address, this.getStoragePort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void receive();

    public void close(){
        this.listener.close();
    }

    public void join() {
        synchronized (this.connection){
            if (this.connection.getStatus() != ConnectionStatus.DISCONNECTED) return;
            this.connection.setStatus(ConnectionStatus.CONNECTING);
        }
        
        printDebugInfo("Joining the cluster");
        MembershipMessageSender message = new MembershipMessageSender(MembershipEvent.JOIN, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            for (int i = 0; i < 3; ++i) {
                MembershipInformationListener listener = new MembershipInformationListener(address, getStoragePort(), nodeSet, log);
                Future<Boolean> futureResult = executor.submit(listener);
                message.send(this.getAddress(), this.getStoragePort());
                printDebugInfo("JOIN multicast message sent (" + (i+1) + "/3)");
                Boolean joinedSuccessfully = false;
                try {
                    joinedSuccessfully = futureResult.get();
                } catch (InterruptedException | ExecutionException e){
                    e.printStackTrace();
                }

                if (joinedSuccessfully) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.open();
        this.receive();
        
        synchronized (this.connection){
            this.connection.setStatus(ConnectionStatus.CONNECTED);
        }
   
        this.log.addEntry(new MembershipLogEntry(this.getAddress(), this.getStoragePort(), this.membershipCounter));
        this.membershipCounter++;
        executor.execute(new MulticastListener(this, multicastAddress, multicastPort, connection, log, nodeSet, executor));
        executor.execute(new MulticastMembershipSender(multicastAddress, multicastPort, membershipCounter, connection, nodeSet, log));
    }

    public void leave() {
        /*
        - Multicast LEAVE message
        - Update membership counter
        */

        printDebugInfo("Leaving the cluster");

        synchronized (this.connection){
            if (this.connection.getStatus() != ConnectionStatus.CONNECTED) return;
            this.connection.setStatus(ConnectionStatus.DISCONNECTING);
        }
        
        MembershipMessageSender message = new MembershipMessageSender(MembershipEvent.LEAVE, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            message.send(this.getAddress(), this.getStoragePort());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.membershipCounter++;

        this.close();
        synchronized (this.connection){
            this.connection.setStatus(ConnectionStatus.DISCONNECTED);
        }
    }

    private void printDebugInfo(String message){
        System.out.println("[" + getAddress() + ":" + getStoragePort()  + "] " + message);
    }

    public String toString() {
        return getAddress() + " " + getStoragePort();
    }
}
