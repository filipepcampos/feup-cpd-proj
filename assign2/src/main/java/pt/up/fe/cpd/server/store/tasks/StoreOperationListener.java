package pt.up.fe.cpd.server.store.tasks;

import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.net.*;
import pt.up.fe.cpd.networking.TCPListener;
import pt.up.fe.cpd.server.NodeInfo;
import pt.up.fe.cpd.server.NodeSearcher;
import pt.up.fe.cpd.server.store.KeyValueStore;

public class StoreOperationListener implements Runnable {
    private final KeyValueStore keyValueStore;
    private final TCPListener listener;
    private final ExecutorService executor;
    private final NodeInfo nodeInfo;
    private final NodeSearcher nodeSearcher;

    public StoreOperationListener(KeyValueStore keyValueStore, TCPListener listener, ExecutorService executor, NodeInfo nodeInfo, NodeSearcher nodeSearcher){
        this.keyValueStore = keyValueStore;
        this.executor = executor;
        this.listener = listener;
        this.nodeInfo = nodeInfo;
        this.nodeSearcher = nodeSearcher;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket socket = listener.accept();
                System.out.println("[debug, storeoperationlistener] accepted");
                executor.execute(new StoreOperationHandler(keyValueStore, socket, nodeInfo, nodeSearcher));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
