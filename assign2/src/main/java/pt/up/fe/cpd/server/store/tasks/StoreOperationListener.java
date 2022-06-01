package pt.up.fe.cpd.server.store.tasks;

import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.net.*;
import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.membership.ConnectionStatus;
import pt.up.fe.cpd.server.membership.cluster.ClusterSearcher;
import pt.up.fe.cpd.server.membership.cluster.ClusterViewer;
import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationListener implements Runnable {
    private final KeyValueStore keyValueStore;
    private final TCPListener listener;
    private final ExecutorService executor;
    private final ClusterSearcher searcher;
    private final ClusterViewer clusterViewer;

    public StoreOperationListener(KeyValueStore keyValueStore, TCPListener listener, ExecutorService executor, ClusterSearcher searcher, ClusterViewer clusterViewer){
        this.keyValueStore = keyValueStore;
        this.executor = executor;
        this.listener = listener;
        this.searcher = searcher;
        this.clusterViewer = clusterViewer;
    }

    @Override
    public void run() {
        while(clusterViewer.getConnectionStatus() == ConnectionStatus.CONNECTED) {
            try {
                Socket socket = listener.accept();
                System.out.println("[debug, storeoperationlistener] accepted");
                executor.execute(new StoreOperationHandler(keyValueStore, socket, searcher, executor));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}