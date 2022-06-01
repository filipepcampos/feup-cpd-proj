package pt.up.fe.cpd.server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.membership.*;
import pt.up.fe.cpd.server.membership.cluster.Cluster;
import pt.up.fe.cpd.server.membership.cluster.ClusterManager;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.SearchableCluster;
import pt.up.fe.cpd.server.membership.tasks.MembershipInformationListener;
import pt.up.fe.cpd.server.membership.tasks.MulticastListener;
import pt.up.fe.cpd.server.membership.tasks.MulticastMembershipSender;
import pt.up.fe.cpd.utils.HashUtils;

public abstract class Node extends ActiveNodeInfo implements MembershipService {
    final private SearchableCluster cluster;
    final private InetAddress multicastAddress;
    final private int multicastPort;
    
    private int membershipCounter;
    final private ExecutorService executor;   // ThreadPool

    private TCPListener listener;

    public Node(String multicastAddress, int multicastPort, String address, int storagePort) throws UnknownHostException {
        super(address, storagePort);
        this.cluster = new SearchableCluster((ActiveNodeInfo) this);
        this.multicastAddress = InetAddress.getByName(multicastAddress);
        this.multicastPort = multicastPort;


        this.membershipCounter = 0; // TODO: Write/Read from file
        this.executor = Executors.newFixedThreadPool(8);
        printDebugInfo("Node online with hash " + HashUtils.keyByteToString(getNodeId()));
    }

    protected ExecutorService getExecutor() {
        return this.executor;
    }

    protected TCPListener getListener() {
        return this.listener;
    }

    public SearchableCluster getCluster() {
        return cluster;
    }

    public void open(){
        try {
            this.listener = new TCPListener(this.getInetAddress(), this.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void receive();

    public void close(){
        this.listener.close();
    }

    public void join() {
        Connection connection = cluster.getConnection();
        synchronized (connection){
            if (connection.getStatus() != ConnectionStatus.DISCONNECTED) return;
            connection.setStatus(ConnectionStatus.CONNECTING);
        }
        
        printDebugInfo("Joining the cluster");
        MembershipMessenger message = new MembershipMessenger(MembershipEvent.JOIN, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            for (int i = 0; i < 3; ++i) {
                MembershipInformationListener listener = new MembershipInformationListener((ActiveNodeInfo) this, cluster);
                Future<Boolean> futureResult = executor.submit(listener);
                message.send(this.getAddress(), this.getPort());
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

            printDebugInfo("Opening TCP connection");
            this.open();
            synchronized (connection){
                connection.setStatus(ConnectionStatus.CONNECTED);
            }
            this.receive();

            printDebugInfo("Sending JOINED message");
            message = new MembershipMessenger(MembershipEvent.JOINED, this.membershipCounter, this.multicastAddress, this.multicastPort);
            message.send(this.getAddress(), this.getPort());
            printDebugInfo("Sent JOINED message");

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.cluster.registerJoinNode(this, this.membershipCounter);
        this.membershipCounter++;
        executor.execute(new MulticastListener(this, multicastAddress, multicastPort, (ClusterViewer) cluster, (ClusterManager) cluster, (ClusterSearcher) cluster, executor));
        executor.execute(new MulticastMembershipSender(multicastAddress, multicastPort, membershipCounter, (ClusterViewer) cluster));
    }

    public void leave() {
        printDebugInfo("Leaving the cluster");

        Connection connection = cluster.getConnection();
        synchronized (connection){
            if (connection.getStatus() != ConnectionStatus.CONNECTED) return;
            connection.setStatus(ConnectionStatus.DISCONNECTING);
        }
        
        MembershipMessenger message = new MembershipMessenger(MembershipEvent.LEAVE, this.membershipCounter, this.multicastAddress, this.multicastPort);
        try {
            message.send(this.getAddress(), this.getPort());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.membershipCounter++;

        this.close();
        synchronized (connection){
            connection.setStatus(ConnectionStatus.DISCONNECTED);
        }
    }

    private void printDebugInfo(String message){
        System.out.println("[" + getAddress() + ":" + getPort()  + "] " + message);
    }
}
